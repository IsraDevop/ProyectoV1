package com.yala.category.service;

import com.yala.category.dto.CategoryResponse;
import com.yala.category.dto.CreateCategoryRequest;
import com.yala.category.model.Category;
import com.yala.category.repository.CategoryRepository;
import com.yala.exception.DuplicateResourceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream().map(CategoryResponse::from).toList();
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Category already exists: " + request.name());
        }
        Category category = Category.builder()
                .name(request.name())
                .description(request.description())
                .build();
        return CategoryResponse.from(categoryRepository.save(category));
    }
}
