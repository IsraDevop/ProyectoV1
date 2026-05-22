package com.yala.image.controller;

import com.yala.image.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
@Tag(name = "Images", description = "Image upload to Amazon S3")
public class ImageController {

    private final ImageService imageService;

    @PostMapping("/upload")
    @Operation(summary = "Upload image for a listing (max 5 per listing, 5MB, JPG/PNG/WEBP)")
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam Long listingId,
            @RequestParam("file") MultipartFile file) {
        String url = imageService.uploadImage(listingId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("url", url));
    }
}
