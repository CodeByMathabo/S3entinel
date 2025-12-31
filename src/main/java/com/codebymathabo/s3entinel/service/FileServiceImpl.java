package com.codebymathabo.s3entinel.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class FileServiceImpl implements FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);

    private final S3Client s3Client;
    private final DynamoDbClient dynamoDbClient; // Database Connection
    private final String bucketName;
    private final String tableName = "S3entinel_Files";

    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png", "application/pdf", "text/plain");

    public FileServiceImpl(@Value("${aws.access.key.id}") String accessKey,
                           @Value("${aws.secret.access.key}") String secretKey,
                           @Value("${aws.region}") String region,
                           @Value("${aws.s3.bucket.name}") String bucketName) {

        this.bucketName = bucketName;

        // Creates the credentials object once to reuse for both clients
        var credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey));

        // 1. Initialize S3 Client
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentials)
                .build();

        // 2. Initialize DynamoDB Client
        this.dynamoDbClient = DynamoDbClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credentials)
                .build();
    }

    @Override
    @Async
    public CompletableFuture<Object> uploadFile(byte[] fileData, String fileName, String contentType) {
        logger.info("Service Layer: Processing file in thread: {}", Thread.currentThread().getName());

        if (!isValidFileType(contentType)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid file type."));
        }

        try {
            // Generate a unique ID for the database
            String fileId = UUID.randomUUID().toString();
            String uniqueFileName = fileId + "_" + fileName;

            // --- Step A: Upload to S3 ---
            PutObjectRequest putOb = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(uniqueFileName)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putOb, RequestBody.fromBytes(fileData));
            logger.info("File uploaded to S3: {}", uniqueFileName);

            // --- Step B: Save Metadata to DynamoDB ---
            saveMetaData(fileId, uniqueFileName, contentType, fileData.length);

            return CompletableFuture.completedFuture("File processed successfully. ID: " + fileId);

        } catch (Exception e) {
            logger.error("Upload process failed", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private void saveMetaData(String id, String fileName, String contentType, long size) {
        // Prepare the data card (Item)
        Map<String, AttributeValue> item = new HashMap<>();

        item.put("id", AttributeValue.builder().s(id).build());
        item.put("fileName", AttributeValue.builder().s(fileName).build());
        item.put("contentType", AttributeValue.builder().s(contentType).build());
        item.put("fileSize", AttributeValue.builder().n(String.valueOf(size)).build());
        item.put("uploadTime", AttributeValue.builder().s(Instant.now().toString()).build());

        // Send it to the table
        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build();

        dynamoDbClient.putItem(request);
        logger.info("Metadata saved to DynamoDB for file ID: {}", id);
    }

    private boolean isValidFileType(String contentType) {
        return contentType != null && ALLOWED_TYPES.contains(contentType);
    }
}