package tech.phegy.api.service.imageGenerator;

import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import tech.phegy.api.exception.PhegyHttpException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Service for generating profile images.
 *
 * @author Nikita
 */
@Component
public class ImageGeneratorService {
    private final ImageGeneratorProps imageGeneratorProps;
    private final RestTemplate restTemplate;

    /**
     * Constructs new instance with needed dependencies.
     */
    public ImageGeneratorService(ImageGeneratorProps imageGeneratorProps, RestTemplate restTemplate) {
        this.imageGeneratorProps = imageGeneratorProps;
        this.restTemplate = restTemplate;
    }

    /**
     * Generate personal profile picture for a specific user.
     *
     * @param username to generate profile picture for.
     * @return bytes of the newly generated png image.
     * @throws PhegyHttpException when can not generate profile picture or can not convert it to the correct format
     */
    public byte[] generateProfilePic(String username) throws PhegyHttpException {
        final String url = "https://{domain}/{avatarType}/{size}/{username}?square&colors={colorPalette}";

        byte[] svgBytes;
        try {
            svgBytes = this.restTemplate.getForObject(url, byte[].class,
                    this.imageGeneratorProps.getDomain(),
                    this.imageGeneratorProps.getAvatarType(),
                    this.imageGeneratorProps.getSizePx(),
                    username,
                    this.imageGeneratorProps.getColorPalette());
        } catch (Exception e) {
            throw new PhegyHttpException("CAN_NOT_GENERATE_PROFILE_PIC", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        byte[] pngBytes;
        try {
            pngBytes = convertSvgToPng(svgBytes, this.imageGeneratorProps.getSizePx(), this.imageGeneratorProps.getSizePx());
        } catch (Exception e) {
            throw new PhegyHttpException("CAN_NOT_CONVERT_CORRECTLY_PROFILE_PIC", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return pngBytes;
    }

    private static byte[] convertSvgToPng(byte[] svgBytes, float height, float width)
            throws TranscoderException, IOException {
        ByteArrayOutputStream resultByteStream = new ByteArrayOutputStream();

        TranscoderInput transcoderInput = new TranscoderInput(new ByteArrayInputStream(svgBytes));
        TranscoderOutput transcoderOutput = new TranscoderOutput(resultByteStream);

        PNGTranscoder pngTranscoder = new PNGTranscoder();
        pngTranscoder.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, height);
        pngTranscoder.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, width);
        pngTranscoder.transcode(transcoderInput, transcoderOutput);

        resultByteStream.flush();
        return resultByteStream.toByteArray();
    }
}
