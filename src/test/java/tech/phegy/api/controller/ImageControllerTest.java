package tech.phegy.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tech.phegy.api.dto.image.request.ImageDataDto;
import tech.phegy.api.dto.image.filter.ImageOrderFilter;
import tech.phegy.api.dto.image.filter.ImagePublishFilter;
import tech.phegy.api.mapper.image.ImageMapper;
import tech.phegy.api.model.Image;
import tech.phegy.api.model.user.PhegyRoleLevel;
import tech.phegy.api.service.ImageService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ImageControllerTest {
    @MockBean
    ImageService imageService;
    @MockBean
    ImageMapper imageMapper;

    @Autowired
    WebApplicationContext context;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Should get all images")
    void shouldGetAllImages() throws Exception {
        int page = 1;
        int size = 12;
        ImagePublishFilter publishFilter = ImagePublishFilter.APPROVED;
        ImageOrderFilter orderFilter = ImageOrderFilter.NEWEST;
        String publisher = "publisher";
        String principal = "principal";

        mvc.perform(get("/api/v1/image")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .param("publishFilter", publishFilter.toString())
                        .param("orderFilter", orderFilter.toString())
                        .param("publisher", publisher)
                        .with(user(principal)))
                .andExpect(status().isOk());

        ArgumentCaptor<PageRequest> pageArgumentCaptor = ArgumentCaptor.forClass(PageRequest.class);
        verify(imageService).getImages(pageArgumentCaptor.capture(), eq(publishFilter), eq(orderFilter), eq(publisher), eq(principal));
        assertThat(pageArgumentCaptor.getValue())
                .matches(x -> x.getPageNumber() == page, "is correct page")
                .matches(x -> x.getPageSize() == size, "is correct size");
    }

    @Test
    @DisplayName("Should post image")
    void shouldPostImage() throws Exception {
        String username = "ivan";
        MockMultipartFile imageFile = new MockMultipartFile("image", new byte[0]);
        String title = "title";
        String description = "desc";
        Image image = Image.builder().title(title).description(description).build();

        when(imageMapper.imageDataDtoToImage(any())).thenReturn(image);

        mvc.perform(multipart("/api/v1/image")
                        .file(imageFile)
                        .param("title", title)
                        .param("description", description)
                        .with(user(username)))
                .andExpect(status().isOk());

        ArgumentCaptor<ImageDataDto> dtoArgumentCaptor = ArgumentCaptor.forClass(ImageDataDto.class);
        verify(imageMapper).imageDataDtoToImage(dtoArgumentCaptor.capture());
        assertThat(dtoArgumentCaptor.getValue())
                .matches(x -> x.getTitle().equals(title), "is title set")
                .matches(x -> x.getDescription().equals(description), "is description set");

        verify(imageService).createImage(imageFile, image, username);
    }

    @Test
    @DisplayName("Should approve image admin")
    void shouldApproveImageAdmin() throws Exception {
        String username = "ivan";
        Long imageId = 1L;
        mvc.perform(post("/api/v1/image/approve/" + imageId)
                        .with(user(username)
                                .roles(PhegyRoleLevel.ADMIN.toString())))
                .andExpect(status().isOk());

        verify(imageService).approveImage(imageId, username);
    }

    @Test
    @DisplayName("Should not allow approve image user")
    void shouldNotAllowApproveImageUser() throws Exception {
        String username = "ivan";
        long imageId = 1L;
        mvc.perform(post("/api/v1/image/approve/" + imageId)
                        .with(user(username)
                                .roles(PhegyRoleLevel.USER.toString())))
                .andExpect(status().isForbidden());

        verify(imageService, never()).approveImage(anyLong(), anyString());
    }

    @Test
    @DisplayName("Should reject image admin")
    void shouldRejectImageAdmin() throws Exception {
        String username = "ivan";
        Long imageId = 1L;
        mvc.perform(delete("/api/v1/image/reject/" + imageId)
                        .with(user(username)
                                .roles(PhegyRoleLevel.ADMIN.toString())))
                .andExpect(status().isOk());

        verify(imageService).rejectImage(imageId, username);
    }

    @Test
    @DisplayName("Should not allow reject image user")
    void shouldNotAllowRejectImageUser() throws Exception {
        String username = "ivan";
        long imageId = 1L;
        mvc.perform(delete("/api/v1/image/reject/" + imageId)
                        .with(user(username)
                                .roles(PhegyRoleLevel.USER.toString())))
                .andExpect(status().isForbidden());

        verify(imageService, never()).rejectImage(anyLong(), anyString());
    }

    @Test
    @DisplayName("Should delete image")
    void shouldDeleteImage() throws Exception {
        String username = "ivan";
        Long imageId = 1L;
        mvc.perform(delete("/api/v1/image/" + imageId)
                        .with(user(username)))
                .andExpect(status().isOk());

        verify(imageService).deleteImage(imageId, username);
    }
}
