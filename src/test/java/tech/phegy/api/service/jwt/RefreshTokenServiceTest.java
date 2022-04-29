package tech.phegy.api.service.jwt;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import tech.phegy.api.exception.PhegyHttpException;
import tech.phegy.api.model.token.RefreshToken;
import tech.phegy.api.model.token.Token;
import tech.phegy.api.model.user.PhegyUser;
import tech.phegy.api.repository.RefreshTokenRepository;
import tech.phegy.api.service.PhegyUserService;
import tech.phegy.api.service.HashingService;
import tech.phegy.api.service.jwt.event.OnTooManyRefreshTokensForUser;
import tech.phegy.api.service.validator.ModelValidatorService;

import javax.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {
    @Mock
    RefreshTokenRepository refreshTokenRepository;
    @Mock
    PhegyUserService userService;
    @Mock
    HashingService hashingService;
    @Mock
    JwtService jwtService;
    @Mock
    JwtProps jwtProps;
    @Mock
    ModelValidatorService modelValidatorService;
    @Mock
    ApplicationEventPublisher eventPublisher;

    RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(
                refreshTokenRepository,
                userService,
                hashingService,
                jwtService,
                jwtProps,
                modelValidatorService,
                eventPublisher);
    }

    @Test
    @DisplayName("Should get refresh token successfully")
    void shouldGetRefreshTokenSuccessfully() {
        String token = "refreshrandomtoken";
        String hashedToken = "hashedtoken";
        RefreshToken refreshToken = RefreshToken.builder().hashedToken(hashedToken).build();

        when(hashingService.hashString(token)).thenReturn(hashedToken);
        when(refreshTokenRepository.getByHashedToken(hashedToken)).thenReturn(Optional.of(refreshToken));

        RefreshToken actual = refreshTokenService.getRefreshToken(token);

        assertThat(actual).isNotNull();
    }

    @Test
    @DisplayName("Should throw exception when get refresh token invalid")
    void shouldThrowExceptionWhenGetRefreshTokenInvalid() {
        String token = "refreshrandomtoken";
        String hashedToken = "hashedtoken";

        when(hashingService.hashString(token)).thenReturn(hashedToken);
        when(refreshTokenRepository.getByHashedToken(hashedToken)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.getRefreshToken(token))
                .isInstanceOf(PhegyHttpException.class)
                .hasMessage("REFRESH_TOKEN_INVALID");
    }

    @Test
    @DisplayName("Should clear all old tokens successfully")
    void shouldClearAllOldTokensSuccessfully() {
        String username = "username";
        PhegyUser user = PhegyUser.builder().username(username).build();
        Collection<RefreshToken> refreshTokens = Lists.newArrayList(
                RefreshToken.builder().createdAt(LocalDateTime.now()).expirationTime(Duration.ofDays(2)).build(),
                RefreshToken.builder().createdAt(LocalDateTime.now()).expirationTime(Duration.ofDays(1)).build(),
                RefreshToken.builder().createdAt(LocalDateTime.now()).expirationTime(Duration.ZERO).build(),
                RefreshToken.builder().createdAt(LocalDateTime.now()).expirationTime(Duration.ZERO).build(),
                RefreshToken.builder().createdAt(LocalDateTime.now()).expirationTime(Duration.ZERO).build(),
                RefreshToken.builder().createdAt(LocalDateTime.now()).expirationTime(Duration.ZERO).build()
        );
        int maxRefreshTokensPerUser = 3;
        int expectedTokensCount = refreshTokens.size() - maxRefreshTokensPerUser + 1;

        when(refreshTokenRepository.getAllByUserUsername(username)).thenReturn(refreshTokens);
        when(jwtProps.getMaxRefreshTokensPerUser()).thenReturn(maxRefreshTokensPerUser);

        refreshTokenService.clearTokens(user);

        ArgumentCaptor<Collection<RefreshToken>> refreshTokensArgumentCapture = ArgumentCaptor.forClass(Collection.class);
        verify(refreshTokenRepository).deleteAll(refreshTokensArgumentCapture.capture());
        Collection<RefreshToken> capturedRefreshTokens = refreshTokensArgumentCapture.getValue();
        assertThat(capturedRefreshTokens.size()).isEqualTo(expectedTokensCount);
        assertThat(capturedRefreshTokens).allMatch(Token::isExpired);
    }

    @Test
    @DisplayName("Should skip clearing tokens when no old ones available")
    void shouldSkipClearingTokensWhenNoOldOnesAvailable() {
        String username = "username";
        PhegyUser user = PhegyUser.builder().username(username).build();
        Collection<RefreshToken> refreshTokens = Lists.newArrayList(
                RefreshToken.builder().build()
        );
        int maxRefreshTokensPerUser = 3;
        int expectedTokensToDeleteCount = 0;

        when(refreshTokenRepository.getAllByUserUsername(username)).thenReturn(refreshTokens);
        when(jwtProps.getMaxRefreshTokensPerUser()).thenReturn(maxRefreshTokensPerUser);

        refreshTokenService.clearTokens(user);

        ArgumentCaptor<Collection<RefreshToken>> refreshTokensArgumentCapture = ArgumentCaptor.forClass(Collection.class);
        verify(refreshTokenRepository).deleteAll(refreshTokensArgumentCapture.capture());
        Collection<RefreshToken> refreshTokensToDelete = refreshTokensArgumentCapture.getValue();
        assertThat(refreshTokensToDelete.size()).isEqualTo(expectedTokensToDeleteCount);
    }

    @Test
    @DisplayName("Should create new refresh token successfully")
    void shouldCreateNewRefreshTokenWithNoOldOnesSuccessfully() {
        String username = "ivan";
        PhegyUser user = PhegyUser.builder().username(username).build();
        String hashedToken = "hashedToken";

        when(userService.getUserByUsername(username)).thenReturn(user);
        when(refreshTokenRepository.countAllByUserUsername(username)).thenReturn(0);
        when(jwtProps.getMaxRefreshTokensPerUser()).thenReturn(8);
        when(jwtProps.getRefreshTokenExpirationSeconds()).thenReturn(36000L);
        when(hashingService.hashString(anyString())).thenReturn(hashedToken);

        String token = refreshTokenService.createToken(username);

        verify(modelValidatorService).validate(any(RefreshToken.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(eventPublisher, never()).publishEvent(any(OnTooManyRefreshTokensForUser.class));

        assertThat(token)
                .isNotNull()
                .isNotBlank();
    }

    @Test
    @DisplayName("Should create new refresh token with too many old ones successfully")
    void shouldCreateNewRefreshTokenWithTooManyOldOnesSuccessfully() {
        String username = "ivan";
        PhegyUser user = PhegyUser.builder().username(username).build();
        String hashedToken = "hashedToken";

        when(userService.getUserByUsername(username)).thenReturn(user);
        when(refreshTokenRepository.countAllByUserUsername(username)).thenReturn(8);
        when(jwtProps.getMaxRefreshTokensPerUser()).thenReturn(8);
        when(jwtProps.getRefreshTokenExpirationSeconds()).thenReturn(36000L);
        when(hashingService.hashString(anyString())).thenReturn(hashedToken);

        String token = refreshTokenService.createToken(username);

        verify(modelValidatorService).validate(any(RefreshToken.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(eventPublisher).publishEvent(any(OnTooManyRefreshTokensForUser.class));

        assertThat(token)
                .isNotNull()
                .isNotBlank();
    }

    @Test
    @DisplayName("Should throw exception when create refresh token invalid username")
    void shouldThrowExceptionWhenCreateRefreshTokenInvalidUsername() {
        String username = "ivan";
        when(userService.getUserByUsername(username)).thenThrow(PhegyHttpException.class);

        assertThatThrownBy(() -> refreshTokenService.createToken(username))
                .isInstanceOf(PhegyHttpException.class);
    }

    @Test
    @DisplayName("Should set refresh token header for user successfully")
    void shouldSetRefreshTokenHeaderForUserSuccessfully() {
        HttpServletResponse httpResponse = Mockito.mock(HttpServletResponse.class);
        String username = "ivan";
        PhegyUser user = PhegyUser.builder().username(username).build();
        String hashedToken = "hashedToken";
        String refreshHeader = "Refresh-Token";
        String refreshPrefix = "Bearer ";

        when(userService.getUserByUsername(username)).thenReturn(user);
        when(refreshTokenRepository.countAllByUserUsername(username)).thenReturn(0);
        when(jwtProps.getMaxRefreshTokensPerUser()).thenReturn(8);
        when(jwtProps.getRefreshTokenExpirationSeconds()).thenReturn(36000L);
        when(jwtProps.getRefreshTokenPrefix()).thenReturn(refreshPrefix);
        when(jwtProps.getRefreshTokenHeader()).thenReturn(refreshHeader);
        when(hashingService.hashString(anyString())).thenReturn(hashedToken);

        refreshTokenService.setRefreshTokenHeaderForUser(httpResponse, username);

        verify(modelValidatorService).validate(any(RefreshToken.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(eventPublisher, never()).publishEvent(any(OnTooManyRefreshTokensForUser.class));

        verify(httpResponse).addHeader("Access-Control-Expose-Headers", refreshHeader);

        ArgumentCaptor<String> refreshTokenArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(httpResponse).setHeader(anyString(), refreshTokenArgumentCaptor.capture());
        String authToken = refreshTokenArgumentCaptor.getValue();

        assertThat(authToken)
                .isNotNull()
                .contains(refreshPrefix);
    }

    @Test
    @DisplayName("Should set new jwt token successfully")
    void shouldSetNewJwtTokenSuccessfully() {
        HttpServletResponse httpResponse = Mockito.mock(HttpServletResponse.class);
        String refreshHeader = "Refresh-Token";
        String refreshPrefix = "Bearer";
        String token = "refreshToken";
        String hashedToken = "hashedToken";
        String username = "username";
        PhegyUser user = PhegyUser.builder().username(username).build();
        RefreshToken refreshToken = RefreshToken.builder()
                .hashedToken(hashedToken)
                .createdAt(LocalDateTime.now())
                .expirationTime(Duration.ofDays(2))
                .user(user)
                .build();

        when(jwtProps.getRefreshTokenPrefix()).thenReturn(refreshPrefix);
        when(jwtProps.getRefreshTokenHeader()).thenReturn(refreshHeader);
        when(hashingService.hashString(token)).thenReturn(hashedToken);
        when(refreshTokenRepository.getByHashedToken(hashedToken)).thenReturn(Optional.of(refreshToken));

        refreshTokenService.refreshAccess(httpResponse, token);

        verify(jwtService).setAuthorizationResponseHeaderForUser(httpResponse, username);

        verify(httpResponse).addHeader("Access-Control-Expose-Headers", refreshHeader);

        ArgumentCaptor<String> refreshTokenArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(httpResponse).setHeader(anyString(), refreshTokenArgumentCaptor.capture());
        String authToken = refreshTokenArgumentCaptor.getValue();

        assertThat(authToken)
                .isNotNull()
                .contains(refreshPrefix)
                .contains(token);
    }

    @Test
    @DisplayName("Should set new jwt token and new refresh token when old is halfway expired")
    void shouldSetNewJwtTokenAndNewRefreshTokenWhenOldIsHalfwayExpired() {
        HttpServletResponse httpResponse = Mockito.mock(HttpServletResponse.class);
        String refreshHeader = "Refresh-Token";
        String refreshPrefix = "Bearer";
        String token = "refreshToken";
        String hashedToken = "hashedToken";
        String username = "username";
        PhegyUser user = PhegyUser.builder().username(username).build();
        RefreshToken refreshToken = RefreshToken.builder()
                .hashedToken(hashedToken)
                .createdAt(LocalDateTime.now().minusDays(2))
                .expirationTime(Duration.ofDays(3))
                .user(user)
                .build();

        when(jwtProps.getRefreshTokenPrefix()).thenReturn(refreshPrefix);
        when(jwtProps.getRefreshTokenHeader()).thenReturn(refreshHeader);
        when(hashingService.hashString(token)).thenReturn(hashedToken);
        when(refreshTokenRepository.getByHashedToken(hashedToken)).thenReturn(Optional.of(refreshToken));

        refreshTokenService.refreshAccess(httpResponse, token);

        verify(jwtService).setAuthorizationResponseHeaderForUser(httpResponse, username);

        verify(httpResponse).addHeader("Access-Control-Expose-Headers", refreshHeader);

        ArgumentCaptor<String> refreshTokenArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(httpResponse).setHeader(anyString(), refreshTokenArgumentCaptor.capture());
        String authToken = refreshTokenArgumentCaptor.getValue();

        assertThat(authToken)
                .isNotNull()
                .contains(refreshPrefix)
                .doesNotContain(token);
    }

    @Test
    @DisplayName("Should throw exception when refresh access token is expired")
    void shouldThrowExceptionWhenRefreshAccessTokenIsExpired() {
        HttpServletResponse httpResponse = Mockito.mock(HttpServletResponse.class);
        String token = "refreshToken";
        String hashedToken = "hashedToken";
        RefreshToken refreshToken = RefreshToken.builder()
                .hashedToken(hashedToken)
                .createdAt(LocalDateTime.now())
                .expirationTime(Duration.ZERO)
                .build();

        when(hashingService.hashString(token)).thenReturn(hashedToken);
        when(refreshTokenRepository.getByHashedToken(hashedToken)).thenReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> refreshTokenService.refreshAccess(httpResponse, token))
                .isInstanceOf(PhegyHttpException.class)
                .hasMessage("REFRESH_TOKEN_INVALID");
    }
}