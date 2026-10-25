package com.inventory.manager.service;

import com.inventory.manager.dto.CategoryRequestDto;
import com.inventory.manager.dto.CategoryResponseDto;
import com.inventory.manager.entity.Category;
import com.inventory.manager.exception.DuplicateResourceException;
import com.inventory.manager.exception.ResourceNotFoundException;
import com.inventory.manager.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public CategoryResponseDto createCategory(CategoryRequestDto categoryRequestDto) {
        if (categoryRepository.existsByName(categoryRequestDto.getName())) {throw new DuplicateResourceException("Category with name " + categoryRequestDto.getName() + " already exists");
        }
        Category category = new Category();
        category.setName(categoryRequestDto.getName());
        category.setDescription(categoryRequestDto.getDescription());
        Category savedCategory = categoryRepository.save(category);
        return mapToCategoryDto(savedCategory);
    }

    public List<CategoryResponseDto> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(this::mapToCategoryDto)
                .toList();
    }

    public CategoryResponseDto getCategoryById(Long id) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return mapToCategoryDto(category);
    }

    public CategoryResponseDto mapToCategoryDto(Category category) {
        CategoryResponseDto categoryResponseDto = new CategoryResponseDto();
        categoryResponseDto.setId(category.getId());
        categoryResponseDto.setName(category.getName());
        categoryResponseDto.setDescription(category.getDescription());
        return categoryResponseDto;
    }
}