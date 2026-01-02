package com.codebymathabo.s3entinel;

import com.codebymathabo.s3entinel.service.FileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private DynamoDbClient dynamoDbClient;

    private FileServiceImpl fileService;

    @BeforeEach
    void setUp() {
        // Injected MOCK clients into our service
        fileService = new FileServiceImpl(s3Client, dynamoDbClient, "test-bucket");
    }

    @Test
    void uploadFile_ShouldUploadToS3AndSaveToDynamoDB() throws ExecutionException, InterruptedException {
        // Arrange
        byte[] mockData = "Hello World".getBytes();
        String fileName = "test.jpg";
        String contentType = "image/jpeg";

        // Act
        CompletableFuture<Object> result = fileService.uploadFile(mockData, fileName, contentType);

        // Assert
        assertNotNull(result);
        String message = (String) result.get(); // Wait for async result
        assertTrue(message.contains("File processed successfully"));

        // VERIFY: Was S3 actually called?
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));

        // VERIFY: Was DynamoDB actually called?
        verify(dynamoDbClient, times(1)).putItem(any(PutItemRequest.class));
    }

    @Test
    void uploadFile_ShouldFail_WhenFileTypeInvalid() {
        // Arrange
        byte[] mockData = "Bad Data".getBytes();
        String contentType = "application/exe"; // Not allowed

        // Act & Assert
        CompletableFuture<Object> result = fileService.uploadFile(mockData, "virus.exe", contentType);

        // Expect a failure
        assertTrue(result.isCompletedExceptionally());

        // Verify was S3 NEVER called
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }
}