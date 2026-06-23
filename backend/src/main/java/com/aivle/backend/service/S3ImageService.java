package com.aivle.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3ImageService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.region}")
    private String region;

    public String uploadBase64Image(String coverImageUrl) {
        if (coverImageUrl == null || coverImageUrl.isBlank()) {
            return coverImageUrl;
        }

        if (coverImageUrl.equals("/noImage.jpg")) {
            return coverImageUrl;
        }

        // 이미 URL이면 다시 업로드하지 않음
        if (!coverImageUrl.startsWith("data:image/")) {
            return coverImageUrl;
        }

        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("AWS_S3_BUCKET 환경변수가 설정되지 않았습니다.");
        }

        String base64Data = coverImageUrl.contains(",")
                ? coverImageUrl.substring(coverImageUrl.indexOf(",") + 1)
                : coverImageUrl;

        byte[] imageBytes = Base64.getDecoder().decode(base64Data);

        String key = "book-covers/" + UUID.randomUUID() + ".png";

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType("image/png")
                .contentLength((long) imageBytes.length)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(imageBytes));

        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }
}