package com.codebymathabo.s3entinel.controller;

import com.codebymathabo.s3entinel.service.FileService;
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

    // Dependency Injection: The Controller now depends on the Service
    private final FileService fileService;

    // Constructor Injection (Best Practice for Testability)
    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        logger.info("Received upload request");

        // Fail fast: Check for empty file
        if (file.isEmpty()) {
            logger.warn("Upload failed: File is empty");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File cannot be empty");
        }

        return processFileSafely(file);
    }

    // Keeps the main endpoint method clean
    private ResponseEntity<String> processFileSafely(MultipartFile file) {
        try {
            // Delegate the heavy lifting to the Service Layer
            String result = fileService.uploadFile(file);

            logger.info("Controller: Request completed successfully");
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            // Catch validation errors (like wrong file type)
            logger.warn("Controller: Bad Request - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());

        } catch (Exception e) {
            // Catch unexpected errors
            logger.error("Controller: Internal Server Error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred processing the file");
        }
    }
}