package tech.phegy.api.mapper.image;

import org.springframework.data.domain.Page;
import tech.phegy.api.model.Image;
import tech.phegy.api.dto.image.request.ImageDataDto;
import tech.phegy.api.dto.image.response.ApprovalImageResponseDto;
import tech.phegy.api.dto.image.response.ImagePageResponseDto;
import tech.phegy.api.dto.image.response.ImageResponseDto;

public interface ImageMapper {
    Image imageDataDtoToImage(ImageDataDto imageDto);

    ImageResponseDto imageToImageResponseDto(Image image);

    ApprovalImageResponseDto imageToApprovalImageResponseDto(Image image);

    ImagePageResponseDto createImagePageResponseDto(Page<Image> imagePage, boolean isPublisherOrAdmin);
}
