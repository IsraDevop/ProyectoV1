package com.yala.image.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class SupabaseStorageClient {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    @Value("${supabase.bucket:collectibles}")
    private String bucket;

    private final RestTemplate restTemplate = new RestTemplate();

    public String uploadFile(MultipartFile file) throws IOException {
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + fileName;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + supabaseKey);
        headers.setContentType(MediaType.parseMediaType(
                file.getContentType() != null ? file.getContentType() : "application/octet-stream"));

        HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);
        ResponseEntity<String> response = restTemplate.exchange(uploadUrl, HttpMethod.POST, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Supabase upload failed: " + response.getStatusCode());
        }

        return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + fileName;
    }
}
