package tech.phegy.api.service.register;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tech.phegy.api.exception.PhegyHttpException;
import tech.phegy.api.repository.PhegyRoleRepository;
import tech.phegy.api.repository.PhegyUserRepository;
import tech.phegy.api.service.register.event.OnEmailConfirmTokenNoLongerValidEvent;
import tech.phegy.api.model.token.EmailConfirmationToken;
import tech.phegy.api.model.user.PhegyRoleLevel;
import tech.phegy.api.model.user.PhegyUser;
import tech.phegy.api.service.PhegyUserService;
import tech.phegy.api.service.EmailService;
import tech.phegy.api.service.register.event.OnEmailConfirmationNeededEvent;

/**
 * Service for managing user registrations.
 *
 * @author Nikita
 */
@Service
public class RegisterService {
    private final PhegyRoleRepository roleRepository;
    private final PhegyUserRepository userRepository;
    private final PhegyUserService userService;
    private final EmailConfirmationTokenService emailConfirmationTokenService;
    private final EmailService emailService;
    private final ApplicationEventPublisher eventPublisher;
    private final RegisterProps registerProps;

    /**
     * Constructs new instance with needed dependencies.
     */
    public RegisterService(PhegyRoleRepository roleRepository,
                           PhegyUserRepository userRepository,
                           PhegyUserService userService,
                           EmailConfirmationTokenService emailConfirmationTokenService,
                           EmailService emailService, ApplicationEventPublisher eventPublisher,
                           RegisterProps registerConfig) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.emailConfirmationTokenService = emailConfirmationTokenService;
        this.emailService = emailService;
        this.eventPublisher = eventPublisher;
        this.registerProps = registerConfig;
    }

    /**
     * Creates new user account and sends confirmation.
     *
     * @param username new account's username.
     * @param email    email associated with new account.
     * @param password password for the new account.
     * @throws PhegyHttpException when can not register new user account.
     */
    public void registerUser(String username, String email, String password) throws PhegyHttpException {
        PhegyUser user = userService.createUser(username, email, password);
        this.eventPublisher.publishEvent(new OnEmailConfirmationNeededEvent(this, user));
    }

    /**
     * Send account activation link to user.
     *
     * @param user to receive activation link.
     */
    public void sendActivationLink(PhegyUser user) {
        String confirmationToken = emailConfirmationTokenService.createToken(user);
        this.emailService.sendToken(user, confirmationToken);
    }

    /**
     * Resend account activation link to user.
     *
     * @param username to receive activation link.
     * @return minimal seconds delay between activation link requests.
     * @throws PhegyHttpException when activation link is requested too soon.
     */
    public long resendActivationLink(String username) throws PhegyHttpException {
        final PhegyUser user = this.userService.getUserByUsername(username);
        if (this.emailConfirmationTokenService.isNewActivationTokenAvailable(user)) {
            this.sendActivationLink(user);
        }

        return registerProps.getTokenMinimalDelaySeconds();
    }

    /**
     * Activate user associated with the given activation token.
     *
     * @param token activation token.
     * @throws PhegyHttpException when confirmation token is expired or user is already confirmed.
     */
    public void activateUser(String token) throws PhegyHttpException {
        final EmailConfirmationToken confirmationToken = emailConfirmationTokenService
                .getConfirmationToken(token);

        if (confirmationToken.isExpired()) {
            this.eventPublisher.publishEvent(
                    new OnEmailConfirmTokenNoLongerValidEvent(this, confirmationToken));

            throw new PhegyHttpException("CONFIRM_TOKEN_EXPIRED", HttpStatus.NOT_ACCEPTABLE);
        }

        PhegyUser user = confirmationToken.getUser();
        if (user.isConfirmed()) {
            this.eventPublisher.publishEvent(
                    new OnEmailConfirmTokenNoLongerValidEvent(this, confirmationToken));

            throw new PhegyHttpException("USER_ALREADY_ENABLED", HttpStatus.METHOD_NOT_ALLOWED);
        }

        user.removeRole(PhegyRoleLevel.NOT_CONFIRMED_USER);
        user.addRole(this.roleRepository.getByLevel(PhegyRoleLevel.USER));
        this.userRepository.save(user);

        this.eventPublisher
                .publishEvent(new OnEmailConfirmTokenNoLongerValidEvent(this, confirmationToken));
    }
}
