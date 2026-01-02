package com.codebymathabo.s3entinel.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

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
    private final DynamoDbClient dynamoDbClient;
    private final String bucketName;
    private final String tableName = "S3entinel_Files_IaC";

    // Allowed MIME types
    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png", "application/pdf", "text/plain");

    // Constructor Injection: Spring Boot will auto-wire the clients from AwsConfig.java
    public FileServiceImpl(S3Client s3Client,
                           DynamoDbClient dynamoDbClient,
                           @Value("${aws.s3.bucket.name}") String bucketName) {
        this.s3Client = s3Client;
        this.dynamoDbClient = dynamoDbClient;
        this.bucketName = bucketName;
    }

    @Override
    @Async // Runs in a background thread
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

            // Used RequestBody.fromBytes since I am working with byte[] now
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