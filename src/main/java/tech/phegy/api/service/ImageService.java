package tech.phegy.api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tech.phegy.api.exception.PhegyHttpException;
import tech.phegy.api.model.Image;
import tech.phegy.api.service.storage.CloudStorageService;
import tech.phegy.api.service.storage.StoragePath;
import tech.phegy.api.service.validator.ModelValidatorService;
import tech.phegy.api.dto.image.response.ImagePageResponseDto;
import tech.phegy.api.dto.image.filter.ImageOrderFilter;
import tech.phegy.api.mapper.image.ImageMapper;
import tech.phegy.api.dto.image.filter.ImagePublishFilter;
import tech.phegy.api.model.notification.Notification;
import tech.phegy.api.model.notification.NotificationCategory;
import tech.phegy.api.model.user.PhegyUser;
import tech.phegy.api.repository.ImageRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for managing images.
 *
 * @author Nikita
 */
@Service
public class ImageService {
    private final ImageRepository imageRepository;
    private final PhegyUserService userService;
    private final CloudStorageService cloudStorageService;
    private final NotificationService notificationService;
    private final ModelValidatorService modelValidatorService;
    private final ImageMapper imageMapper;

    /**
     * Constructs new instance with needed dependencies.
     */
    public ImageService(ImageRepository imageRepository,
                        PhegyUserService userService,
                        CloudStorageService cloudStorageService,
                        NotificationService notificationService,
                        ModelValidatorService modelValidatorService,
                        ImageMapper imageMapper) {
        this.imageRepository = imageRepository;
        this.userService = userService;
        this.cloudStorageService = cloudStorageService;
        this.notificationService = notificationService;
        this.modelValidatorService = modelValidatorService;
        this.imageMapper = imageMapper;
    }

    /**
     * Get count of public images published by a specific user.
     *
     * @param username image publisher.
     * @return count of public images.
     * @throws UsernameNotFoundException if the user does not exist.
     */
    public Long getImagesCount(String username) throws UsernameNotFoundException {
        userService.getUserByUsername(username);
        return imageRepository.countByPublisherUsernameAndApprovedOnNotNull(username);
    }

    /**
     * Get page of images.
     *
     * @param pageRequest       selected page and size.
     * @param publishFilter     image publish filter.
     * @param orderFilter       image order filter.
     * @param publisherUsername publisher username or null.
     * @param principalUsername principal username or null.
     * @return page of images.
     * @throws PhegyHttpException when can not get images.
     */
    public ImagePageResponseDto getImages(PageRequest pageRequest,
                                          ImagePublishFilter publishFilter,
                                          ImageOrderFilter orderFilter,
                                          String publisherUsername,
                                          String principalUsername) throws PhegyHttpException {
        // retrieve user from database
        PhegyUser principal;
        try {
            principal = this.userService.getUserByUsername(principalUsername);
        } catch (UsernameNotFoundException e) {
            principal = null;
        }

        final boolean isPublisherOrAdmin = principal != null
                && (principal.isAdminOrModerator() || principal.getUsername().equals(publisherUsername));

        // properties to be populated after the filtration
        Page<Image> imagePage;

        // determine and get requested images
        switch (publishFilter) {
            case APPROVED:
                // only approved images are requested
                imagePage = this.getApprovedImages(publisherUsername, pageRequest, orderFilter);
                break;
            case PENDING:
                // only the publisher and admins/moderators have access to the pending images
                if (!isPublisherOrAdmin) {
                    throw new PhegyHttpException("IMAGE_FILTER_NOT_ALLOWED", HttpStatus.FORBIDDEN);
                }

                // only pending images requested
                imagePage = this.getPendingImages(publisherUsername, pageRequest, orderFilter);
                break;
            case ALL:
                // only the publisher and admins/moderators have access to the pending images
                if (!isPublisherOrAdmin) {
                    throw new PhegyHttpException("IMAGE_FILTER_NOT_ALLOWED", HttpStatus.FORBIDDEN);
                }

                // all images requested
                imagePage = this.getAllImages(publisherUsername, pageRequest, orderFilter);
                break;
            default:
                // no filter type provided
                throw new PhegyHttpException("IMAGE_FILTER_NOT_ALLOWED", HttpStatus.FORBIDDEN);
        }

        return imageMapper.createImagePageResponseDto(imagePage, isPublisherOrAdmin);
    }

    /**
     * Get a specific image.
     *
     * @param imageId           image id.
     * @param principalUsername principal username or null.
     * @return image.
     * @throws PhegyHttpException when can not find image with that id.
     */
    public Image getImage(Long imageId, String principalUsername) throws PhegyHttpException {
        if (principalUsername == null) {
            return this.imageRepository.findByIdAndApprovedOnNotNull(imageId)
                    .orElseThrow(() -> new PhegyHttpException("IMAGE_ID_INVALID", HttpStatus.NOT_FOUND));
        }

        final Image image = this.imageRepository.findById(imageId)
                .orElseThrow(() -> new PhegyHttpException("IMAGE_ID_INVALID", HttpStatus.NOT_FOUND));

        final PhegyUser user = this.userService.getUserByUsername(principalUsername);
        if (image.isApproved()
                || user.isAdminOrModerator()
                || user.equals(image.getPublisher())) {
            return image;
        }

        throw new PhegyHttpException("IMAGE_ID_INVALID", HttpStatus.NOT_FOUND);
    }

    /**
     * Create new image.
     *
     * @param imageFile         image file.
     * @param image             image object.
     * @param principalUsername publisher username.
     * @throws PhegyHttpException when can not create image.
     */
    @Transactional
    public void createImage(MultipartFile imageFile, Image image, String principalUsername) throws PhegyHttpException {
        final PhegyUser publisher = this.userService.getConfirmedUser(principalUsername);

        try {
            final String imageId = UUID.randomUUID() + ".png";
            this.cloudStorageService.upload(imageFile.getBytes(), imageId, StoragePath.IMAGE);
            image.setImageKey(imageId);
        } catch (IOException e) {
            throw new PhegyHttpException("CAN_NOT_READ_IMAGE_BYTES", HttpStatus.BAD_REQUEST);
        }

        image.setId(null);
        image.setPublisher(publisher);
        image.setApprovedBy(null);
        image.setApprovedOn(null);
        image.setPublishedOn(LocalDateTime.now());

        if (image.getDescription() == null
                || image.getDescription().length() == 0
                || image.getDescription().equals("null")) {
            image.setDescription(null);
        }

        this.modelValidatorService.validate(image);
        this.imageRepository.save(image);
    }

    /**
     * Approve image.
     *
     * @param imageId            image id.
     * @param principalUsername principal username.
     * @throws PhegyHttpException when can not approve image.
     */
    @Transactional
    public void approveImage(Long imageId, String principalUsername) throws PhegyHttpException {
        final PhegyUser principal = this.userService.getConfirmedUser(principalUsername);
        final Image image = this.getImage(imageId, principalUsername);

        if (image.isApproved()) {
            throw new PhegyHttpException("IMAGE_ALREADY_APPROVED", HttpStatus.BAD_REQUEST);
        }

        image.setApprovedBy(principal);
        image.setApprovedOn(LocalDateTime.now());

        final Image approvedImage = this.imageRepository.save(image);
        this.notificationService.pushNotificationTo(
                Notification.builder()
                        .title("Снимката е публичка!")
                        .message("Снимката Ви \"" + approvedImage.getTitle() + "\" е одобрена от " + principal.getUsername())
                        .category(NotificationCategory.SUCCESS)
                        .build(),
                approvedImage.getPublisher());
    }

    /**
     * Reject image upload request and delete it.
     *
     * @param imageId            id of the image to be rejected.
     * @param principalUsername principal's username.
     * @throws PhegyHttpException when can not reject image.
     */
    @Transactional
    public void rejectImage(Long imageId, String principalUsername) throws PhegyHttpException {
        // retrieve image from database
        final PhegyUser principal = this.userService.getConfirmedUser(principalUsername);
        final Image image = this.getImage(imageId, principalUsername);

        // check if image is already approved
        if (image.isApproved()) {
            throw new PhegyHttpException("IMAGE_ALREADY_APPROVED", HttpStatus.BAD_REQUEST);
        }

        // remove image from the storage
        this.removeImage(image);

        // push notification to the image publisher
        this.notificationService.pushNotificationTo(
                Notification.builder()
                        .title("Неодобрена снимка!")
                        .message("Снимката Ви \"" + image.getTitle() + "\" не е бе одобрена от " + principal.getUsername())
                        .category(NotificationCategory.DANGER)
                        .build(),
                image.getPublisher());
    }

    /**
     * Delete image.
     *
     * @param imageId            id of the image to be deleted.
     * @param principalUsername principal's username.
     * @throws PhegyHttpException when can not delete image.
     */
    @Transactional
    public void deleteImage(Long imageId, String principalUsername) throws PhegyHttpException {
        // retrieve confirmed user and image from database
        final PhegyUser principal = this.userService.getConfirmedUser(principalUsername);
        final Image image = this.getImage(imageId, principalUsername);

        // check if principal is publisher or admin/moderator
        if (!image.getPublisher().equals(principal) && !principal.isAdminOrModerator()) {
            throw new PhegyHttpException("CAN_NOT_DELETE_FOREIGN_IMAGE", HttpStatus.UNAUTHORIZED);
        }

        // remove image from the storage
        this.removeImage(image);

        // if publisher is different from the principal, push notification to the publisher
        if (!image.getPublisher().equals(principal)) {
            this.notificationService.pushNotificationTo(
                    Notification.builder()
                            .title("Изтрита снимка!")
                            .message("Снимката Ви \"" + image.getTitle() + "\" беше изтрита от " + principalUsername)
                            .category(NotificationCategory.DANGER)
                            .build(),
                    image.getPublisher());
        }
    }

    /**
     * Remove image from the database and the cloud storage.
     *
     * @param image image to be deleted.
     */
    private void removeImage(Image image) {
        this.cloudStorageService.remove(image.getImageKey(), StoragePath.IMAGE);
        this.imageRepository.delete(image);
    }

    private Page<Image> getApprovedImages(String publisherUsername, PageRequest pageRequest, ImageOrderFilter orderFilter) {
        try {
            pageRequest = getPageRequestWithBasicOrderFilter(pageRequest, orderFilter);
        } catch (PhegyHttpException e) {
            return this.getApprovedImagesWithAdvanceOrderFilter(publisherUsername, pageRequest, orderFilter);
        }

        if (publisherUsername != null) {
            // approved images from a specific user
            return this.imageRepository.findAllByPublisherUsernameAndApprovedOnNotNull(publisherUsername, pageRequest);
        }
        // approved images from all users
        return this.imageRepository.findAllByApprovedOnNotNull(pageRequest);
    }

    private Page<Image> getApprovedImagesWithAdvanceOrderFilter(String publisherUsername, PageRequest pageRequest, ImageOrderFilter orderFilter) {
        int daysFromNow = 0;
        switch (orderFilter) {
            case LATEST_VOTED:
                if (publisherUsername != null) {
                    return this.imageRepository.findAllByPublisherUsernameApprovedOnNotNullOrderByLatestTipped(publisherUsername, pageRequest);
                }
                return this.imageRepository.findAllByApprovedOnNotNullOrderByLatestTipped(pageRequest);
            case MOST_VOTED:
                if (publisherUsername != null) {
                    return this.imageRepository.findAllByPublisherUsernameApprovedOnNotNullOrderByMostTipped(publisherUsername, pageRequest);
                }
                return this.imageRepository.findAllByApprovedOnNotNullOrderByMostTipped(pageRequest);
            case TOP_VOTED_LAST_3_DAYS:
                daysFromNow = 3;
                break;
            case TOP_VOTED_LAST_WEEK:
                daysFromNow = 7;
                break;
            case TOP_VOTED_LAST_MONTH:
                daysFromNow = 30;
                break;
        }

        if (publisherUsername == null && daysFromNow > 0) {
            return this.imageRepository.findAllByApprovedOnNotNullOrderByTopTipped(pageRequest, daysFromNow);
        }

        throw new PhegyHttpException("IMAGE_FILTER_NOT_ALLOWED", HttpStatus.FORBIDDEN);
    }

    private Page<Image> getPendingImages(String publisherUsername, PageRequest pageRequest, ImageOrderFilter orderFilter) {
        pageRequest = getPageRequestWithBasicOrderFilter(pageRequest, orderFilter);

        if (publisherUsername != null) {
            // pending images from a specific user
            return this.imageRepository.findAllByPublisherUsernameAndApprovedOnNull(publisherUsername, pageRequest);
        }

        // pending images from all users
        return this.imageRepository.findAllByApprovedOnNull(pageRequest);
    }

    private Page<Image> getAllImages(String publisherUsername, PageRequest pageRequest, ImageOrderFilter orderFilter) {
        pageRequest = getPageRequestWithBasicOrderFilter(pageRequest, orderFilter);

        if (publisherUsername != null) {
            // all images from a specific user
            return this.imageRepository.findAllByPublisherUsername(publisherUsername, pageRequest);
        }

        // all images from all users
        return this.imageRepository.findAll(pageRequest);
    }

    private PageRequest getPageRequestWithBasicOrderFilter(PageRequest pageRequest, ImageOrderFilter orderFilter) {
        switch (orderFilter) {
            case NEWEST:
                return pageRequest.withSort(Sort.by(Sort.Direction.DESC, "approvedOn", "publishedOn"));
            case OLDEST:
                return pageRequest.withSort(Sort.by(Sort.Direction.ASC, "approvedOn", "publishedOn"));
            default:
                throw new PhegyHttpException("IMAGE_FILTER_NOT_ALLOWED", HttpStatus.FORBIDDEN);
        }
    }
}
