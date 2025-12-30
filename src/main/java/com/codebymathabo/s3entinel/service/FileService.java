package com.codebymathabo.s3entinel.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.CompletableFuture;

public interface FileService {
    // Pass the raw bytes, filename, and content type manually
    CompletableFuture<Object> uploadFile(byte[] fileData, String fileName, String contentType);
}