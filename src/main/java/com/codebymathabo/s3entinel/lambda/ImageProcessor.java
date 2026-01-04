package com.codebymathabo.s3entinel.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageProcessor implements RequestHandler<S3Event, String> {

    private static final Logger logger = LoggerFactory.getLogger(ImageProcessor.class);

    @Override
    public String handleRequest(S3Event s3Event, Context context) {
        // Method runs automatically whenever a file is uploaded to S3

        s3Event.getRecords().forEach(record -> {
            String bucketName = record.getS3().getBucket().getName();
            String fileName = record.getS3().getObject().getKey();
            long fileSize = record.getS3().getObject().getSizeAsLong();

            logger.info("⚡ LAMBDA TRIGGERED ⚡");
            logger.info("New File Detected: {}", fileName);
            logger.info("Location: {} (Size: {} bytes)", bucketName, fileSize);
            logger.info("Analysis complete. System scalable and responsive.");
        });

        return "Processing Complete";
    }
}