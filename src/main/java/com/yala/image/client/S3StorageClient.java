package com.yala.image.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class S3StorageClient {

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.region}")
    private String region;

    @Value("${aws.s3.public-url-base:}")
    private String publicUrlBase;

    private final S3Client s3Client;

    public String uploadFile(MultipartFile file) throws IOException {
        String key = "listings/" + UUID.randomUUID() + "_" + sanitize(file.getOriginalFilename());
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
        return publicUrl(key);
    }

    private String publicUrl(String key) {
        if (StringUtils.hasText(publicUrlBase)) {
            return publicUrlBase.replaceAll("/+$", "") + "/" + key;
        }
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }

    private String sanitize(String originalName) {
        String name = StringUtils.hasText(originalName) ? originalName : "image";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
