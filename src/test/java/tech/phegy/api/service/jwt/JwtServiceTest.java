package tech.phegy.api.service.jwt;

import io.jsonwebtoken.security.Keys;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import tech.phegy.api.exception.PhegyHttpException;
import tech.phegy.api.model.user.PhegyRole;
import tech.phegy.api.model.user.PhegyRoleLevel;
import tech.phegy.api.model.user.PhegyUser;
import tech.phegy.api.security.PhegyUserDetails;

import javax.crypto.SecretKey;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {
    @Mock
    UserDetailsService userDetailsService;
    @Mock
    JwtProps jwtProps;

    String secretKeyString = "supersecrettestkeydontworryitsjustatestxd";
    SecretKey secretKey = Keys.hmacShaKeyFor(secretKeyString.getBytes());

    JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(userDetailsService, secretKey, jwtProps);
    }

    @Test
    @DisplayName("Should get authentication from http request successfully")
    void shouldGetAuthenticationFromHttpRequestSuccessfully() {
        HttpServletRequest httpRequest = Mockito.mock(HttpServletRequest.class);
        String authPrefix = "Bearer";
        String authHeader = "Authorization";
        String username = "ivan";
        PhegyUser user = PhegyUser.builder()
                .username(username)
                .roles(Lists.newArrayList(PhegyRole.builder().level(PhegyRoleLevel.USER).build()))
                .build();

        when(jwtProps.getAuthTokenHeader()).thenReturn(authHeader);
        when(jwtProps.getAuthTokenPrefix()).thenReturn(authPrefix);

        when(userDetailsService.loadUserByUsername(username)).thenReturn(new PhegyUserDetails(user));
        when(jwtProps.getAuthTokenExpirationSeconds()).thenReturn(36000L);
        String jwt = jwtService.createJwt(username);
        when(httpRequest.getHeader(authHeader)).thenReturn(authPrefix + jwt);

        Authentication authentication = jwtService.getAuthentication(httpRequest);

        assertThat(authentication)
                .isNotNull()
                .matches(x -> x.getPrincipal().equals(username))
                .matches(x -> (long) x.getAuthorities().size() == 1);
    }

    @Test
    @DisplayName("Should get authentication from token string successfully")
    void shouldGetAuthenticationFromTokenStringSuccessfully() {
        String authPrefix = "Bearer";
        String username = "ivan";
        PhegyUser user = PhegyUser.builder()
                .username(username)
                .roles(Lists.newArrayList(PhegyRole.builder().level(PhegyRoleLevel.USER).build()))
                .build();

        when(userDetailsService.loadUserByUsername(username)).thenReturn(new PhegyUserDetails(user));
        when(jwtProps.getAuthTokenExpirationSeconds()).thenReturn(36000L);
        String jwt = jwtService.createJwt(username);

        when(jwtProps.getAuthTokenPrefix()).thenReturn(authPrefix);

        Authentication authentication = jwtService.getAuthentication(authPrefix + jwt);

        assertThat(authentication)
                .isNotNull()
                .matches(x -> x.getPrincipal().equals(username))
                .matches(x -> x.getAuthorities().size() == 1);
    }

    @Test
    @DisplayName("Should not get authentication when token is null")
    void shouldNotGetAuthenticationWhenTokenIsNull() {
        String authorizationToken = null;
        Authentication authentication = jwtService.getAuthentication(authorizationToken);
        assertThat(authentication).isNull();
    }

    @Test
    @DisplayName("Should not get authentication when auth token header is missing")
    void shouldNotGetAuthenticationWhenAuthTokenHeaderIsMissing() {
        String authPrefix = "Bearer";
        String authorizationToken = "defnottoken";

        when(jwtProps.getAuthTokenPrefix()).thenReturn(authPrefix);
        Authentication authentication = jwtService.getAuthentication(authorizationToken);
        assertThat(authentication).isNull();
    }

    @Test
    @DisplayName("Should not get authentication when token is missing")
    void shouldNotGetAuthenticationWhenTokenIsMissing() {
        String authPrefix = "Bearer";

        when(jwtProps.getAuthTokenPrefix()).thenReturn(authPrefix);

        Authentication authentication = jwtService.getAuthentication(authPrefix);

        assertThat(authentication).isNull();
    }

    @Test
    @DisplayName("Should throw exception when jwt has expired")
    void shouldThrowExceptionWhenJwtHasExpired() {
        String authPrefix = "Bearer";
        String username = "ivan";
        PhegyUser user = PhegyUser.builder()
                .username(username)
                .roles(Lists.newArrayList(PhegyRole.builder().level(PhegyRoleLevel.USER).build()))
                .build();

        when(userDetailsService.loadUserByUsername(username)).thenReturn(new PhegyUserDetails(user));
        when(jwtProps.getAuthTokenExpirationSeconds()).thenReturn(0L);
        String jwt = jwtService.createJwt(username);

        when(jwtProps.getAuthTokenPrefix()).thenReturn(authPrefix);

        assertThatThrownBy(() -> jwtService.getAuthentication(authPrefix + jwt))
                .isInstanceOf(PhegyHttpException.class)
                .hasMessage("JWT_EXPIRED");
    }

    @Test
    @DisplayName("Should throw exception when jwt invalid")
    void shouldThrowExceptionWhenJwtInvalid() {
        String authPrefix = "Bearer";
        String jwt = "invalidtoken";

        when(jwtProps.getAuthTokenPrefix()).thenReturn(authPrefix);

        assertThatThrownBy(() -> jwtService.getAuthentication(authPrefix + jwt))
                .isInstanceOf(PhegyHttpException.class)
                .hasMessage("JWT_INVALID");
    }

    @Test
    @DisplayName("Should create jwt successfully")
    void shouldCreateJwtSuccessfully() {
        String username = "ivan";
        PhegyUser user = PhegyUser.builder()
                .username(username)
                .roles(Lists.newArrayList(PhegyRole.builder().level(PhegyRoleLevel.USER).build()))
                .build();

        when(userDetailsService.loadUserByUsername(username)).thenReturn(new PhegyUserDetails(user));
        when(jwtProps.getAuthTokenExpirationSeconds()).thenReturn(36000L);

        String jwt = jwtService.createJwt(username);
        assertThat(jwt)
                .isNotNull()
                .isNotEmpty();
    }

    @Test
    @DisplayName("Should set authorization response header for user successfully")
    void shouldSetAuthorizationResponseHeaderForUserSuccessfully() {
        HttpServletResponse httpResponse = Mockito.mock(HttpServletResponse.class);
        String authHeader = "Authorization";
        String authPrefix = "Bearer";
        String username = "ivan";
        PhegyUser user = PhegyUser.builder()
                .username(username)
                .roles(Lists.newArrayList(PhegyRole.builder().level(PhegyRoleLevel.USER).build()))
                .build();

        when(userDetailsService.loadUserByUsername(username)).thenReturn(new PhegyUserDetails(user));
        when(jwtProps.getAuthTokenExpirationSeconds()).thenReturn(36000L);
        when(jwtProps.getAuthTokenHeader()).thenReturn(authHeader);
        when(jwtProps.getAuthTokenPrefix()).thenReturn(authPrefix);

        jwtService.setAuthorizationResponseHeaderForUser(httpResponse, username);

        verify(httpResponse).addHeader("Access-Control-Expose-Headers", authHeader);

        ArgumentCaptor<String> authTokenArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(httpResponse).setHeader(anyString(), authTokenArgumentCaptor.capture());
        String authToken = authTokenArgumentCaptor.getValue();

        assertThat(authToken)
                .isNotNull()
                .contains(authPrefix);
    }
}