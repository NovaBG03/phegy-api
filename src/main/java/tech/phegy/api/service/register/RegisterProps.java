package tech.phegy.api.service.register;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Registration configuration properties.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ConfigurationProperties("application.register")
public class RegisterProps {
    /**
     * Email confirmation token activation url.
     */
    private String tokenActivationUrl;

    /**
     * Email confirmation token expiration in days.
     */
    private Integer tokenExpirationDays;

    /**
     * Minimum delay between sending new email confirmation token.
     */
    private Long tokenMinimalDelaySeconds;
}
