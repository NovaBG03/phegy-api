package tech.phegy.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import tech.phegy.api.exception.PhegyHttpException;
import tech.phegy.api.service.register.RegisterProps;
import tech.phegy.api.service.validator.EmailVerifier;
import tech.phegy.api.model.user.PhegyUser;

/**
 * Service for sending emails.
 *
 * @author Nikita
 */
@Service
public class EmailService {
    private final JavaMailSender mailSender;
    private final EmailVerifier emailVerifier;
    private final RegisterProps registerConfig;
    private final String businessEmail;

    /**
     * Constructs new instance with needed dependencies.
     */
    public EmailService(JavaMailSender mailSender,
                        EmailVerifier emailVerifier,
                        RegisterProps registerConfig,
                        @Value("${spring.mail.username}") String businessEmail) {
        this.mailSender = mailSender;
        this.emailVerifier = emailVerifier;
        this.registerConfig = registerConfig;
        this.businessEmail = businessEmail;
    }

    /**
     * Send mail with confirmation token to a specific user.
     *
     * @param user user receiver.
     * @param confirmationToken token to send.
     */
    public void sendToken(PhegyUser user, String confirmationToken) {
        final String registerUrl = registerConfig.getTokenActivationUrl() + "/" + confirmationToken;
        final String subject = "Активирайте профила си във Phegy";
        final String content = "За да активирате профила си във Phegy, натиснете тук: " + registerUrl;

        this.sendEmail(user.getEmail(), subject, content);
    }

    /**
     * Send email from application business mail to a specific email.
     *
     * @param email mail receiver.
     * @param subject mail subject.
     * @param content mail content.
     * @throws PhegyHttpException when email is invalid or can not send email.
     */
    public void sendEmail(String email, String subject, String content) throws PhegyHttpException {
        if (!this.emailVerifier.isValidEmail(email)) {
            throw new PhegyHttpException("SENDING_EMAIL_INVALID", HttpStatus.BAD_REQUEST);
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(businessEmail);
        message.setTo(email);
        message.setSubject(subject);
        message.setText(content);

        try {
            mailSender.send(message);
        } catch (Exception e) {
            throw new PhegyHttpException("CAN_NOT_SEND", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
