package tech.phegy.api.mapper.poins;

import tech.phegy.api.dto.poins.response.PointsBagResponseDto;
import tech.phegy.api.model.points.PointsBag;

public interface PoinsMapper {
    PointsBagResponseDto pointsBagToPointsBagResponseDto(PointsBag pointsBag);
}
