package tech.phegy.api.dto.image.response;

import lombok.*;

import java.util.ArrayList;
import java.util.Collection;

@Getter
@Setter
@Builder
public class ImagePageResponseDto {
    @Builder.Default
    private Collection<ImageResponseDto> images = new ArrayList<>();
    private long totalCount;
}
