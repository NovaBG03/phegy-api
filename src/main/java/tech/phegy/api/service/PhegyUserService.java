package tech.phegy.api.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tech.phegy.api.exception.PhegyHttpException;
import tech.phegy.api.model.points.PointsBag;
import tech.phegy.api.repository.PhegyRoleRepository;
import tech.phegy.api.repository.PhegyUserRepository;
import tech.phegy.api.repository.PointsBagRepository;
import tech.phegy.api.security.PhegyUserDetails;
import tech.phegy.api.service.imageGenerator.ImageGeneratorService;
import tech.phegy.api.service.register.event.OnEmailConfirmationNeededEvent;
import tech.phegy.api.service.storage.CloudStorageService;
import tech.phegy.api.service.storage.StoragePath;
import tech.phegy.api.service.validator.UserVerifier;
import tech.phegy.api.service.validator.ModelValidatorService;
import tech.phegy.api.model.user.PhegyRole;
import tech.phegy.api.model.user.PhegyRoleLevel;
import tech.phegy.api.model.user.PhegyUser;

import java.io.IOException;

/**
 * Service for common user operations.
 *
 * @author Nikita
 */
@Service
public class PhegyUserService implements UserDetailsService {
    private final PasswordEncoder passwordEncoder;
    private final PhegyUserRepository userRepository;
    private final PointsBagRepository pointsBagRepository;
    private final PhegyRoleRepository roleRepository;
    private final CloudStorageService cloudStorageService;
    private final ImageGeneratorService imageGeneratorService;
    private final UserVerifier userVerifier;
    private final ModelValidatorService modelValidatorService;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Constructs new instance with needed dependencies.
     */
    public PhegyUserService(PasswordEncoder passwordEncoder,
                            PhegyUserRepository userRepository,
                            PointsBagRepository pointsBagRepository,
                            PhegyRoleRepository roleRepository,
                            CloudStorageService cloudStorageService,
                            ImageGeneratorService imageGeneratorService,
                            UserVerifier userVerifier,
                            ModelValidatorService modelValidatorService,
                            ApplicationEventPublisher applicationEventPublisher) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.pointsBagRepository = pointsBagRepository;
        this.roleRepository = roleRepository;
        this.cloudStorageService = cloudStorageService;
        this.imageGeneratorService = imageGeneratorService;
        this.userVerifier = userVerifier;
        this.modelValidatorService = modelValidatorService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * Retrieves a specific user from the database and creates UserDetails from it.
     * Mostly used by the framework.
     *
     * @param username username to search for.
     * @return UserDetails wrapper of user from the database.
     * @throws UsernameNotFoundException if the user does not exist.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return new PhegyUserDetails(getUserByUsername(username));
    }

    /**
     * Retrieves a specific user from the database.
     *
     * @param username user to search for.
     * @return user from the database.
     * @throws UsernameNotFoundException if the user does not exist.
     */
    public PhegyUser getUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }

    /**
     * Retrieves a confirmed user from the database.
     *
     * @param username username to search for.
     * @return confirmed user from the database.
     * @throws UsernameNotFoundException if the user does not exist.
     * @throws PhegyHttpException         USER_NOT_CONFIRMED if user is found but not confirmed.
     */
    public PhegyUser getConfirmedUser(String username) throws PhegyHttpException, UsernameNotFoundException {
        final PhegyUser user = this.getUserByUsername(username);
        if (!user.isConfirmed()) {
            throw new PhegyHttpException("USER_NOT_CONFIRMED", HttpStatus.METHOD_NOT_ALLOWED);
        }
        return user;
    }

    /**
     * Creates new user instance and save it to the database.
     *
     * @param username new account's username.
     * @param email    email associated with the new account.
     * @param password password for the new account.
     * @return saved to the database user.
     * @throws PhegyHttpException when can not create new user.
     */
    @Transactional
    public PhegyUser createUser(String username, String email, String password) throws PhegyHttpException {
        // validate username, email and password
        userVerifier.verifyUsername(username);
        userVerifier.verifyEmail(email);
        userVerifier.verifyPassword(password);

        // create user instance
        final PhegyUser user = PhegyUser.builder()
                .username(username.trim())
                .email(email.trim())
                .encodedPassword(passwordEncoder.encode(password))
                .build();

        // mark user account as not confirmed
        final PhegyRole userRole = this.roleRepository.getByLevel(PhegyRoleLevel.NOT_CONFIRMED_USER);
        user.addRole(userRole);

        // validate user and save it to the database
        modelValidatorService.validate(user);
        PhegyUser savedUser = userRepository.save(user);

        pointsBagRepository.save(PointsBag.builder().points(0d).user(user).build());

        // generate personalized profile pic and save it to cloud storage
        try {
            final byte[] profilePic = this.imageGeneratorService.generateProfilePic(savedUser.getUsername());
            this.cloudStorageService.upload(profilePic, savedUser.getUsername() + ".png", StoragePath.USER);
        } catch (Exception e) {
            // skip profile pic generation
        }

        return savedUser;
    }

    /**
     * Change email of a specific user account.
     *
     * @param newEmail new email to associate with the user account.
     * @param username     user to be updated.
     * @return updated user.
     * @throws PhegyHttpException when can not update email.
     */
    public PhegyUser changeUserEmail(String newEmail, String username) throws PhegyHttpException {
        // validate the new email
        this.userVerifier.verifyEmail(newEmail);

        // get user
        final PhegyUser user = getUserByUsername(username);

        // update email
        user.setEmail(newEmail.trim());

        // mark user account as not confirmed
        user.removeRole(PhegyRoleLevel.USER);
        user.addRole(this.roleRepository.getByLevel(PhegyRoleLevel.NOT_CONFIRMED_USER));

        // validate user and save it to the database
        this.modelValidatorService.validate(user);
        final PhegyUser updatedUser = userRepository.save(user);

        // publish email confirmation needed event
        this.applicationEventPublisher.publishEvent(new OnEmailConfirmationNeededEvent(this, updatedUser));

        return updatedUser;
    }

    /**
     * Change password of a specific user account
     *
     * @param oldPassword     old password
     * @param newPassword     new password
     * @param confirmPassword confirm new password
     * @param username        username of the user whose password to change
     * @throws PhegyHttpException PASSWORDS_DOES_NOT_MATCH if new password and confirm password does not match
     * @throws PhegyHttpException WRONG_OLD_PASSWORD if old password is wrong
     * @throws PhegyHttpException NEW_PASSWORD_AND_OLD_PASSWORD_ARE_THE_SAME if old password and new password are the same
     */
    public void changePassword(String oldPassword, String newPassword, String confirmPassword, String username)
            throws PhegyHttpException {
        // retrieve user from database
        final PhegyUser user = this.getUserByUsername(username);

        // check if new password and confirm password match
        if (!newPassword.equals(confirmPassword)) {
            throw new PhegyHttpException("PASSWORDS_DOES_NOT_MATCH", HttpStatus.BAD_REQUEST);
        }

        // check if old password is correct
        if (!passwordEncoder.matches(oldPassword, user.getEncodedPassword())) {
            throw new PhegyHttpException("WRONG_OLD_PASSWORD", HttpStatus.BAD_REQUEST);
        }

        // check if new password is different from the old one
        if (passwordEncoder.matches(newPassword, user.getEncodedPassword())) {
            throw new PhegyHttpException("NEW_PASSWORD_AND_OLD_PASSWORD_ARE_THE_SAME", HttpStatus.BAD_REQUEST);
        }

        // validate the new password
        this.userVerifier.verifyPassword(newPassword);

        // update password
        user.setEncodedPassword(this.passwordEncoder.encode(newPassword));

        // validate user and save it to the database
        this.modelValidatorService.validate(user);
        userRepository.save(user);
    }

    /**
     * Change profile picture of a specific user account
     *
     * @param image    new profile image multipart file
     * @param username username of the user whose image to update
     * @throws PhegyHttpException CAN_NOT_READ_IMAGE_BYTES if multipart file bytes can't be read
     * @throws PhegyHttpException CAN_NOT_SAVE_IMAGE if image can not be uploaded to the cloud
     */
    public void setProfileImage(MultipartFile image, String username) {
        // retrieve confirmed user from database
        final PhegyUser user = this.getConfirmedUser(username);

        try {
            // try to upload image bytes to cloud storage
            final String imageId = user.getUsername() + ".png";
            this.cloudStorageService.upload(image.getBytes(), imageId, StoragePath.USER);
        } catch (IOException e) {
            throw new PhegyHttpException("CAN_NOT_READ_IMAGE_BYTES", HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            throw new PhegyHttpException("CAN_NOT_SAVE_IMAGE", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
