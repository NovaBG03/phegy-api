package tech.phegy.api.mapper.poins;

import org.springframework.stereotype.Component;
import tech.phegy.api.dto.poins.response.PointsBagResponseDto;
import tech.phegy.api.model.points.PointsBag;

@Component
public class PoinsMapperImpl implements PoinsMapper {

    @Override
    public PointsBagResponseDto pointsBagToPointsBagResponseDto(PointsBag pointsBag) {
        if (pointsBag == null) {
            return null;
        }

        return PointsBagResponseDto.builder()
                .points(pointsBag.getPoints())
                .username(pointsBag.getUser().getUsername())
                .build();
    }
}
