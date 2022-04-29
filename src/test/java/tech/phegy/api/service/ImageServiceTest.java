package tech.phegy.api.service;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.multipart.MultipartFile;
import tech.phegy.api.dto.image.response.ImagePageResponseDto;
import tech.phegy.api.dto.image.filter.ImageOrderFilter;
import tech.phegy.api.dto.image.filter.ImagePublishFilter;
import tech.phegy.api.exception.PhegyHttpException;
import tech.phegy.api.mapper.image.ImageMapper;
import tech.phegy.api.model.Image;
import tech.phegy.api.model.user.PhegyRole;
import tech.phegy.api.model.user.PhegyRoleLevel;
import tech.phegy.api.model.user.PhegyUser;
import tech.phegy.api.repository.ImageRepository;
import tech.phegy.api.service.storage.CloudStorageService;
import tech.phegy.api.service.storage.StoragePath;
import tech.phegy.api.service.validator.ModelValidatorService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {
    @Mock
    ImageRepository imageRepository;
    @Mock
    PhegyUserService userService;
    @Mock
    CloudStorageService cloudStorageService;
    @Mock
    NotificationService notificationService;
    @Mock
    ModelValidatorService modelValidatorService;
    @Mock
    ImageMapper imageMapper;

    ImageService imageService;

    @BeforeEach
    void setUp() {
        imageService = new ImageService(
                imageRepository,
                userService,
                cloudStorageService,
                notificationService,
                modelValidatorService,
                imageMapper
        );
    }

    @Test
    @DisplayName("Should get images count correctly")
    void shouldGetImagesCountCorrectly() {
        String username = "username";
        Long count = 10L;

        when(imageRepository.countByPublisherUsernameAndApprovedOnNotNull(username)).thenReturn(count);

        Long actual = imageService.getImagesCount(username);
        assertThat(actual).isEqualTo(count);
    }

    @Test
    @DisplayName("Should get approved images correctly")
    void shouldGetApprovedImagesCorrectly() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        ImagePublishFilter publishFilter = ImagePublishFilter.APPROVED;
        ImageOrderFilter orderFilter = ImageOrderFilter.NEWEST;
        String publisherUsername = null;
        String principalUsername = null;

        Page<Image> imagePage = new PageImpl<>(Lists.newArrayList());
        ImagePageResponseDto imagePageResponseDto = ImagePageResponseDto.builder().build();

        when(userService.getUserByUsername(principalUsername)).thenThrow(UsernameNotFoundException.class);
        when(imageRepository.findAllByApprovedOnNotNull(any())).thenReturn(imagePage);
        when(imageMapper.createImagePageResponseDto(imagePage, false)).thenReturn(imagePageResponseDto);

        ImagePageResponseDto actual = imageService.getImages(pageRequest, publishFilter, orderFilter, publisherUsername, principalUsername);

        assertThat(actual).isEqualTo(imagePageResponseDto);
    }

    @Test
    @DisplayName("Should get approved image without principal correctly")
    void shouldGetApprovedImageWithoutPrincipalCorrectly() {
        Long imageId = 1L;
        Image image = Image.builder()
                .id(imageId)
                .build();

        when(imageRepository.findByIdAndApprovedOnNotNull(imageId)).thenReturn(Optional.of(image));

        Image actual = imageService.getImage(imageId, null);
        assertThat(actual).isEqualTo(image);
    }

    @Test
    @DisplayName("Should throw exception when image not found without principal")
    void shouldThrowExceptionWhenImageNotFoundWithoutPrincipalCorrectly() {
        Long imageId = 1L;

        when(imageRepository.findByIdAndApprovedOnNotNull(imageId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> imageService.getImage(imageId, null))
                .isInstanceOf(PhegyHttpException.class)
                .hasMessage("IMAGE_ID_INVALID");
    }

    @Test
    @DisplayName("Should throw exception when image not found with principal")
    void shouldThrowExceptionWhenImageNotFoundWithPrincipal() {
        Long imageId = 1L;
        String username = "principal";

        when(imageRepository.findById(imageId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> imageService.getImage(imageId, username))
                .isInstanceOf(PhegyHttpException.class)
                .hasMessage("IMAGE_ID_INVALID");
    }

    @Test
    @DisplayName("Should get approved image with user correctly")
    void shouldGetApprovedImageWithUserCorrectly() {
        PhegyRole role = PhegyRole.builder().level(PhegyRoleLevel.USER).build();
        String username = "principal";
        PhegyUser user = PhegyUser.builder()
                .username(username)
                .roles(Lists.newArrayList(role))
                .build();
        Long imageId = 1L;
        Image image = Image.builder()
                .id(imageId)
                .approvedOn(LocalDateTime.now())
                .build();

        when(imageRepository.findById(imageId)).thenReturn(Optional.of(image));
        when(userService.getUserByUsername(username)).thenReturn(user);

        Image actual = imageService.getImage(imageId, username);
        assertThat(actual).isEqualTo(image);
    }

    @Test
    @DisplayName("Should get image with publisher correctly")
    void shouldGetImageWithPublisherCorrectly() {
        PhegyRole role = PhegyRole.builder().level(PhegyRoleLevel.USER).build();
        String username = "principal";
        PhegyUser user = PhegyUser.builder()
                .username(username)
                .roles(Lists.newArrayList(role))
                .build();
        Long imageId = 1L;
        Image image = Image.builder()
                .id(imageId)
                .publisher(user)
                .build();

        when(imageRepository.findById(imageId)).thenReturn(Optional.of(image));
        when(userService.getUserByUsername(username)).thenReturn(user);

        Image actual = imageService.getImage(imageId, username);
        assertThat(actual).isEqualTo(image);
    }

    @Test
    @DisplayName("Should get image with admin correctly")
    void shouldGetImageWithAdminCorrectly() {
        PhegyRole role = PhegyRole.builder().level(PhegyRoleLevel.ADMIN).build();
        String username = "principal";
        PhegyUser user = PhegyUser.builder()
                .username(username)
                .roles(Lists.newArrayList(role))
                .build();
        Long imageId = 1L;
        Image image = Image.builder()
                .id(imageId)
                .build();

        when(imageRepository.findById(imageId)).thenReturn(Optional.of(image));
        when(userService.getUserByUsername(username)).thenReturn(user);

        Image actual = imageService.getImage(imageId, username);
        assertThat(actual).isEqualTo(image);
    }

    @Test
    @DisplayName("Should throw exception when get not approved image with user")
    void shouldThrowExceptionWhenGetNotApprovedImageWithUser() {
        PhegyRole role = PhegyRole.builder().level(PhegyRoleLevel.USER).build();
        String username = "principal";
        PhegyUser user = PhegyUser.builder()
                .username(username)
                .roles(Lists.newArrayList(role))
                .build();
        Long imageId = 1L;
        Image image = Image.builder()
                .id(imageId)
                .build();

        when(imageRepository.findById(imageId)).thenReturn(Optional.of(image));
        when(userService.getUserByUsername(username)).thenReturn(user);

        assertThatThrownBy(() -> imageService.getImage(imageId, username))
                .isInstanceOf(PhegyHttpException.class)
                .hasMessage("IMAGE_ID_INVALID");
    }

    @Test
    @DisplayName("Should create image successfully")
    void shouldCreateImageSuccessfully() throws IOException {
        MultipartFile imageFile = Mockito.mock(MultipartFile.class);
        byte[] imageBytes = new byte[10];
        Image image = Image.builder().build();
        String username = "username";
        PhegyUser user = PhegyUser.builder()
                .id(UUID.randomUUID())
                .username(username)
                .build();

        when(userService.getConfirmedUser(username)).thenReturn(user);
        when(imageFile.getBytes()).thenReturn(imageBytes);

        imageService.createImage(imageFile, image, username);

        ArgumentCaptor<Image> imageArgumentCaptor = ArgumentCaptor.forClass(Image.class);
        verify(imageRepository).save(imageArgumentCaptor.capture());
        Image caputredImage = imageArgumentCaptor.getValue();

        assertThat(caputredImage)
                .matches(x -> x.getId() == null, "id is null")
                .matches(x -> x.getApprovedBy() == null, "approved by is null")
                .matches(x -> x.getApprovedOn() == null, "approved on is null")
                .matches(x -> x.getPublisher().equals(user), "is correct publisher")
                .matches(x -> x.getPublishedOn().isBefore(LocalDateTime.now()), "is published before now");

        verify(cloudStorageService).upload(imageBytes, caputredImage.getImageKey(), StoragePath.IMAGE);
        verify(modelValidatorService).validate(caputredImage);
    }

    @Test
    @DisplayName("Should throw exception when can not read image bytes")
    void shouldThrowExceptionWhenCanNotReadImageBytes() throws IOException {
        MultipartFile imageFile = Mockito.mock(MultipartFile.class);
        Image image = Image.builder().build();
        String username = "username";
        PhegyUser user = PhegyUser.builder()
                .id(UUID.randomUUID())
                .username(username)
                .build();

        when(userService.getConfirmedUser(username)).thenReturn(user);
        when(imageFile.getBytes()).thenThrow(IOException.class);

        assertThatThrownBy(() -> imageService.createImage(imageFile, image, username))
                .isInstanceOf(PhegyHttpException.class)
                .hasMessage("CAN_NOT_READ_IMAGE_BYTES");

        verify(cloudStorageService, never()).upload(any(), any(), any());
        verify(modelValidatorService, never()).validate(any(Image.class));
        verify(imageRepository, never()).save(any(Image.class));
    }
    
    @Test
    @DisplayName("Should throw exception when can not save image")
    void shouldThrowExceptionWhenCanNotSaveImage() throws IOException {
        MultipartFile imageFile = Mockito.mock(MultipartFile.class);
        Image image = Image.builder().build();
        String username = "username";
        PhegyUser user = PhegyUser.builder()
                .id(UUID.randomUUID())
                .username(username)
                .build();

        when(userService.getConfirmedUser(username)).thenReturn(user);
        when(imageFile.getBytes()).thenReturn(new byte[10]);
        doThrow(PhegyHttpException.class).when(cloudStorageService).upload(any(), anyString(), any());

        assertThatThrownBy(() -> imageService.createImage(imageFile, image, username))
                .isInstanceOf(PhegyHttpException.class);

        verify(modelValidatorService, never()).validate(any(Image.class));
        verify(imageRepository, never()).save(any(Image.class));
    }

    @Test
    @DisplayName("Should approve image successfully")
    void shouldApproveimageSuccessfully() {
        String username = "username";
        PhegyRole role = PhegyRole.builder().level(PhegyRoleLevel.ADMIN).build();
        PhegyUser admin = PhegyUser.builder()
                .id(UUID.randomUUID())
                .username(username)
                .roles(Lists.newArrayList(role))
                .build();
        PhegyUser publisher = PhegyUser.builder().id(UUID.randomUUID()).build();
        Long imageId = 1L;
        Image image = Image.builder()
                .id(imageId)
                .publisher(publisher)
                .build();

        when(userService.getConfirmedUser(username)).thenReturn(admin);
        when(userService.getUserByUsername(username)).thenReturn(admin);
        when(imageRepository.findById(imageId)).thenReturn(Optional.of(image));
        when(imageRepository.save(image)).thenReturn(image);

        imageService.approveImage(imageId, username);

        ArgumentCaptor<Image> imageArgumentCaptor = ArgumentCaptor.forClass(Image.class);
        verify(imageRepository).save(imageArgumentCaptor.capture());
        assertThat(imageArgumentCaptor.getValue())
                .matches(x -> x.getApprovedBy().equals(admin), "approved by is set")
                .matches(x -> x.getApprovedOn().isBefore(LocalDateTime.now()), "approved on date is set");

        ArgumentCaptor<PhegyUser> publisherArgumetCapture = ArgumentCaptor.forClass(PhegyUser.class);
        verify(notificationService).pushNotificationTo(any(), publisherArgumetCapture.capture());
        assertThat(publisherArgumetCapture.getValue()).isEqualTo(publisher);
    }

    @Test
    @DisplayName("Should throw exception when approve image aleady approved")
    void shouldThrowExceptionWhenApproveImageAleadyApproved() {
        String username = "username";
        PhegyRole role = PhegyRole.builder().level(PhegyRoleLevel.ADMIN).build();
        PhegyUser user = PhegyUser.builder()
                .username(username)
                .roles(Lists.newArrayList(role))
                .build();
        Long imageId = 1L;
        Image image = Image.builder()
                .id(imageId)
                .approvedOn(LocalDateTime.now())
                .build();

        when(userService.getConfirmedUser(username)).thenReturn(user);
        when(imageRepository.findById(imageId)).thenReturn(Optional.of(image));

        assertThatThrownBy(() -> imageService.approveImage(imageId, username))
                .isInstanceOf(PhegyHttpException.class)
                .hasMessage("IMAGE_ALREADY_APPROVED");
    }

    @Test
    @DisplayName("Should reject image successfully")
    void shouldRejectImageSuccessfully() {
        String username = "username";
        PhegyRole role = PhegyRole.builder().level(PhegyRoleLevel.ADMIN).build();
        PhegyUser admin = PhegyUser.builder()
                .id(UUID.randomUUID())
                .username(username)
                .roles(Lists.newArrayList(role))
                .build();
        PhegyUser publisher = PhegyUser.builder().id(UUID.randomUUID()).build();
        Long imageId = 1L;
        String imageKey = "imagekey.png";
        Image image = Image.builder()
                .id(imageId)
                .publisher(publisher)
                .imageKey(imageKey)
                .build();

        when(userService.getConfirmedUser(username)).thenReturn(admin);
        when(userService.getUserByUsername(username)).thenReturn(admin);
        when(imageRepository.findById(imageId)).thenReturn(Optional.of(image));

        imageService.rejectImage(imageId, username);

        verify(cloudStorageService).remove(imageKey, StoragePath.IMAGE);
        verify(imageRepository).delete(image);

        ArgumentCaptor<PhegyUser> publisherArgumetCapture = ArgumentCaptor.forClass(PhegyUser.class);
        verify(notificationService).pushNotificationTo(any(), publisherArgumetCapture.capture());
        assertThat(publisherArgumetCapture.getValue()).isEqualTo(publisher);
    }

    @Test
    @DisplayName("Should thow exception when reject image already approved")
    void shouldThowExceptionWhenRejectImageAlreadyApproved() {
        String username = "username";
        PhegyRole role = PhegyRole.builder().level(PhegyRoleLevel.ADMIN).build();
        PhegyUser user = PhegyUser.builder()
                .username(username)
                .roles(Lists.newArrayList(role))
                .build();
        Long imageId = 1L;
        Image image = Image.builder()
                .id(imageId)
                .approvedOn(LocalDateTime.now())
                .build();

        when(userService.getConfirmedUser(username)).thenReturn(user);
        when(imageRepository.findById(imageId)).thenReturn(Optional.of(image));

        assertThatThrownBy(() -> imageService.rejectImage(imageId, username))
                .isInstanceOf(PhegyHttpException.class)
                .hasMessage("IMAGE_ALREADY_APPROVED");
    }

    @Test
    @DisplayName("Should delete image by admin successfully")
    void shouldDeleteImageByAdminSuccessfully() {
        String username = "username";
        PhegyRole role = PhegyRole.builder().level(PhegyRoleLevel.ADMIN).build();
        PhegyUser admin = PhegyUser.builder()
                .id(UUID.randomUUID())
                .username(username)
                .roles(Lists.newArrayList(role))
                .build();
        PhegyUser publisher = PhegyUser.builder().id(UUID.randomUUID()).build();
        Long imageId = 1L;
        String imageKey = "imagekey.png";
        Image image = Image.builder()
                .id(imageId)
                .publisher(publisher)
                .imageKey(imageKey)
                .build();

        when(userService.getConfirmedUser(username)).thenReturn(admin);
        when(userService.getUserByUsername(username)).thenReturn(admin);
        when(imageRepository.findById(imageId)).thenReturn(Optional.of(image));

        imageService.deleteImage(imageId, username);

        verify(cloudStorageService).remove(imageKey, StoragePath.IMAGE);
        verify(imageRepository).delete(image);

        ArgumentCaptor<PhegyUser> publisherArgumetCapture = ArgumentCaptor.forClass(PhegyUser.class);
        verify(notificationService).pushNotificationTo(any(), publisherArgumetCapture.capture());
        assertThat(publisherArgumetCapture.getValue()).isEqualTo(publisher);
    }

    @Test
    @DisplayName("Should delete image by publisher successfully")
    void shouldDeleteImageByPublisherSuccessfully() {
        String username = "username";
        PhegyRole role = PhegyRole.builder().level(PhegyRoleLevel.USER).build();
        PhegyUser user = PhegyUser.builder()
                .id(UUID.randomUUID())
                .username(username)
                .roles(Lists.newArrayList(role))
                .build();
        Long imageId = 1L;
        String imageKey = "imagekey.png";
        Image image = Image.builder()
                .id(imageId)
                .publisher(user)
                .imageKey(imageKey)
                .build();

        when(userService.getConfirmedUser(username)).thenReturn(user);
        when(userService.getUserByUsername(username)).thenReturn(user);
        when(imageRepository.findById(imageId)).thenReturn(Optional.of(image));

        imageService.deleteImage(imageId, username);

        verify(cloudStorageService).remove(imageKey, StoragePath.IMAGE);
        verify(imageRepository).delete(image);

        verify(notificationService, never()).pushNotificationTo(any(), any());
    }

    @Test
    @DisplayName("Should throw exception when delete image not admin or publisher")
    void shouldThrowExceptionWhenDeleteImageNotAdminOrPublisher() {
        String username = "username";
        PhegyRole role = PhegyRole.builder().level(PhegyRoleLevel.USER).build();
        PhegyUser admin = PhegyUser.builder()
                .id(UUID.randomUUID())
                .username(username)
                .roles(Lists.newArrayList(role))
                .build();
        PhegyUser publisher = PhegyUser.builder().id(UUID.randomUUID()).build();
        Long imageId = 1L;
        Image image = Image.builder()
                .id(imageId)
                .publisher(publisher)
                .approvedOn(LocalDateTime.now())
                .build();

        when(userService.getConfirmedUser(username)).thenReturn(admin);
        when(userService.getUserByUsername(username)).thenReturn(admin);
        when(imageRepository.findById(imageId)).thenReturn(Optional.of(image));

        assertThatThrownBy(() -> imageService.deleteImage(imageId, username))
                .isInstanceOf(PhegyHttpException.class)
                .hasMessage("CAN_NOT_DELETE_FOREIGN_IMAGE");
    }
}