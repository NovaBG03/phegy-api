package tech.phegy.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import tech.phegy.api.exception.PhegyHttpException;
import tech.phegy.api.model.user.PhegyUser;
import tech.phegy.api.service.register.RegisterProps;
import tech.phegy.api.service.validator.EmailVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {
    @Mock
    JavaMailSender mailSender;
    @Mock
    EmailVerifier emailVerifier;
    @Mock
    RegisterProps registerProps;

    String businessEmail = "phegy@test.com";

    EmailService emailService;

    String email = "test@dev.com";
    String subject = "test mail";
    String content = "mail content";

    @BeforeEach
    void initEmailService() {
        emailService = new EmailService(mailSender, emailVerifier, registerProps, businessEmail);
    }

    @Test
    @DisplayName("Should send email successfully")
    void shouldSendEmailSuccessfully() {
        when(emailVerifier.isValidEmail(email)).thenReturn(true);

        emailService.sendEmail(email, subject, content);

        verify(emailVerifier).isValidEmail(email);

        ArgumentCaptor<SimpleMailMessage> simpleMailMessageArgumentCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(simpleMailMessageArgumentCaptor.capture());
        SimpleMailMessage message = simpleMailMessageArgumentCaptor.getValue();

        assertThat(message.getFrom()).isEqualTo(businessEmail);
        assertThat(message.getTo()).contains(email);
        assertThat(message.getSubject()).isEqualTo(subject);
        assertThat(message.getText()).isEqualTo(content);
    }

    @Test
    @DisplayName("Should throw exception when email is not valid")
    void shouldThrowExceptionWhenEmailIsNotValid() {
        when(emailVerifier.isValidEmail(email)).thenReturn(false);

        assertThatThrownBy(() -> emailService.sendEmail(email, subject, content))
                .isInstanceOf(PhegyHttpException.class)
                .hasMessage("SENDING_EMAIL_INVALID");
    }

    @Test
    @DisplayName("Should throw exception when can not sent message")
    void shouldThrowExceptionWhenCanNotSentMessage() {
        when(emailVerifier.isValidEmail(email)).thenReturn(true);
        doThrow(new RuntimeException()).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> emailService.sendEmail(email, subject, content))
                .isInstanceOf(PhegyHttpException.class)
                .hasMessage("CAN_NOT_SEND");
    }


    @Test
    @DisplayName("Should send confirmation token to user email successfully")
    void shouldSendConfirmationTokenToUserEmailSuccessfully() {
        String email = "user@dev.com";
        PhegyUser user = PhegyUser.builder().email(email).build();
        String confirmationToken = "confirmation token";

        when(emailVerifier.isValidEmail(email)).thenReturn(true);

        emailService.sendToken(user, confirmationToken);

        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}
