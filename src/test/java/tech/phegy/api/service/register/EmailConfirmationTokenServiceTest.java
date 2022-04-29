package tech.phegy.api.service.register;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.phegy.api.exception.PhegyHttpException;
import tech.phegy.api.model.token.EmailConfirmationToken;
import tech.phegy.api.model.user.PhegyRole;
import tech.phegy.api.model.user.PhegyRoleLevel;
import tech.phegy.api.model.user.PhegyUser;
import tech.phegy.api.repository.EmailConfirmationTokenRepository;
import tech.phegy.api.service.HashingService;
import tech.phegy.api.service.validator.ModelValidatorService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class EmailConfirmationTokenServiceTest {
    @Mock
    RegisterProps registerProps;
    @Mock
    EmailConfirmationTokenRepository emailConfirmationTokenRepository;
    @Mock
    ModelValidatorService modelValidatorService;
    @Mock
    HashingService hashingService;

    EmailConfirmationTokenService emailConfirmationTokenService;

    @BeforeEach
    void setUp() {
        emailConfirmationTokenService = new EmailConfirmationTokenService(
                registerProps,
                emailConfirmationTokenRepository,
                modelValidatorService,
                hashingService
        );
    }

    @Test
    @DisplayName("Should create new activation token")
    void shouldCreateNewActivationToken() {
        PhegyRole notConfirmedRome = PhegyRole.builder().level(PhegyRoleLevel.NOT_CONFIRMED_USER).build();
        PhegyUser user = PhegyUser.builder().roles(Lists.newArrayList(notConfirmedRome)).build();

        when(hashingService.hashString(any())).thenReturn("hashed");

        String token = emailConfirmationTokenService.createToken(user);

        ArgumentCaptor<EmailConfirmationToken> emailConfirmationTokenArgumentCaptor = ArgumentCaptor.forClass(EmailConfirmationToken.class);
        verify(modelValidatorService).validate(any(EmailConfirmationToken.class));
        verify(emailConfirmationTokenRepository).save(emailConfirmationTokenArgumentCaptor.capture());

        String hashedToken = emailConfirmationTokenArgumentCaptor.getValue().getHashedToken();
        assertThat(hashedToken).isNotEqualTo(token);
    }

    @Test
    @DisplayName("Should throw exception when user requesting activation token is already activated")
    void shouldThrowExceptionWhenUserRequestingActivationTokenIsAlreadyActivated() {
        PhegyRole userRole = PhegyRole.builder().level(PhegyRoleLevel.USER).build();
        PhegyUser user = PhegyUser.builder().roles(Lists.newArrayList(userRole)).build();

        assertThatThrownBy(() -> emailConfirmationTokenService.createToken(user))
                .isInstanceOf(PhegyHttpException.class)
                .hasMessage("USER_ALREADY_ENABLED");
    }

    @Test
    @DisplayName("Should get confirmation token successfully")
    void shouldGetConfirmationTokenSuccessfully() {
        String token = "emailconfirmationtoken";
        String hashedToken = "hashedtoken";
        EmailConfirmationToken emailConfirmationToken = EmailConfirmationToken.builder()
                .hashedToken(hashedToken)
                .build();

        when(hashingService.hashString(token)).thenReturn(hashedToken);
        when(emailConfirmationTokenRepository.getByHashedToken(hashedToken))
                .thenReturn(Optional.of(emailConfirmationToken));

        EmailConfirmationToken actualConfirmationToken = emailConfirmationTokenService.getConfirmationToken(token);
        assertThat(actualConfirmationToken).isEqualTo(emailConfirmationToken);
    }

    @Test
    @DisplayName("Should throw exception when confirmation token invalid")
    void shouldThrowExceptionWhenConfirmationTokenInvalid() {
        String token = "emailconfirmationtoken";
        String hashedToken = "hashedtoken";

        when(hashingService.hashString(token)).thenReturn(hashedToken);
        when(emailConfirmationTokenRepository.getByHashedToken(hashedToken)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emailConfirmationTokenService.getConfirmationToken(token))
                .isInstanceOf(PhegyHttpException.class)
                .hasMessage("CONFIRM_TOKEN_INVALID");
    }

    @Test
    @DisplayName("Should delete all confirmation tokens")
    void shouldDeleteAllConfirmationTokens() {
        Collection<EmailConfirmationToken> confirmationTokns = Lists.newArrayList(
                EmailConfirmationToken.builder().build(),
                EmailConfirmationToken.builder().build(),
                EmailConfirmationToken.builder().build()
        );
        PhegyUser user = PhegyUser.builder()
                .emailConfirmationTokens(confirmationTokns)
                .build();

        emailConfirmationTokenService.deleteAllTokens(user);

        verify(emailConfirmationTokenRepository).deleteAll(confirmationTokns);
    }

    @Test
    @DisplayName("Should delete all expired confirmation tokens")
    void shouldDeleteAllExpiredConfirmationTokens() {
        String email = "dev@test.com";
        PhegyUser user = PhegyUser.builder()
                .email(email)
                .build();
        Collection<EmailConfirmationToken> confirmationTokns = Lists.newArrayList(
                EmailConfirmationToken.builder()
                        .originEmail(email)
                        .createdAt(LocalDateTime.now())
                        .expirationTime(Duration.ZERO)
                        .user(user)
                        .build(),
                EmailConfirmationToken.builder()
                        .originEmail(email)
                        .createdAt(LocalDateTime.now())
                        .expirationTime(Duration.ZERO)
                        .user(user)
                        .build(),
                EmailConfirmationToken.builder()
                        .originEmail(email)
                        .createdAt(LocalDateTime.now())
                        .expirationTime(Duration.ofHours(2))
                        .user(user)
                        .build()
        );
        user.setEmailConfirmationTokens(confirmationTokns);

        emailConfirmationTokenService.deleteAllExpiredTokens(user);

        ArgumentCaptor<Collection<EmailConfirmationToken>> confirmationTokensArgumentCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(emailConfirmationTokenRepository).deleteAll(confirmationTokensArgumentCaptor.capture());
        Collection<EmailConfirmationToken> confirmationTokens = confirmationTokensArgumentCaptor.getValue();
        assertThat(confirmationTokens.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should be able to create new activation token when no old ones available")
    void shouldBeAbleToCreateNewActivationTokenWhenNoOldOnesAvailable() {
        String email = "dev@test.com";
        PhegyRole notConfirmedRole = PhegyRole.builder().level(PhegyRoleLevel.NOT_CONFIRMED_USER).build();
        PhegyUser user = PhegyUser.builder()
                .email(email)
                .roles(Lists.newArrayList(notConfirmedRole))
                .emailConfirmationTokens(Lists.newArrayList())
                .build();

        boolean actual = emailConfirmationTokenService.isNewActivationTokenAvailable(user);
        assertThat(actual).isTrue();
    }

    @Test
    @DisplayName("Should be able to create new activation token when minimum delay is passed")
    void shouldBeAbleToCreateNewActivationTokenWhenMinimumDelayIsPassed() {
        String email = "dev@test.com";
        PhegyRole notConfirmedRole = PhegyRole.builder().level(PhegyRoleLevel.NOT_CONFIRMED_USER).build();
        PhegyUser user = PhegyUser.builder()
                .email(email)
                .roles(Lists.newArrayList(notConfirmedRole))
                .build();
        Collection<EmailConfirmationToken> confirmationTokens = Lists.newArrayList(
                EmailConfirmationToken.builder()
                        .user(user)
                        .originEmail(email)
                        .createdAt(LocalDateTime.now())
                        .expirationTime(Duration.ofHours(2))
                        .build()
        );
        user.setEmailConfirmationTokens(confirmationTokens);

        boolean actual = emailConfirmationTokenService.isNewActivationTokenAvailable(user);
        assertThat(actual).isTrue();
    }

    @Test
    @DisplayName("Should not be able to create new activation token when minimum delay is not passed")
    void shouldNotBeAbleToCreateNewActivationTokenWhenMinimumDelayIsNotPassed() {
        String email = "dev@test.com";
        PhegyRole notConfirmedRole = PhegyRole.builder().level(PhegyRoleLevel.NOT_CONFIRMED_USER).build();
        PhegyUser user = PhegyUser.builder()
                .email(email)
                .roles(Lists.newArrayList(notConfirmedRole))
                .build();
        Collection<EmailConfirmationToken> confirmationTokens = Lists.newArrayList(
                EmailConfirmationToken.builder()
                        .user(user)
                        .originEmail(email)
                        .createdAt(LocalDateTime.now())
                        .expirationTime(Duration.ZERO)
                        .build()
        );
        user.setEmailConfirmationTokens(confirmationTokens);

        when(registerProps.getTokenMinimalDelaySeconds()).thenReturn(36000L);

        assertThatThrownBy(() -> emailConfirmationTokenService.isNewActivationTokenAvailable(user))
                .isInstanceOf(PhegyHttpException.class)
                .hasMessageContaining("CAN_NOT_SENT_NEW_TOKEN_SECONDS_LEFT_");
    }

    @Test
    @DisplayName("Should not be able to create new activation token when user is activated")
    void shouldNotBeAbleToCreateNewActivationTokenWhenUserIsActivated() {
        String email = "dev@test.com";
        PhegyRole userRole = PhegyRole.builder().level(PhegyRoleLevel.USER).build();
        PhegyUser user = PhegyUser.builder()
                .email(email)
                .roles(Lists.newArrayList(userRole))
                .build();

        assertThatThrownBy(() -> emailConfirmationTokenService.isNewActivationTokenAvailable(user))
                .isInstanceOf(PhegyHttpException.class)
                .hasMessage("USER_ALREADY_ENABLED");
    }
}