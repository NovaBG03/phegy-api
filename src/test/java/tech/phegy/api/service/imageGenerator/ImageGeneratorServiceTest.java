package tech.phegy.api.service.imageGenerator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import tech.phegy.api.exception.PhegyHttpException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class ImageGeneratorServiceTest {
    @Mock
    ImageGeneratorProps imageGeneratorProps;
    @Mock
    RestTemplate restTemplate;

    ImageGeneratorService imageGeneratorService;

    String url = "https://{domain}/{avatarType}/{size}/{username}?square&colors={colorPalette}";
    String username = "ivan";
    String domain = "test.com";
    String avatarType = "circle";
    float sizePx = 250;
    String colorPalette = "somerandomcolors";

    @BeforeEach
    void setUp() {
        imageGeneratorService = new ImageGeneratorService(imageGeneratorProps, restTemplate);
    }

    @Test
    @DisplayName("Should generate profile picture successfully")
    void shouldGenerateProfilePictureSuccessfully() {
        byte[] imageBytes = {60, 63, 120, 109, 108, 32, 118, 101, 114, 115, 105, 111, 110, 61, 34, 49, 46, 48, 34, 32, 63, 62, 60, 115, 118, 103, 32, 119, 105, 100, 116, 104, 61, 34, 52, 56, 112, 120, 34, 32, 104, 101, 105, 103, 104, 116, 61, 34, 52, 56, 112, 120, 34, 32, 118, 105, 101, 119, 66, 111, 120, 61, 34, 48, 32, 48, 32, 52, 56, 32, 52, 56, 34, 32, 100, 97, 116, 97, 45, 110, 97, 109, 101, 61, 34, 76, 97, 121, 101, 114, 32, 49, 34, 32, 105, 100, 61, 34, 76, 97, 121, 101, 114, 95, 49, 34, 32, 120, 109, 108, 110, 115, 61, 34, 104, 116, 116, 112, 58, 47, 47, 119, 119, 119, 46, 119, 51, 46, 111, 114, 103, 47, 50, 48, 48, 48, 47, 115, 118, 103, 34, 62, 60, 100, 101, 102, 115, 62, 60, 115, 116, 121, 108, 101, 62, 46, 99, 108, 115, 45, 49, 123, 102, 105, 108, 108, 58, 35, 53, 53, 97, 97, 101, 49, 59, 125, 46, 99, 108, 115, 45, 50, 123, 111, 112, 97, 99, 105, 116, 121, 58, 48, 46, 51, 53, 59, 125, 60, 47, 115, 116, 121, 108, 101, 62, 60, 47, 100, 101, 102, 115, 62, 60, 116, 105, 116, 108, 101, 47, 62, 60, 112, 97, 116, 104, 32, 99, 108, 97, 115, 115, 61, 34, 99, 108, 115, 45, 49, 34, 32, 100, 61, 34, 77, 52, 52, 44, 50, 50, 97, 49, 44, 49, 44, 48, 44, 48, 44, 49, 45, 46, 54, 50, 45, 46, 50, 50, 108, 45, 49, 56, 46, 55, 53, 45, 49, 53, 97, 49, 44, 49, 44, 48, 44, 48, 44, 48, 45, 49, 46, 50, 53, 44, 48, 108, 45, 49, 56, 46, 55, 54, 44, 49, 53, 97, 49, 44, 49, 44, 48, 44, 49, 44, 49, 45, 49, 46, 50, 52, 45, 49, 46, 53, 54, 108, 49, 56, 46, 55, 53, 45, 49, 53, 97, 51, 44, 51, 44, 48, 44, 48, 44, 49, 44, 51, 46, 55, 52, 44, 48, 108, 49, 56, 46, 55, 53, 44, 49, 53, 97, 49, 44, 49, 44, 48, 44, 48, 44, 49, 44, 46, 49, 54, 44, 49, 46, 52, 65, 49, 44, 49, 44, 48, 44, 48, 44, 49, 44, 52, 52, 44, 50, 50, 90, 34, 47, 62, 60, 112, 97, 116, 104, 32, 99, 108, 97, 115, 115, 61, 34, 99, 108, 115, 45, 49, 34, 32, 100, 61, 34, 77, 51, 55, 44, 52, 52, 72, 49, 49, 97, 51, 44, 51, 44, 48, 44, 48, 44, 49, 45, 51, 45, 51, 86, 49, 55, 46, 49, 54, 104, 50, 86, 52, 49, 97, 49, 44, 49, 44, 48, 44, 48, 44, 48, 44, 49, 44, 49, 72, 51, 55, 97, 49, 44, 49, 44, 48, 44, 48, 44, 48, 44, 49, 45, 49, 86, 49, 55, 46, 49, 54, 104, 50, 86, 52, 49, 65, 51, 44, 51, 44, 48, 44, 48, 44, 49, 44, 51, 55, 44, 52, 52, 90, 34, 47, 62, 60, 103, 32, 99, 108, 97, 115, 115, 61, 34, 99, 108, 115, 45, 50, 34, 62, 60, 112, 97, 116, 104, 32, 99, 108, 97, 115, 115, 61, 34, 99, 108, 115, 45, 49, 34, 32, 100, 61, 34, 77, 51, 55, 44, 49, 53, 72, 51, 54, 86, 51, 55, 97, 51, 44, 51, 44, 48, 44, 48, 44, 49, 45, 51, 44, 51, 72, 56, 118, 49, 97, 51, 44, 51, 44, 48, 44, 48, 44, 48, 44, 51, 44, 51, 72, 51, 55, 97, 51, 44, 51, 44, 48, 44, 48, 44, 48, 44, 51, 45, 51, 86, 49, 56, 65, 51, 44, 51, 44, 48, 44, 48, 44, 48, 44, 51, 55, 44, 49, 53, 90, 34, 47, 62, 60, 47, 103, 62, 60, 47, 115, 118, 103, 62};

        when(imageGeneratorProps.getDomain()).thenReturn(domain);
        when(imageGeneratorProps.getAvatarType()).thenReturn(avatarType);
        when(imageGeneratorProps.getSizePx()).thenReturn(sizePx);
        when(imageGeneratorProps.getColorPalette()).thenReturn(colorPalette);
        when(restTemplate.getForObject(url, byte[].class, domain, avatarType, sizePx, username, colorPalette))
                .thenReturn(imageBytes);

        byte[] actualButes = imageGeneratorService.generateProfilePic(username);

        assertThat(actualButes).isNotNull();
    }

    @Test
    @DisplayName("Should throw exception when can not generate profile picture")
    void shouldThrowExceptionWhenCanNotGenerateProfilePicture() {
        when(imageGeneratorProps.getDomain()).thenReturn(domain);
        when(imageGeneratorProps.getAvatarType()).thenReturn(avatarType);
        when(imageGeneratorProps.getSizePx()).thenReturn(sizePx);
        when(imageGeneratorProps.getColorPalette()).thenReturn(colorPalette);
        when(restTemplate.getForObject(url, byte[].class, domain, avatarType, sizePx, username, colorPalette))
                .thenThrow(RestClientException.class);

        assertThatThrownBy(() -> imageGeneratorService.generateProfilePic(username))
                .isInstanceOf(PhegyHttpException.class)
                .hasMessage("CAN_NOT_GENERATE_PROFILE_PIC");
    }

    @Test
    @DisplayName("Should throw exception when can not convert correctly profile pic")
    void shouldThrowExceptionWhenCanNotConvertCorrectlyProfilePic() {
        byte[] imageBytes = {};

        when(imageGeneratorProps.getDomain()).thenReturn(domain);
        when(imageGeneratorProps.getAvatarType()).thenReturn(avatarType);
        when(imageGeneratorProps.getSizePx()).thenReturn(sizePx);
        when(imageGeneratorProps.getColorPalette()).thenReturn(colorPalette);
        when(restTemplate.getForObject(url, byte[].class, domain, avatarType, sizePx, username, colorPalette))
                .thenReturn(imageBytes);

        assertThatThrownBy(() -> imageGeneratorService.generateProfilePic(username))
                .isInstanceOf(PhegyHttpException.class)
                .hasMessage("CAN_NOT_CONVERT_CORRECTLY_PROFILE_PIC");
    }
}