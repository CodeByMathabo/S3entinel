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
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class FileServiceImpl implements FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);

    private final S3Client s3Client;
    private final String bucketName;

    // Allowed MIME types
    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png", "application/pdf", "text/plain");

    public FileServiceImpl(@Value("${aws.access.key.id}") String accessKey,
                           @Value("${aws.secret.access.key}") String secretKey,
                           @Value("${aws.region}") String region,
                           @Value("${aws.s3.bucket.name}") String bucketName) {

        this.bucketName = bucketName;
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

    @Override
    @Async // Background thread
    public CompletableFuture<Object> uploadFile(byte[] fileData, String fileName, String contentType) {
        logger.info("Service Layer: Uploading to AWS S3 in thread: {}", Thread.currentThread().getName());

        if (!isValidFileType(contentType)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid file type."));
        }

        try {
            String uniqueFileName = UUID.randomUUID() + "_" + fileName;

            PutObjectRequest putOb = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(uniqueFileName)
                    .contentType(contentType)
                    .build();

            // I used RequestBody.fromBytes because the raw data is available now
            s3Client.putObject(putOb, RequestBody.fromBytes(fileData));

            logger.info("File successfully uploaded to S3: {}", uniqueFileName);
            return CompletableFuture.completedFuture("File uploaded to S3: " + uniqueFileName);

        } catch (S3Exception e) {
            logger.error("AWS S3 Upload failed", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private boolean isValidFileType(String contentType) {
        return contentType != null && ALLOWED_TYPES.contains(contentType);
    }
}