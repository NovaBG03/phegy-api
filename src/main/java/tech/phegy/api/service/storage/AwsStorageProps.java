package tech.phegy.api.service.storage;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AWS configuration properties.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ConfigurationProperties("application.aws")
public class AwsStorageProps {
    /**
     * AWS access key.
     */
    private String accessKey;

    /**
     * AWS secret key.
     */
    private String secretKey;

    /**
     * AWS region.
     */
    private String region;

    /**
     * AWS S3 bucket name.
     */
    private String bucketName;
}
