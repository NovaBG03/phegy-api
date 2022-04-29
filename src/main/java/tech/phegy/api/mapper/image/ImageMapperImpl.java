package tech.phegy.api.mapper.image;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import tech.phegy.api.dto.image.request.ImageDataDto;
import tech.phegy.api.dto.image.response.ApprovalImageResponseDto;
import tech.phegy.api.dto.image.response.ImagePageResponseDto;
import tech.phegy.api.dto.image.response.ImageResponseDto;
import tech.phegy.api.model.points.Vote;
import tech.phegy.api.model.Image;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Image mapper.
 *
 * @author Nikita
 */
@Component
public class ImageMapperImpl implements ImageMapper {
    @Override
    public Image imageDataDtoToImage(ImageDataDto imageDto) {
        if (imageDto == null) {
            return null;
        }

        return Image.builder()
                .title(imageDto.getTitle().trim())
                .description(imageDto.getDescription().trim())
                .build();
    }

    @Override
    public ImageResponseDto imageToImageResponseDto(Image image) {
        if (image == null) {
            return null;
        }

        return ImageResponseDto.builder()
                .id(image.getId())
                .title(image.getTitle())
                .description(image.getDescription())
                .imageKey(image.getImageKey())
                .publisherUsername(image.getPublisher() != null ? image.getPublisher().getUsername() : null)
                .publishedOn(image.isApproved() ? image.getApprovedOn() : image.getPublishedOn())
                .points(image.getVotes().stream().mapToDouble(Vote::getPoints).sum())
                .build();
    }

    @Override
    public ApprovalImageResponseDto imageToApprovalImageResponseDto(Image image) {
        if (image == null) {
            return null;
        }

        return ApprovalImageResponseDto.builder()
                .id(image.getId())
                .title(image.getTitle())
                .description(image.getDescription())
                .imageKey(image.getImageKey())
                .publisherUsername(image.getPublisher() != null ? image.getPublisher().getUsername() : null)
                .publishedOn(image.isApproved() ? image.getApprovedOn() : image.getPublishedOn())
                .isApproved(image.isApproved())
                .points(image.getVotes().stream().mapToDouble(Vote::getPoints).sum())
                .build();
    }

    @Override
    public ImagePageResponseDto createImagePageResponseDto(Page<Image> imagePage, boolean isPublisherOrAdmin) {
        if (imagePage == null) {
            return null;
        }

        List<ImageResponseDto> imageResponseDtos;
        if (isPublisherOrAdmin) {
            imageResponseDtos = StreamSupport.stream(imagePage.spliterator(), false)
                    .map(this::imageToApprovalImageResponseDto).
                    collect(Collectors.toList());
        } else {
            imageResponseDtos = StreamSupport.stream(imagePage.spliterator(), false)
                    .map(this::imageToImageResponseDto).
                    collect(Collectors.toList());
        }

        return ImagePageResponseDto.builder()
                .images(imageResponseDtos)
                .totalCount(imagePage.getTotalElements())
                .build();
    }
}
