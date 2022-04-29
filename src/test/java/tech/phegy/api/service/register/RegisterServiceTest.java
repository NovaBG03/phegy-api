package tech.phegy.api.service.register;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import tech.phegy.api.exception.PhegyHttpException;
import tech.phegy.api.model.token.EmailConfirmationToken;
import tech.phegy.api.model.user.PhegyRole;
import tech.phegy.api.model.user.PhegyRoleLevel;
import tech.phegy.api.model.user.PhegyUser;
import tech.phegy.api.repository.PhegyRoleRepository;
import tech.phegy.api.repository.PhegyUserRepository;
import tech.phegy.api.service.PhegyUserService;
import tech.phegy.api.service.EmailService;
import tech.phegy.api.service.register.event.OnEmailConfirmTokenNoLongerValidEvent;
import tech.phegy.api.service.register.event.OnEmailConfirmationNeededEvent;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class RegisterServiceTest {
    @Mock
    PhegyRoleRepository roleRepository;
    @Mock
    PhegyUserRepository userRepository;
    @Mock
    PhegyUserService userService;
    @Mock
    EmailConfirmationTokenService emailConfirmationTokenService;
    @Mock
    EmailService emailService;
    @Mock
    ApplicationEventPublisher eventPublisher;
    @Mock
    RegisterProps registerProps;

    RegisterService registerService;

    @BeforeEach
    void setUp() {
        registerService = new RegisterService(roleRepository,
                userRepository,
                userService,
                emailConfirmationTokenService,
                emailService,
                eventPublisher,
                registerProps);
    }

    @Test
    @DisplayName("Should register new user successfully")
    void shouldRegisterNewUserSuccessfully() {
        UUID id = UUID.randomUUID();
        String username = "username";
        String email = "test@dev.com";
        String password = "password";
        PhegyUser user = PhegyUser.builder()
                .id(id)
                .username(username)
                .email(email)
                .encodedPassword(password)
                .build();

        when(userService.createUser(username, email, password)).thenReturn(user);

        registerService.registerUser(username, email, password);

        verify(userService).createUser(username, email, password);
        ArgumentCaptor<OnEmailConfirmationNeededEvent> eventArgumentCaptor = ArgumentCaptor.forClass(OnEmailConfirmationNeededEvent.class);
        verify(eventPublisher).publishEvent(eventArgumentCaptor.capture());
        OnEmailConfirmationNeededEvent onEmailConfirmationNeededEvent = eventArgumentCaptor.getValue();
        assertThat(onEmailConfirmationNeededEvent.getUser()).isEqualTo(user);
    }

    @Test
    @DisplayName("Should throw exception when can not register new user")
    void shouldThrowExceptionWhenCanNotRegisterNewUser() {
        String username = "username";
        String email = "test@dev.com";
        String password = "password";

        when(userService.createUser(username, email, password)).thenThrow(PhegyHttpException.class);

        assertThatThrownBy(() -> registerService.registerUser(username, email, password))
                .isInstanceOf(PhegyHttpException.class);

        verify(eventPublisher, never()).publishEvent(OnEmailConfirmationNeededEvent.class);
    }

    @Test
    @DisplayName("Should send activation link")
    void shouldSendActivationLink() {
        PhegyUser user = PhegyUser.builder().build();
        String confirmationToken = "confirmationtoken";
        when(emailConfirmationTokenService.createToken(user)).thenReturn(confirmationToken);

        registerService.sendActivationLink(user);

        verify(emailService).sendToken(user, confirmationToken);
    }

    @Test
    @DisplayName("Should resend activation link successfully")
    void shouldResendActivationLinkSuccessfully() {
        Long minDelaySeconds = 120L;
        String confirmationToken = "confirmationtoken";
        String username = "username";
        PhegyUser user = PhegyUser.builder()
                .username(username)
                .build();

        when(registerProps.getTokenMinimalDelaySeconds()).thenReturn(minDelaySeconds);
        when(userService.getUserByUsername(username)).thenReturn(user);
        when(emailConfirmationTokenService.isNewActivationTokenAvailable(user)).thenReturn(true);
        when(emailConfirmationTokenService.createToken(user)).thenReturn(confirmationToken);

        Long actualMinDelaySeconds = registerService.resendActivationLink(username);

        verify(emailService).sendToken(user, confirmationToken);
        assertThat(actualMinDelaySeconds).isEqualTo(minDelaySeconds);
    }

    @Test
    @DisplayName("Should throw exception when resend activation requested too soon")
    void shouldNotResendActivationLinkWhenCanNotCreateNewToken() {
        String confirmationToken = "confirmationtoken";
        String username = "username";
        PhegyUser user = PhegyUser.builder()
                .username(username)
                .build();

        when(userService.getUserByUsername(username)).thenReturn(user);
        when(emailConfirmationTokenService.isNewActivationTokenAvailable(user)).thenThrow(PhegyHttpException.class);

        assertThatThrownBy(() -> registerService.resendActivationLink(username))
                .isInstanceOf(PhegyHttpException.class);

        verify(emailService, never()).sendToken(user, confirmationToken);
    }

    @Test
    @DisplayName("Should activate user successfully")
    void shouldActivateUserSuccessfully() {
        String token = "activationtoken";
        String email = "dev@test.com";
        PhegyRole userRole = PhegyRole.builder().level(PhegyRoleLevel.USER).build();
        PhegyRole notConfirmedRole = PhegyRole.builder().level(PhegyRoleLevel.NOT_CONFIRMED_USER).build();
        PhegyUser user = PhegyUser.builder()
                .email(email)
                .roles(Lists.newArrayList(notConfirmedRole))
                .build();
        EmailConfirmationToken emailConfirmationToken = EmailConfirmationToken.builder()
                .id(1L)
                .user(user)
                .originEmail(email)
                .createdAt(LocalDateTime.now())
                .expirationTime(Duration.ofHours(2))
                .build();

        when(emailConfirmationTokenService.getConfirmationToken(token)).thenReturn(emailConfirmationToken);
        when(roleRepository.getByLevel(PhegyRoleLevel.USER)).thenReturn(userRole);

        registerService.activateUser(token);

        ArgumentCaptor<PhegyUser> userArgumentCaptor = ArgumentCaptor.forClass(PhegyUser.class);
        verify(userRepository).save(userArgumentCaptor.capture());
        PhegyUser capturedUser = userArgumentCaptor.getValue();
        assertThat(capturedUser)
                .matches(x -> x.getRoles().contains(userRole), "contains user role")
                .matches(x -> !x.getRoles().contains(notConfirmedRole), "does not contains not confirmed role")
                .matches(PhegyUser::isConfirmed, "is confirmed")
                .matches(x -> !x.isAdminOrModerator(), "is not admin or moderator");
    }

    @Test
    @DisplayName("Should activate admin successfully")
    void shouldActivateAdminSuccessfully() {
        String token = "activationtoken";
        String email = "dev@test.com";
        PhegyRole adminRole = PhegyRole.builder().level(PhegyRoleLevel.ADMIN).build();
        PhegyRole userRole = PhegyRole.builder().level(PhegyRoleLevel.USER).build();
        PhegyRole notConfirmedRole = PhegyRole.builder().level(PhegyRoleLevel.NOT_CONFIRMED_USER).build();
        PhegyUser user = PhegyUser.builder()
                .email(email)
                .roles(Lists.newArrayList(notConfirmedRole, adminRole))
                .build();
        EmailConfirmationToken emailConfirmationToken = EmailConfirmationToken.builder()
                .id(1L)
                .user(user)
                .originEmail(email)
                .createdAt(LocalDateTime.now())
                .expirationTime(Duration.ofHours(2))
                .build();

        when(emailConfirmationTokenService.getConfirmationToken(token)).thenReturn(emailConfirmationToken);
        when(roleRepository.getByLevel(PhegyRoleLevel.USER)).thenReturn(userRole);

        registerService.activateUser(token);

        ArgumentCaptor<PhegyUser> userArgumentCaptor = ArgumentCaptor.forClass(PhegyUser.class);
        verify(userRepository).save(userArgumentCaptor.capture());
        PhegyUser capturedUser = userArgumentCaptor.getValue();
        assertThat(capturedUser)
                .matches(x -> x.getRoles().contains(userRole), "contains user role")
                .matches(x -> x.getRoles().contains(adminRole), "contains admin role")
                .matches(x -> !x.getRoles().contains(notConfirmedRole), "does not contains not confirmed role")
                .matches(PhegyUser::isConfirmed, "is confirmed")
                .matches(PhegyUser::isAdminOrModerator, "is admin or moderator");

        verify(eventPublisher).publishEvent(any(OnEmailConfirmTokenNoLongerValidEvent.class));
    }

    @Test
    @DisplayName("Should throw exception when user is already confirmed")
    void shouldThrowExceptionWhenUserIsAlreadyConfirmed() {
        String token = "activationtoken";
        String email = "dev@test.com";
        PhegyRole userRole = PhegyRole.builder().level(PhegyRoleLevel.USER).build();
        PhegyUser user = PhegyUser.builder()
                .email(email)
                .roles(Lists.newArrayList(userRole))
                .build();
        EmailConfirmationToken emailConfirmationToken = EmailConfirmationToken.builder()
                .id(1L)
                .user(user)
                .originEmail(email)
                .createdAt(LocalDateTime.now())
                .expirationTime(Duration.ofHours(2))
                .build();

        when(emailConfirmationTokenService.getConfirmationToken(token)).thenReturn(emailConfirmationToken);

        assertThatThrownBy(() -> registerService.activateUser(token))
                .isInstanceOf(PhegyHttpException.class)
                .hasMessage("USER_ALREADY_ENABLED");

        verify(eventPublisher).publishEvent(any(OnEmailConfirmTokenNoLongerValidEvent.class));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when email confirmation token is expired")
    void shouldThrowExceptionWhenEmailConfirmationTokenIsExpired() {
        String token = "activationtoken";
        String email = "dev@test.com";
        PhegyRole notConfirmedRole = PhegyRole.builder().level(PhegyRoleLevel.NOT_CONFIRMED_USER).build();
        PhegyUser user = PhegyUser.builder()
                .email(email)
                .roles(Lists.newArrayList(notConfirmedRole))
                .build();
        EmailConfirmationToken emailConfirmationToken = EmailConfirmationToken.builder()
                .id(1L)
                .user(user)
                .originEmail(email)
                .createdAt(LocalDateTime.now())
                .expirationTime(Duration.ZERO)
                .build();

        when(emailConfirmationTokenService.getConfirmationToken(token)).thenReturn(emailConfirmationToken);

        assertThatThrownBy(() -> registerService.activateUser(token))
                .isInstanceOf(PhegyHttpException.class)
                .hasMessage("CONFIRM_TOKEN_EXPIRED");

        verify(eventPublisher).publishEvent(any(OnEmailConfirmTokenNoLongerValidEvent.class));
        verify(userRepository, never()).save(any());
    }
}