package tech.phegy.api.dto.poins.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointsBagResponseDto {
    private Double points;
    private String username;
}
