package tech.phegy.api.dto.image.response;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class ApprovalImageResponseDto extends ImageResponseDto {
    private boolean isApproved;
}
