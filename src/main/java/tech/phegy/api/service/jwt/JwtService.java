package tech.phegy.api.service.jwt;

import com.google.common.base.Strings;
import io.jsonwebtoken.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import tech.phegy.api.exception.PhegyHttpException;

import javax.crypto.SecretKey;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing jwt tokens.
 *
 * @author Nikita
 */
@Service
public class JwtService {
    private final UserDetailsService userDetailsService;
    private final SecretKey secretKey;
    private final JwtProps jwtProps;

    /**
     * Constructs new instance with needed dependencies.
     */
    public JwtService(UserDetailsService userDetailsService, SecretKey secretKey, JwtProps jwtConfig) {
        this.userDetailsService = userDetailsService;
        this.secretKey = secretKey;
        this.jwtProps = jwtConfig;
    }

    /**
     * Get authentication object from http serverlet request.
     *
     * @param request user's request.
     * @return authentication.
     * @throws PhegyHttpException when jwt is expired.
     */
    public Authentication getAuthentication(HttpServletRequest request) throws PhegyHttpException {
        String authorizationToken = request.getHeader(jwtProps.getAuthTokenHeader());
        return this.getAuthentication(authorizationToken);
    }

    /**
     * Get authentication object from authorization token string.
     *
     * @param authorizationToken authorization prefix + token.
     * @return authentication.
     * @throws PhegyHttpException when jwt is expired.
     */
    public Authentication getAuthentication(String authorizationToken) throws PhegyHttpException {
        if (Strings.isNullOrEmpty(authorizationToken) || !authorizationToken.startsWith(jwtProps.getAuthTokenPrefix())) {
            return null;
        }

        String token = authorizationToken.replace(jwtProps.getAuthTokenPrefix(), "").strip();
        if (Strings.isNullOrEmpty(token)) {
            return null;
        }

        Jws<Claims> claimsJws;
        try {
            claimsJws = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
        } catch (ExpiredJwtException e) {
            throw new PhegyHttpException("JWT_EXPIRED", HttpStatus.UNAUTHORIZED);
        } catch (JwtException e) {
            throw new PhegyHttpException("JWT_INVALID", HttpStatus.UNAUTHORIZED);
        }

        Claims body = claimsJws.getBody();

        String username = body.getSubject();

        var authorities = (List<Map<String, String>>) body.get("authorities");

        Set<SimpleGrantedAuthority> simpleGrantedAuthorities = authorities.stream()
                .map(authorityMap -> new SimpleGrantedAuthority(authorityMap.get("authority")))
                .collect(Collectors.toSet());

        return new UsernamePasswordAuthenticationToken(
                username,
                null,
                simpleGrantedAuthorities
        );
    }

    /**
     * Create new jwt token for a specific user.
     *
     * @param username user to create token for.
     * @return jwt string.
     * @throws UsernameNotFoundException when user does not exist.
     */
    public String createJwt(String username) throws UsernameNotFoundException {
        final UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .claim("authorities", userDetails.getAuthorities())
                .setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plusSeconds(jwtProps.getAuthTokenExpirationSeconds())))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Set http response authorization header for a specific user.
     *
     * @param response http servlet response.
     * @param username principal username.
     */
    public void setAuthorizationResponseHeaderForUser(HttpServletResponse response, String username) {
        final String jwt = this.createJwt(username);
        this.setAuthorizationResponseHeader(response, jwt);
    }

    private void setAuthorizationResponseHeader(HttpServletResponse response, String jwt) {
        response.addHeader("Access-Control-Expose-Headers", jwtProps.getAuthTokenHeader());
        response.setHeader(jwtProps.getAuthTokenHeader(), jwtProps.getAuthTokenPrefix() + " " + jwt);
    }
}
