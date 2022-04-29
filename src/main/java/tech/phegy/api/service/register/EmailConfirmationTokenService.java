package tech.phegy.api.service.register;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tech.phegy.api.exception.PhegyHttpException;
import tech.phegy.api.repository.EmailConfirmationTokenRepository;
import tech.phegy.api.service.validator.ModelValidatorService;
import tech.phegy.api.model.token.EmailConfirmationToken;
import tech.phegy.api.model.user.PhegyUser;
import tech.phegy.api.model.token.Token;
import tech.phegy.api.service.HashingService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing email confirmation tokens.
 *
 * @author Nikita
 */
@Service
public class EmailConfirmationTokenService {
    private final RegisterProps registerProps;
    private final EmailConfirmationTokenRepository emailConfirmationTokenRepository;
    private final ModelValidatorService modelValidatorService;
    private final HashingService hashingService;

    /**
     * Constructs new instance with needed dependencies.
     */
    public EmailConfirmationTokenService(RegisterProps registerConfig,
                                         EmailConfirmationTokenRepository emailConfirmationTokenRepository,
                                         ModelValidatorService modelValidatorService,
                                         HashingService hashingService) {
        this.registerProps = registerConfig;
        this.emailConfirmationTokenRepository = emailConfirmationTokenRepository;
        this.modelValidatorService = modelValidatorService;
        this.hashingService = hashingService;
    }

    /**
     * Create new email confirmation token associated with a specific user.
     *
     * @param user to be created activation token for.
     * @return activation token.
     * @throws PhegyHttpException when can not create confirmation token.
     */
    public String createToken(PhegyUser user) throws PhegyHttpException {
        if (user.isConfirmed()) {
            throw new PhegyHttpException("USER_ALREADY_ENABLED", HttpStatus.BAD_REQUEST);
        }

        final String token = UUID.randomUUID().toString();

        EmailConfirmationToken emailConfirmationToken = EmailConfirmationToken.builder()
                .hashedToken(hashingService.hashString(token))
                .createdAt(LocalDateTime.now())
                .expirationTime(Duration.ofDays(registerProps.getTokenExpirationDays()))
                .user(user)
                .originEmail(user.getEmail())
                .build();

        modelValidatorService.validate(emailConfirmationToken);
        emailConfirmationTokenRepository.save(emailConfirmationToken);

        return token;
    }

    /**
     * Get email confirmation token object from token string.
     *
     * @param token token string.
     * @return email confirmation tokne.
     * @throws PhegyHttpException when confirmation token is not found.
     */
    public EmailConfirmationToken getConfirmationToken(String token) throws PhegyHttpException {
        return this.emailConfirmationTokenRepository.getByHashedToken(hashingService.hashString(token))
                .orElseThrow(() -> new PhegyHttpException("CONFIRM_TOKEN_INVALID", HttpStatus.NOT_FOUND));
    }

    /**
     * Delete all email confirmation tokens realated to user.
     *
     * @param user to delete activation tokens.
     */
    public void deleteAllTokens(PhegyUser user) {
        this.emailConfirmationTokenRepository.deleteAll(user.getEmailConfirmationTokens());
    }

    /**
     * Delete all expired email confirmation tokens related to user.
     *
     * @param user to delete activation tokens.
     */
    public void deleteAllExpiredTokens(PhegyUser user) {
        this.emailConfirmationTokenRepository.deleteAll(user
                .getEmailConfirmationTokens()
                .stream()
                .filter(EmailConfirmationToken::isExpired)
                .collect(Collectors.toSet()));
    }

    /**
     * Chack activation token availability.
     *
     * @param user to check activation token.
     * @return true when can create new token.
     * @throws PhegyHttpException when user is already enabled or new token is requested too soon.
     */
    public boolean isNewActivationTokenAvailable(PhegyUser user) throws PhegyHttpException {
        if (user.isConfirmed()) {
            throw new PhegyHttpException("USER_ALREADY_ENABLED", HttpStatus.BAD_REQUEST);
        }

        final Optional<EmailConfirmationToken> optionalToken = user.getEmailConfirmationTokens()
                .stream()
                .max(Comparator.comparing(Token::getCreatedAt));

        if (optionalToken.isPresent()) {
            EmailConfirmationToken token = optionalToken.get();
            LocalDateTime nowDateTime = LocalDateTime.now();
            LocalDateTime canCreateNewTokenDateTime = token.getCreatedAt()
                    .plusSeconds(this.registerProps.getTokenMinimalDelaySeconds());

            if (nowDateTime.isBefore(canCreateNewTokenDateTime)) {
                long seconds = nowDateTime.until(canCreateNewTokenDateTime, ChronoUnit.SECONDS);
                throw new PhegyHttpException("CAN_NOT_SENT_NEW_TOKEN_SECONDS_LEFT_" + seconds, HttpStatus.FORBIDDEN);
            }
        }

        return true;
    }
}
