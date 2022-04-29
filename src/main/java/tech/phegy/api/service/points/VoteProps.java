package tech.phegy.api.service.points;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ConfigurationProperties("application.vote")
public class VoteProps {
    /**
     * Maximum vote points
     */
    private Double maxPoints;

    /**
     * Minimum vote points
     */
    private Double minPoints;
}
