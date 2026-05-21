package com.yala.image.service;

import org.springframework.web.multipart.MultipartFile;

public interface ImageService {
    String uploadImage(Long listingId, MultipartFile file);
}
