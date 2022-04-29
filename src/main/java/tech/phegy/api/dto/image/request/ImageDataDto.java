package tech.phegy.api.dto.image.request;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ImageDataDto {
    private String title;
    private String description;
}
