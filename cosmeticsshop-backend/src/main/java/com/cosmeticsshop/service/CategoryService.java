package com.cosmeticsshop.service;

import com.cosmeticsshop.exception.ResourceNotFoundException;
import com.cosmeticsshop.model.Category;
import com.cosmeticsshop.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }

    public Category save(Category category) {
        return categoryRepository.save(category);
    }

    public Category updateCategory(Long id, Category category) {
        Category existing = getCategoryById(id);
        existing.setName(category.getName());
        existing.setDescription(category.getDescription());
        existing.setParentCategoryId(category.getParentCategoryId());
        return categoryRepository.save(existing);
    }

    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }
}
