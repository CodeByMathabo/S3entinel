package com.codebymathabo.s3entinel.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        // Log entry to track request flow
        logger.info("Received upload request");

        // Fail first: Handle empty file case immediately to avoid nesting success logic
        if (file.isEmpty()) {
            logger.warn("Upload failed: File is empty");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File cannot be empty");
        }

        // Delegate complex logic to a separate method to keep this controller method flat and readable
        return processFileSafely(file);
    }

    private ResponseEntity<String> processFileSafely(MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();
            logger.info("Processing file: {}", fileName);

            // TODO: Service layer integration will happen in Phase 3

            logger.info("File processed successfully: {}", fileName);
            return ResponseEntity.ok("File received: " + fileName);

        } catch (Exception e) {
            // Catching generic Exception ensures the API never crashes unexpectedly
            logger.error("Error processing file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred processing the file");
        }
    }
}