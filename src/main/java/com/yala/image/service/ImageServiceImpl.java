package com.yala.image.service;

import com.yala.exception.ImageLimitExceededException;
import com.yala.exception.InvalidOperationException;
import com.yala.exception.ResourceNotFoundException;
import com.yala.image.client.SupabaseStorageClient;
import com.yala.image.model.Image;
import com.yala.image.repository.ImageRepository;
import com.yala.listing.model.Listing;
import com.yala.listing.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png", "image/webp");
    private static final int MAX_IMAGES = 5;

    private final ImageRepository imageRepository;
    private final ListingRepository listingRepository;
    private final SupabaseStorageClient storageClient;

    @Override
    @Transactional
    public String uploadImage(Long listingId, MultipartFile file) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found: " + listingId));

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidOperationException("File size exceeds 5MB limit");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new InvalidOperationException("Only JPG, PNG, and WEBP images are allowed");
        }

        long currentCount = imageRepository.countByListingId(listingId);
        if (currentCount >= MAX_IMAGES) {
            throw new ImageLimitExceededException("Maximum 5 images per listing");
        }

        try {
            String url = storageClient.uploadFile(file);
            Image image = Image.builder()
                    .url(url)
                    .sortOrder((int) currentCount + 1)
                    .listing(listing)
                    .build();
            imageRepository.save(image);
            return url;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload image: " + e.getMessage(), e);
        }
    }
}
