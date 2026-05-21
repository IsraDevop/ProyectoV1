package com.yala.category.service;

import com.yala.category.dto.CategoryResponse;
import com.yala.category.dto.CreateCategoryRequest;

import java.util.List;

public interface CategoryService {
    List<CategoryResponse> getAllCategories();
    CategoryResponse createCategory(CreateCategoryRequest request);
}
