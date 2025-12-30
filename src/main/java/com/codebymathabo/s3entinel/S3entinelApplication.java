package com.codebymathabo.s3entinel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync 
public class S3entinelApplication {

    public static void main(String[] args) {

        SpringApplication.run(S3entinelApplication.class, args);
    }
}