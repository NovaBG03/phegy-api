package tech.phegy.api.service;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;
import tech.phegy.api.exception.PhegyHttpException;
import tech.phegy.api.model.user.PhegyRole;
import tech.phegy.api.model.user.PhegyRoleLevel;
import tech.phegy.api.model.user.PhegyUser;
import tech.phegy.api.repository.PhegyRoleRepository;
import tech.phegy.api.repository.PhegyUserRepository;
import tech.phegy.api.repository.PointsBagRepository;
import tech.phegy.api.service.imageGenerator.ImageGeneratorService;
import tech.phegy.api.service.register.event.OnEmailConfirmationNeededEvent;
import tech.phegy.api.service.storage.CloudStorageService;
import tech.phegy.api.service.storage.StoragePath;
import tech.phegy.api.service.validator.UserVerifier;
import tech.phegy.api.service.validator.ModelValidatorService;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhegyUserServiceTest {
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    @Mock()
    PhegyUserRepository userRepository;
    @Mock()
    PointsBagRepository pointsBagRepository;
    @Mock()
    PhegyRoleRepository roleRepository;
    @Mock()
    CloudStorageService cloudStorageService;
    @Mock()
    ImageGeneratorService imageGeneratorService;
    @Mock()
    UserVerifier userVerifier;
    @Mock()
    ModelValidatorService modelValidatorService;
    @Mock()
    ApplicationEventPublisher applicationEventPublisher;

    PhegyUserService userService;

    String username = "ivan";

    @BeforeEach
    void initUserService() {
        userService = new PhegyUserService(passwordEncoder,
                userRepository,
                pointsBagRepository,
                roleRepository,
                cloudStorageService,
                imageGeneratorService,
                userVerifier,
                modelValidatorService,
                applicationEventPublisher);
    }

    @Test
    @DisplayName("Should get user by username successfully")
    void shouldGetUserByUsernameSuccessfully() {
        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(PhegyUser.builder().username(username).build()));

        final PhegyUser user = userService.getUserByUsername(username);

        assertThat(user.getUsername()).isEqualTo(username);
    }

    @Test
    @DisplayName("Should throw exception when get user username not found")
    void shouldThrowExceptionWhenGetUserUsernameNotFound() {
        when(userRepository.findByUsername(username))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByUsername(username))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("Should load user details successfully")
    void shouldLoadUserDetailsSuccessfully() {
        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(PhegyUser.builder().username(username).build()));

        final UserDetails userDetails = userService.loadUserByUsername(username);

        assertThat(userDetails.getUsername()).isEqualTo(username);
    }

    @Test
    @DisplayName("Should throw exception when load user details username not found")
    void shouldThrowExceptionWhenLoadUserDetailsUsernameNotFound() {
        when(userRepository.findByUsername(username))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loadUserByUsername(username))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("Should get confirmed user by username successfully")
    void shouldGetConfirmedUserByUsernameSuccessfully() {
        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(PhegyUser.builder()
                        .username(username)
                        .roles(List.of(PhegyRole.builder().level(PhegyRoleLevel.USER).build()))
                        .build()));

        PhegyUser user = userService.getConfirmedUser(username);

        assertThat(user.getUsername()).isEqualTo(username);
    }

    @Test
    @DisplayName("Should throw exception when get confirmed user not confirmed")
    void shouldThrowExceptionWhenGetConfirmedUserNotConfirmed() {
        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(PhegyUser.builder()
                        .username(username)
                        .roles(List.of(PhegyRole.builder().level(PhegyRoleLevel.NOT_CONFIRMED_USER).build()))
                        .build()));

        assertThatThrownBy(() -> userService.getConfirmedUser(username))
                .isInstanceOf(PhegyHttpException.class)
                .hasMessage("USER_NOT_CONFIRMED");
    }

    @Test
    @DisplayName("Should throw exception when get confirmed user not found")
    void shouldThrowExceptionWhenGetConfirmedUserNotFound() {
        when(userRepository.findByUsername(username))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getConfirmedUser(username))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("Should create new user successfully")
    void shouldCreateNewUserSuccessfully() {
        String email = "test@abv.bg";
        String password = "test";
        byte[] generatedImageBytes = new byte[100];

        when(roleRepository.getByLevel(PhegyRoleLevel.NOT_CONFIRMED_USER))
                .thenReturn(getRole(PhegyRoleLevel.NOT_CONFIRMED_USER));

        when(userRepository.save(any(PhegyUser.class)))
                .then(AdditionalAnswers.returnsFirstArg());

        when(imageGeneratorService.generateProfilePic(username))
                .thenReturn(generatedImageBytes);

        PhegyUser user = userService.createUser(username, email, password);

        assertThat(user.getUsername()).isEqualTo(username);
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getEncodedPassword()).matches(x -> passwordEncoder.matches(password, x));
        assertThat(user.isConfirmed()).isFalse();
        verify(userVerifier).verifyUsername(username);
        verify(userVerifier).verifyEmail(email);
        verify(userVerifier).verifyPassword(password);
        verify(modelValidatorService, atLeastOnce()).validate(any(PhegyUser.class));
        verify(userRepository, atLeastOnce()).save(any(PhegyUser.class));
        verify(imageGeneratorService).generateProfilePic(username);
        verify(cloudStorageService).upload(generatedImageBytes, username + ".png", StoragePath.USER);
    }

    @Test
    @DisplayName("Should create new user successfully when can not generate profile pic")
    void shouldCreateNewUserSuccessfullyWhenCanNotGenerateProfilePic() {
        String email = "test@abv.bg";
        String password = "test";

        when(roleRepository.getByLevel(PhegyRoleLevel.NOT_CONFIRMED_USER))
                .thenReturn(getRole(PhegyRoleLevel.NOT_CONFIRMED_USER));

        when(userRepository.save(any(PhegyUser.class)))
                .then(AdditionalAnswers.returnsFirstArg());

        when(imageGeneratorService.generateProfilePic(username))
                .thenThrow(PhegyHttpException.class);

        PhegyUser user = userService.createUser(username, email, password);

        assertThat(user.getUsername()).isEqualTo(username);
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getEncodedPassword()).matches(x -> passwordEncoder.matches(password, x));
        assertThat(user.isConfirmed()).isFalse();
        verify(userVerifier).verifyUsername(username);
        verify(userVerifier).verifyEmail(email);
        verify(userVerifier).verifyPassword(password);
        verify(modelValidatorService, atLeastOnce()).validate(any(PhegyUser.class));
        verify(userRepository, atLeastOnce()).save(any(PhegyUser.class));
        verify(imageGeneratorService).generateProfilePic(username);
        verify(cloudStorageService, never()).upload(any(), any(), any());
    }

    @Test
    @DisplayName("Should create new user successfully when can not upload profile pic to cloud storage")
    void shouldCreateNewUserSuccessfullyWhenCanNotUploadProfilePicToCloudStorage() {
        String email = "test@abv.bg";
        String password = "test";
        byte[] generatedImageBytes = new byte[100];

        when(roleRepository.getByLevel(PhegyRoleLevel.NOT_CONFIRMED_USER))
                .thenReturn(getRole(PhegyRoleLevel.NOT_CONFIRMED_USER));

        when(userRepository.save(any(PhegyUser.class)))
                .then(AdditionalAnswers.returnsFirstArg());

        when(imageGeneratorService.generateProfilePic(username))
                .thenReturn(generatedImageBytes);

        doThrow(new PhegyHttpException("SOMETHING_WENT_WONG", HttpStatus.BAD_REQUEST))
                .when(cloudStorageService)
                .upload(generatedImageBytes, username + ".png", StoragePath.USER);

        PhegyUser user = userService.createUser(username, email, password);

        assertThat(user.getUsername()).isEqualTo(username);
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getEncodedPassword()).matches(x -> passwordEncoder.matches(password, x));
        assertThat(user.isConfirmed()).isFalse();
        verify(userVerifier).verifyUsername(username);
        verify(userVerifier).verifyEmail(email);
        verify(userVerifier).verifyPassword(password);
        verify(modelValidatorService, atLeastOnce()).validate(any(PhegyUser.class));
        verify(userRepository, atLeastOnce()).save(any(PhegyUser.class));
        verify(imageGeneratorService).generateProfilePic(username);
        verify(cloudStorageService).upload(generatedImageBytes, username + ".png", StoragePath.USER);
    }

    @Test
    @DisplayName("Should change user email successfully")
    void shouldChangeUserEmailSuccessfully() {
        String oldEmail = "test@test.com";
        String newEmail = "new_email@gmail.com";

        PhegyUser user = PhegyUser.builder()
                .username(username)
                .email(oldEmail)
                .roles(Sets.newHashSet(getRole(PhegyRoleLevel.USER)))
                .build();


        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(roleRepository.getByLevel(PhegyRoleLevel.NOT_CONFIRMED_USER))
                .thenReturn(getRole(PhegyRoleLevel.NOT_CONFIRMED_USER));

        when(userRepository.save(any(PhegyUser.class)))
                .then(AdditionalAnswers.returnsFirstArg());

        PhegyUser updatedUser = userService.changeUserEmail(newEmail, username);

        assertThat(updatedUser.getEmail()).isEqualTo(newEmail);
        assertThat(user.isConfirmed()).isFalse();
        verify(userVerifier).verifyEmail(newEmail);
        verify(modelValidatorService).validate(user);
        verify(applicationEventPublisher).publishEvent(any(OnEmailConfirmationNeededEvent.class));
    }

    @Test
    @DisplayName("Should change admin email successfully")
    void shouldChangeAdminEmailSuccessfully() {
        String oldEmail = "test@test.com";
        String newEmail = "new_email@gmail.com";

        PhegyUser user = PhegyUser.builder()
                .username(username)
                .email(oldEmail)
                .roles(Sets.newHashSet(getRole(PhegyRoleLevel.ADMIN), getRole(PhegyRoleLevel.MODERATOR)))
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(roleRepository.getByLevel(PhegyRoleLevel.NOT_CONFIRMED_USER))
                .thenReturn(getRole(PhegyRoleLevel.NOT_CONFIRMED_USER));

        when(userRepository.save(any(PhegyUser.class)))
                .then(AdditionalAnswers.returnsFirstArg());

        PhegyUser updatedUser = userService.changeUserEmail(newEmail, username);

        assertThat(updatedUser.getEmail()).isEqualTo(newEmail);
        assertThat(updatedUser.isAdminOrModerator()).isTrue();
        assertThat(updatedUser.isConfirmed()).isFalse();
        verify(userVerifier).verifyEmail(newEmail);
        verify(modelValidatorService).validate(user);
        verify(userRepository).save(any(PhegyUser.class));
        verify(applicationEventPublisher).publishEvent(any(OnEmailConfirmationNeededEvent.class));
    }

    @Test
    @DisplayName("Should change password successfully")
    void shouldChangePasswordSuccessfully() {
        String oldPassword = "old_password";
        String newPassword = "new_password";
        String confirmPassword = "new_password";

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(PhegyUser.builder()
                        .username(username)
                        .encodedPassword(passwordEncoder.encode(oldPassword))
                        .build()));

        userService.changePassword(oldPassword, newPassword, confirmPassword, username);

        verify(userVerifier).verifyPassword(newPassword);
        verify(modelValidatorService).validate(any(PhegyUser.class));

        ArgumentCaptor<PhegyUser> userArgumentCaptor = ArgumentCaptor.forClass(PhegyUser.class);
        verify(userRepository).save(userArgumentCaptor.capture());

        final PhegyUser capturedUser = userArgumentCaptor.getValue();
        assertThat(capturedUser.getEncodedPassword()).matches(x -> passwordEncoder.matches(newPassword, x));
    }

    @Test
    @DisplayName("Should throw exception when change password new and confirm passwords does not match")
    void shouldThrowExceptionWhenChangePasswordNewAndConfirmPasswordsDoesNotMatch() {
        String oldPassword = "old_password";
        String newPassword = "new_password";
        String confirmPassword = "confirm_password";

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(PhegyUser.builder()
                        .username(username)
                        .encodedPassword(passwordEncoder.encode(oldPassword))
                        .build()));

        assertThatThrownBy(() -> userService.changePassword(oldPassword, newPassword, confirmPassword, username))
                .isInstanceOf(PhegyHttpException.class)
                .hasMessage("PASSWORDS_DOES_NOT_MATCH");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when change password wrong old password")
    void shouldThrowExceptionWhenChangePasswordWrongOldPassword() {
        String oldPassword = "old_password";
        String wrongOldPassword = "wrong_old_password";
        String newPassword = "new_password";
        String confirmPassword = "new_password";

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(PhegyUser.builder()
                        .username(username)
                        .encodedPassword(passwordEncoder.encode(oldPassword))
                        .build()));

        assertThatThrownBy(() -> userService.changePassword(wrongOldPassword, newPassword, confirmPassword, username))
                .isInstanceOf(PhegyHttpException.class)
                .hasMessage("WRONG_OLD_PASSWORD");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when change password new and old passwords are the same")
    void shouldThrowExceptionWhenChangePasswordNewAndOldPasswordsAreTheSame() {
        String oldPassword = "old_password";
        String newPassword = "old_password";
        String confirmPassword = "old_password";

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(PhegyUser.builder()
                        .username(username)
                        .encodedPassword(passwordEncoder.encode(oldPassword))
                        .build()));

        assertThatThrownBy(() -> userService.changePassword(oldPassword, newPassword, confirmPassword, username))
                .isInstanceOf(PhegyHttpException.class)
                .hasMessage("NEW_PASSWORD_AND_OLD_PASSWORD_ARE_THE_SAME");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should set profile image successfully")
    void shouldSetProfileImageSuccessfully() throws Exception {
        MultipartFile multipartFile = new MockMultipartFile("image", new byte[100]);

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(PhegyUser.builder()
                        .username(username)
                        .build()));

        userService.setProfileImage(multipartFile, username);

        verify(cloudStorageService).upload(multipartFile.getBytes(), username + ".png", StoragePath.USER);
    }

    @Test
    @DisplayName("Should throw exception when set profile image can not read image bytes")
    void shouldThrowExceptionWhenSetProfileImageCanNotReadImageBytes() {
        MultipartFile multipartFile = new MockMultipartFile("image", new byte[100]) {
            @NotNull
            @Override
            public byte[] getBytes() throws IOException {
                throw new IOException();
            }
        };

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(PhegyUser.builder()
                        .username(username)
                        .build()));

        assertThatThrownBy(() -> userService.setProfileImage(multipartFile, username))
                .isInstanceOf(PhegyHttpException.class)
                .hasMessage("CAN_NOT_READ_IMAGE_BYTES");

        verify(cloudStorageService, never()).upload(any(), any(), any());
    }

    @Test
    @DisplayName("Should throw exception when set profile image can not be saved")
    void shouldThrowExceptionWhenSetProfileImageCanNotBeSaved() throws Exception {
        MultipartFile multipartFile = new MockMultipartFile("image", new byte[100]);

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(PhegyUser.builder()
                        .username(username)
                        .build()));

        doThrow(new RuntimeException())
                .when(cloudStorageService)
                .upload(multipartFile.getBytes(), username + ".png", StoragePath.USER);

        assertThatThrownBy(() -> userService.setProfileImage(multipartFile, username))
                .isInstanceOf(PhegyHttpException.class)
                .hasMessage("CAN_NOT_SAVE_IMAGE");

        verify(cloudStorageService).upload(multipartFile.getBytes(), username + ".png", StoragePath.USER);
    }

    private PhegyRole getRole(PhegyRoleLevel roleLevel) {
        return PhegyRole.builder().level(roleLevel).build();
    }
}
