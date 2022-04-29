package tech.phegy.api.service.storage;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import tech.phegy.api.exception.PhegyHttpException;

/**
 * Service for storing images on the clould CDN.
 *
 * @author Nikita
 */
@Service
public class CloudStorageService {
    private final S3Client s3Client;
    private final AwsStorageProps awsConfig;

    /**
     * Constructs new instance with needed dependencies.
     */
    public CloudStorageService(S3Client s3Client, AwsStorageProps awsConfig) {
        this.s3Client = s3Client;
        this.awsConfig = awsConfig;
    }

    /**
     * Upload file to the clould CDN.
     *
     * @param fileBytes file bytes to upload.
     * @param fileKey   file name.
     * @param path      path to file on the CDN.
     * @throws PhegyHttpException when can not upload file.
     */
    public void upload(byte[] fileBytes, String fileKey, StoragePath path) throws PhegyHttpException {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(awsConfig.getBucketName())
                .key(path.toString().toLowerCase() + "/" + fileKey)
                .build();
        try {
            s3Client.putObject(putRequest, RequestBody.fromBytes(fileBytes));
        } catch (Exception e) {
            throw new PhegyHttpException("CAN_NOT_UPLOAD_FILE", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Remove file from the clould CDN.
     *
     * @param fileKey file name.
     * @param path    path to file on the CDN.
     * @throws PhegyHttpException when can not remove file.
     */
    public void remove(String fileKey, StoragePath path) {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(awsConfig.getBucketName())
                .key(path.toString().toLowerCase() + "/" + fileKey)
                .build();
        try {
            s3Client.deleteObject(deleteRequest);
        } catch (Exception e) {
            throw new PhegyHttpException("CAN_NOT_REMOVE_FILE", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
