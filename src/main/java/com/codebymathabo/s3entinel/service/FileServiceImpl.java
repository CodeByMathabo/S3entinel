package com.codebymathabo.s3entinel.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);

    // Define the local storage path
    // We use "C:/S3entinel-Uploads" for Windows.
    // Note: Java handles forward slashes '/' correctly even on Windows.
    private final Path storagePath = Paths.get("C:/S3entinel-Uploads");

    public FileServiceImpl() {
        // Create the directory if it doesn't exist (Constructor logic)
        try {
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
                logger.info("Created local storage directory at: {}", storagePath);
            }
        } catch (IOException e) {
            logger.error("Could not initialize storage location", e);
            // If we can't create the folder, the service is broken. Fail fast.
            throw new RuntimeException("Could not initialize storage", e);
        }
    }

    @Override
    public String uploadFile(MultipartFile file) {
        logger.info("Starting file upload: {}", file.getOriginalFilename());

        if (!isValidFileType(file)) {
            logger.warn("Invalid file type attempted");
            throw new IllegalArgumentException("Only .txt files are allowed");
        }

        return saveFileLocally(file);
    }

    // Isolate the IO logic
    private String saveFileLocally(MultipartFile file) {
        try {
            // Generate a unique name to prevent overwriting files with the same name
            // e.g., "test.txt" becomes "uuid-test.txt"
            String uniqueFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

            // Resolve the full path: C:/S3entinel-Uploads/uuid-test.txt
            Path destinationPath = storagePath.resolve(uniqueFileName);

            // Copy the file stream to the destination
            Files.copy(file.getInputStream(), destinationPath, StandardCopyOption.REPLACE_EXISTING);

            logger.info("File saved locally at: {}", destinationPath);
            return "File stored locally with name: " + uniqueFileName;

        } catch (IOException e) {
            logger.error("Failed to store file locally", e);
            throw new RuntimeException("Failed to store file", e);
        }
    }

    private boolean isValidFileType(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.equals("text/plain");
    }
}