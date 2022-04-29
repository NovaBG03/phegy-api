package tech.phegy.api.service.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import tech.phegy.api.exception.PhegyHttpException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudStorageServiceTest {
    @Mock
    S3Client s3Client;
    @Mock
    AwsStorageProps awsConfig;

    CloudStorageService cloudStorageService;

    @BeforeEach
    void setUp() {
        cloudStorageService = new CloudStorageService(s3Client, awsConfig);
    }

    @Test
    @DisplayName("Should upload file successfully")
    void shouldUploadFileSuccessfully() {
        String bucketName = "bucket";
        byte[] fileBytes = new byte[10];
        String fileKey = "key";
        StoragePath path = StoragePath.IMAGE;

        when(awsConfig.getBucketName()).thenReturn(bucketName);

        cloudStorageService.upload(fileBytes, fileKey, path);

        ArgumentCaptor<PutObjectRequest> putObjectRequestArgumentCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(putObjectRequestArgumentCaptor.capture(), any(RequestBody.class));
        PutObjectRequest putObjectRequest = putObjectRequestArgumentCaptor.getValue();
        assertThat(putObjectRequest.bucket()).isEqualTo(bucketName);
        assertThat(putObjectRequest.key()).isEqualTo(path.toString().toLowerCase() + "/" + fileKey);
    }

    @Test
    @DisplayName("Should throw exception when can not upload file")
    void shouldThrowExceptionWhenCanNotUploadFile() {
        String bucketName = "bucket";
        byte[] fileBytes = new byte[10];
        String fileKey = "key";
        StoragePath path = StoragePath.IMAGE;

        when(awsConfig.getBucketName()).thenReturn(bucketName);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenThrow(S3Exception.class);

        assertThatThrownBy(() -> cloudStorageService.upload(fileBytes, fileKey, path))
                .isInstanceOf(PhegyHttpException.class)
                .hasMessage("CAN_NOT_UPLOAD_FILE");
    }

    @Test
    @DisplayName("Should remove file successfully")
    void shouldRemoveFileSuccessfully() {
        String bucketName = "bucket";
        String fileKey = "key";
        StoragePath path = StoragePath.IMAGE;

        when(awsConfig.getBucketName()).thenReturn(bucketName);

        cloudStorageService.remove(fileKey, path);

        ArgumentCaptor<DeleteObjectRequest> deleteObjectRequestArgumentCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(deleteObjectRequestArgumentCaptor.capture());
        DeleteObjectRequest deleteObjectRequest = deleteObjectRequestArgumentCaptor.getValue();
        assertThat(deleteObjectRequest.bucket()).isEqualTo(bucketName);
        assertThat(deleteObjectRequest.key()).isEqualTo(path.toString().toLowerCase() + "/" + fileKey);
    }

    @Test
    @DisplayName("Should throw exception when can not remove file")
    void shouldThrowExceptionWhenCanNotRemoveFile() {
        String bucketName = "bucket";
        String fileKey = "key";
        StoragePath path = StoragePath.IMAGE;

        when(awsConfig.getBucketName()).thenReturn(bucketName);
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenThrow(S3Exception.class);

        assertThatThrownBy(() -> cloudStorageService.remove(fileKey, path))
                .isInstanceOf(PhegyHttpException.class)
                .hasMessage("CAN_NOT_REMOVE_FILE");
    }
}