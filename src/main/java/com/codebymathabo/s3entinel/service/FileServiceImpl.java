package com.codebymathabo.s3entinel.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileServiceImpl implements FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);

    @Override
    public String uploadFile(MultipartFile file) {
        logger.info("Service Layer: Starting upload process for {}", file.getOriginalFilename());

        // Fail fast: Validate file type immediately
        if (!isValidFileType(file)) {
            logger.warn("Service Layer: Invalid file type detected");
            throw new IllegalArgumentException("Only .txt files are allowed for now");
        }

        // TODO: In Phase 4, we will add actual AWS S3 logic here.
        // For now, we simulate the processing.
        logger.info("Service Layer: Simulating file upload to cloud storage...");

        return "File successfully processed: " + file.getOriginalFilename();
    }

    // Helper method to keep the main logic flat and readable
    private boolean isValidFileType(MultipartFile file) {
        String contentType = file.getContentType();
        // Check if content type is present and is text/plain
        return contentType != null && contentType.equals("text/plain");
    }
}