package com.menumind.backend.service;

import com.menumind.backend.dto.menu.CategoryRequest;
import com.menumind.backend.dto.menu.CategoryResponse;

import java.util.List;

/**
 * Category Service Contract
 */
public interface CategoryService {

    List<CategoryResponse> getAllCategories();

    List<CategoryResponse> getActiveCategories();

    CategoryResponse getCategoryById(Long id);

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse updateCategory(Long id, CategoryRequest request);

    void deleteCategory(Long id);
}
