package tech.phegy.api.dto.image.response;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class ImageResponseDto {
    private Long id;
    private String title;
    private String description;
    private String imageKey;
    private String publisherUsername;
    private LocalDateTime publishedOn;
    private Double points;
}
