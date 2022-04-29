package tech.phegy.api.service.imageGenerator;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Image generator configuration properties.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ConfigurationProperties("application.image.generator")
public class ImageGeneratorProps {
    /**
     * Image generator api domain.
     */
    private String domain;

    /**
     * Profile image avatar type.
     */
    private String avatarType;

    /**
     * Profile image size in pixels.
     */
    private float sizePx;

    /**
     * Profile image color palette.
     */
    private String colorPalette;
}
