package tech.phegy.api.service.jwt;

import com.google.common.net.HttpHeaders;
import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT configuration properties.
 * @author Nikita
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ConfigurationProperties("application.jwt")
public class JwtProps {
    /**
     * Secret key, used to sign and validate JWT authorization tokens.
     */
    private String secretKey;

    /**
     * JWT authorization token prefix.
     */
    private String authTokenPrefix;

    /**
     * JWT authorization token expiration in seconds.
     */
    private Long authTokenExpirationSeconds;

    /**
     * JWT authorization header.
     */
    private String authTokenHeader = HttpHeaders.AUTHORIZATION;

    /**
     * Refresh token prefix.
     */
    private String refreshTokenPrefix;

    /**
     * Refresh token expiration in seconds.
     */
    private Long refreshTokenExpirationSeconds;

    /**
     * Refresh token header.
     */
    private String refreshTokenHeader;

    /**
     * Maximum allowed refresh tokens per user.
     */
    private Integer maxRefreshTokensPerUser;
}
