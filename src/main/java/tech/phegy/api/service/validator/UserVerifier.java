package tech.phegy.api.service.validator;

import com.google.common.base.Strings;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tech.phegy.api.exception.PhegyHttpException;
import tech.phegy.api.repository.PhegyUserRepository;

/**
 * Service for validating user information.
 *
 * @author Nikita
 */
@Service
public class UserVerifier {
    private static final int PASSWORD_MIN_LENGTH = 6;
    private static final int PASSWORD_MAX_LENGTH = 50;

    private final PhegyUserRepository userRepository;
    private final EmailVerifier emailVerifier;

    /**
     * Constructs a UserVerifier with needed dependencies.
     */
    public UserVerifier(PhegyUserRepository userRepository, EmailVerifier emailVerifier) {
        this.userRepository = userRepository;
        this.emailVerifier = emailVerifier;
    }

    /**
     * Verify that provided username is valid.
     *
     * @param username the username to validate.
     * @throws PhegyHttpException USER_USERNAME_INVALID if is not valid username.
     * @throws PhegyHttpException USER_USERNAME_EXISTS if account with this username already exists.
     */
    public void verifyUsername(String username) throws PhegyHttpException {
        if (username == null
                || username.isBlank()
                || username.trim().contains(" ")) {
            throw new PhegyHttpException("USER_USERNAME_INVALID", HttpStatus.BAD_REQUEST);
        }

        if (userRepository.existsByUsername(username.trim())) {
            throw new PhegyHttpException("USER_USERNAME_EXISTS", HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Verify that provided email is valid.
     *
     * @param email the email to validate.
     * @throws PhegyHttpException USER_EMAIL_INVALID if is not valid email.
     * @throws PhegyHttpException USER_EMAIL_EXISTS if account with this email already exists.
     */
    public void verifyEmail(String email) throws PhegyHttpException {
        if (!emailVerifier.isValidEmail(email)) {
            throw new PhegyHttpException("USER_EMAIL_INVALID", HttpStatus.BAD_REQUEST);
        }

        if (userRepository.existsByEmail(email.trim())) {
            throw new PhegyHttpException("USER_EMAIL_EXISTS", HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Verify that provided password is valid.
     *
     * @param password the password to validate.
     * @throws PhegyHttpException USER_PASSWORD_NULL if password is null or empty.
     * @throws PhegyHttpException USER_PASSWORD_TOO_SHORT if password is less than {@value PASSWORD_MIN_LENGTH} characters.
     * @throws PhegyHttpException USER_PASSWORD_TOO_LONG if password is more than {@value PASSWORD_MAX_LENGTH} characters.
     * @throws PhegyHttpException USER_PASSWORD_NO_DIGITS if password contains no digits.
     * @throws PhegyHttpException USER_PASSWORD_NO_ALPHABETIC_CHARACTERS if password contains no alphabetic characters.
     */
    public void verifyPassword(String password) throws PhegyHttpException {
        if (Strings.isNullOrEmpty(password)) {
            throw new PhegyHttpException("USER_PASSWORD_NULL", HttpStatus.BAD_REQUEST);
        }

        if (password.length() < PASSWORD_MIN_LENGTH) {
            throw new PhegyHttpException("USER_PASSWORD_TOO_SHORT", HttpStatus.BAD_REQUEST);
        }

        if (password.length() > PASSWORD_MAX_LENGTH) {
            throw new PhegyHttpException("USER_PASSWORD_TOO_LONG", HttpStatus.BAD_REQUEST);
        }

        boolean hasDigits = password.chars().anyMatch(Character::isDigit);
        if (!hasDigits) {
            throw new PhegyHttpException("USER_PASSWORD_NO_DIGITS", HttpStatus.BAD_REQUEST);
        }

        boolean hasAlphabeticCharacters = password.chars().anyMatch(Character::isAlphabetic);
        if (!hasAlphabeticCharacters) {
            throw new PhegyHttpException("USER_PASSWORD_NO_ALPHABETIC_CHARACTERS", HttpStatus.BAD_REQUEST);
        }
    }
}
