package tech.phegy.api.mapper.image;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import tech.phegy.api.dto.image.request.ImageDataDto;
import tech.phegy.api.dto.image.response.ApprovalImageResponseDto;
import tech.phegy.api.dto.image.response.ImagePageResponseDto;
import tech.phegy.api.dto.image.response.ImageResponseDto;
import tech.phegy.api.model.points.Vote;
import tech.phegy.api.model.Image;
import tech.phegy.api.model.user.PhegyUser;

import java.time.LocalDateTime;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

class ImageMapperImplTest {
    ImageMapperImpl imageMappr;

    @BeforeEach
    void setUp() {
        imageMappr = new ImageMapperImpl();
    }

    @Test
    @DisplayName("Should map image data dto to image")
    void shouldMapImageDataDtoToImage() {
        String title = "title";
        String description = "description";
        ImageDataDto imageDataDto = ImageDataDto.builder()
                .title(title)
                .description(description)
                .build();

        Image actual = imageMappr.imageDataDtoToImage(imageDataDto);

        assertThat(actual)
                .matches(x -> x.getTitle().equals(title), "title is set")
                .matches(x -> x.getDescription().equals(description), "description is set");
    }

    @Test
    @DisplayName("Should map image data dto to image when null")
    void shouldMapImageDataDtoToImageWhenNull() {
        Image actual = imageMappr.imageDataDtoToImage(null);

        assertThat(actual).isNull();
    }

    @Test
    @DisplayName("Should map not approved image to image response dto")
    void shouldMapNotApprovedImageToImageResponseDto() {
        long id = 1L;
        String title = "title";
        String description = "description";
        String imageKey = "imagekey";
        String publisherUsername = "username";
        PhegyUser publisher = PhegyUser.builder().username(publisherUsername).build();
        LocalDateTime publishedOn = LocalDateTime.now().minusHours(10);
        Collection<Vote> votes = Lists.newArrayList(
                Vote.builder().points(10d).build(),
                Vote.builder().points(10d).build()
        );
        Double expectedPoints = 20d;

        Image image = Image.builder()
                .id(id)
                .title(title)
                .description(description)
                .imageKey(imageKey)
                .publisher(publisher)
                .publishedOn(publishedOn)
                .votes(votes)
                .build();

        ImageResponseDto actual = imageMappr.imageToImageResponseDto(image);

        assertThat(actual)
                .matches(x -> x.getId().equals(id), "id is set")
                .matches(x -> x.getTitle().equals(title), "title is set")
                .matches(x -> x.getDescription().equals(description), "description is set")
                .matches(x -> x.getImageKey().equals(imageKey), "image key is set")
                .matches(x -> x.getPublisherUsername().equals(publisherUsername), "publisher username is set")
                .matches(x -> x.getPublishedOn().equals(publishedOn), "published on is set to publish date")
                .matches(x -> x.getPoints().equals(expectedPoints), "points is set");
    }

    @Test
    @DisplayName("Should map approved image to image response dto")
    void shouldMapApprovedImageToImageResponseDto() {
        long id = 1L;
        String title = "title";
        String description = "description";
        String imageKey = "imagekey";
        String publisherUsername = "username";
        PhegyUser publisher = PhegyUser.builder().username(publisherUsername).build();
        LocalDateTime publishedOn = LocalDateTime.now().minusHours(10);
        LocalDateTime approvedOn = LocalDateTime.now().minusHours(1);
        Collection<Vote> votes = Lists.newArrayList(
                Vote.builder().points(10d).build(),
                Vote.builder().points(10d).build()
        );
        Double expectedPoints = 20d;

        Image image = Image.builder()
                .id(id)
                .title(title)
                .description(description)
                .imageKey(imageKey)
                .publisher(publisher)
                .publishedOn(publishedOn)
                .approvedOn(approvedOn)
                .votes(votes)
                .build();

        ImageResponseDto actual = imageMappr.imageToImageResponseDto(image);

        assertThat(actual)
                .matches(x -> x.getId().equals(id), "id is set")
                .matches(x -> x.getTitle().equals(title), "title is set")
                .matches(x -> x.getDescription().equals(description), "description is set")
                .matches(x -> x.getImageKey().equals(imageKey), "image key is set")
                .matches(x -> x.getPublisherUsername().equals(publisherUsername), "publisher username is set")
                .matches(x -> x.getPublishedOn().equals(approvedOn), "published on is set to approved date")
                .matches(x -> x.getPoints().equals(expectedPoints), "points is set");
    }

    @Test
    @DisplayName("Should map image to image response dto when null")
    void shouldMapImageToImageResponseDtoWhenNull() {
        ImageResponseDto actual = imageMappr.imageToImageResponseDto(null);
        assertThat(actual).isNull();
    }

    @Test
    @DisplayName("Should map not approved image to approval image response dto")
    void shouldMapNotApprovedImageToApprovalImageResponseDto() {
        long id = 1L;
        String title = "title";
        String description = "description";
        String imageKey = "imagekey";
        String publisherUsername = "username";
        PhegyUser publisher = PhegyUser.builder().username(publisherUsername).build();
        LocalDateTime publishedOn = LocalDateTime.now().minusHours(10);
        Collection<Vote> votes = Lists.newArrayList(
                Vote.builder().points(10d).build(),
                Vote.builder().points(10d).build()
        );
        Double expectedPoints = 20d;

        Image image = Image.builder()
                .id(id)
                .title(title)
                .description(description)
                .imageKey(imageKey)
                .publisher(publisher)
                .publishedOn(publishedOn)
                .votes(votes)
                .build();

        ApprovalImageResponseDto actual = imageMappr.imageToApprovalImageResponseDto(image);

        assertThat(actual)
                .matches(x -> x.getId().equals(id), "id is set")
                .matches(x -> x.getTitle().equals(title), "title is set")
                .matches(x -> x.getDescription().equals(description), "description is set")
                .matches(x -> x.getImageKey().equals(imageKey), "image key is set")
                .matches(x -> x.getPublisherUsername().equals(publisherUsername), "publisher username is set")
                .matches(x -> x.getPublishedOn().equals(publishedOn), "published on is set to publish date")
                .matches(x -> x.getPoints().equals(expectedPoints), "points is set")
                .matches(x -> !x.isApproved(), "is not approved");
    }

    @Test
    @DisplayName("Should map approved image to approval image response dto")
    void shouldMapApprovedImageToApprovalImageResponseDto() {
        long id = 1L;
        String title = "title";
        String description = "description";
        String imageKey = "imagekey";
        String publisherUsername = "username";
        PhegyUser publisher = PhegyUser.builder().username(publisherUsername).build();
        LocalDateTime publishedOn = LocalDateTime.now().minusHours(10);
        LocalDateTime approvedOn = LocalDateTime.now().minusHours(1);
        Collection<Vote> votes = Lists.newArrayList(
                Vote.builder().points(10d).build(),
                Vote.builder().points(10d).build()
        );
        Double expectedPoints = 20d;

        Image image = Image.builder()
                .id(id)
                .title(title)
                .description(description)
                .imageKey(imageKey)
                .publisher(publisher)
                .publishedOn(publishedOn)
                .approvedOn(approvedOn)
                .votes(votes)
                .build();

        ApprovalImageResponseDto actual = imageMappr.imageToApprovalImageResponseDto(image);

        assertThat(actual)
                .matches(x -> x.getId().equals(id), "id is set")
                .matches(x -> x.getTitle().equals(title), "title is set")
                .matches(x -> x.getDescription().equals(description), "description is set")
                .matches(x -> x.getImageKey().equals(imageKey), "image key is set")
                .matches(x -> x.getPublisherUsername().equals(publisherUsername), "publisher username is set")
                .matches(x -> x.getPublishedOn().equals(approvedOn), "published on is set to approved date")
                .matches(x -> x.getPoints().equals(expectedPoints), "points is set")
                .matches(ApprovalImageResponseDto::isApproved, "is approved");
    }

    @Test
    @DisplayName("Should map image to approval image response dto when null")
    void shouldMapImageToApprovalImageResponseDtoWhenNull() {
        ApprovalImageResponseDto actual = imageMappr.imageToApprovalImageResponseDto(null);
        assertThat(actual).isNull();
    }

    @Test
    @DisplayName("Should create image page response dto when is publisher or admin")
    void shouldCreateImagePageResponseDtoWhenIsPublisherOrAdmin() {
        boolean isAdminOrModerator = true;
        Page<Image> imagePage = new PageImpl<>(Lists.newArrayList(
                Image.builder().build(),
                Image.builder().build(),
                Image.builder().build()
        ));

        ImagePageResponseDto imagePageResponseDto = imageMappr.createImagePageResponseDto(imagePage, isAdminOrModerator);

        assertThat(imagePageResponseDto)
                .matches(x -> x.getTotalCount() == 3, "is correct image count");
    }

    @Test
    @DisplayName("Should create image page response dto when is not publisher or admin")
    void shouldCreateImagePageResponseDtoWhenIsNotPublisherOrAdmin() {
        boolean isAdminOrModerator = false;
        Page<Image> imagePage = new PageImpl<>(Lists.newArrayList(
                Image.builder().build(),
                Image.builder().build(),
                Image.builder().build()
        ));

        ImagePageResponseDto imagePageResponseDto = imageMappr.createImagePageResponseDto(imagePage, isAdminOrModerator);

        assertThat(imagePageResponseDto)
                .matches(x -> x.getTotalCount() == 3, "is correct image count");
    }

    @Test
    @DisplayName("Should create image page response dto when null")
    void shouldCreateImagePageResponseDtoWhenNull() {
        ImagePageResponseDto actual = imageMappr.createImagePageResponseDto(null, true);
        assertThat(actual).isNull();
    }
}
