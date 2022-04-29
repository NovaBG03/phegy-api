package tech.phegy.api.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tech.phegy.api.dto.image.filter.ImageOrderFilter;
import tech.phegy.api.dto.image.filter.ImagePublishFilter;
import tech.phegy.api.dto.image.request.ImageDataDto;
import tech.phegy.api.dto.image.response.ImagePageResponseDto;
import tech.phegy.api.mapper.image.ImageMapper;
import tech.phegy.api.service.ImageService;

import java.security.Principal;

/**
 * Image controller.
 *
 * @author Nikita
 */
@RestController
@RequestMapping("/api/v1/image")
public class ImageController {
    private final ImageService imageService;
    private final ImageMapper imageMapper;

    public ImageController(ImageService imageService, ImageMapper imageMapper) {
        this.imageService = imageService;
        this.imageMapper = imageMapper;
    }

    /**
     * Get a page of images by a specific creteria.
     *
     * @param page              default page 0
     * @param size              default size 4
     * @param publishFilter     default APPROVED
     * @param orderFilter       default NEWEST
     * @param publisherUsername default null -> all users
     * @param authentication    user authentication
     * @return image page response dto.
     */
    @GetMapping()
    public ImagePageResponseDto getImages(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "4") int size,
                                          @RequestParam(defaultValue = "APPROVED") ImagePublishFilter publishFilter,
                                          @RequestParam(defaultValue = "NEWEST") ImageOrderFilter orderFilter,
                                          @RequestParam(name = "publisher", required = false) String publisherUsername,
                                          Authentication authentication) {
        final PageRequest pageRequest = PageRequest.of(page, size);
        final String principalUsername = authentication != null ? authentication.getName() : null;
        return this.imageService.getImages(pageRequest, publishFilter, orderFilter, publisherUsername, principalUsername);
    }

    @PostMapping
    public void postImage(@RequestParam MultipartFile image,
                          @ModelAttribute ImageDataDto iamgeDto,
                          Principal principal) {
        this.imageService.createImage(image, imageMapper.imageDataDtoToImage(iamgeDto), principal.getName());
    }

    @PostMapping("/approve/{imageId}")
    @Secured({"ROLE_ADMIN", "ROLE_MODERATOR"})
    public void approveImage(@PathVariable Long imageId, Principal principal) {
        this.imageService.approveImage(imageId, principal.getName());
    }

    @DeleteMapping("/reject/{imageId}")
    @Secured({"ROLE_ADMIN", "ROLE_MODERATOR"})
    public void rejectImage(@PathVariable Long imageId, Principal principal) {
        this.imageService.rejectImage(imageId, principal.getName());
    }

    @DeleteMapping("/{imageId}")
    public void deleteImage(@PathVariable Long imageId, Principal principal) {
        this.imageService.deleteImage(imageId, principal.getName());
    }
}
