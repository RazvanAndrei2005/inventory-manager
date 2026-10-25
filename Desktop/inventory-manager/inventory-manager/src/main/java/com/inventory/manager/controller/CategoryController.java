package com.inventory.manager.controller;

import com.inventory.manager.dto.CategoryRequestDto;
import com.inventory.manager.dto.CategoryResponseDto;
import com.inventory.manager.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private CategoryService categoryService;
    public CategoryController(CategoryService categoryService) {this.categoryService = categoryService;}
    @PostMapping
    public ResponseEntity<CategoryResponseDto>createCategory(@Valid @RequestBody CategoryRequestDto categoryRequestDto) {
        CategoryResponseDto categoryResponseDto = categoryService.createCategory(categoryRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryResponseDto);
    }
    @GetMapping
    public ResponseEntity<List<CategoryResponseDto>> getAllCategories() {
        List<CategoryResponseDto> categoryResponseDtoList = categoryService.getAllCategories();
        return ResponseEntity.ok(categoryResponseDtoList);
    }
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponseDto> getCategoryById(@PathVariable Long id) {
        CategoryResponseDto categoryResponseDto = categoryService.getCategoryById(id);
        return ResponseEntity.ok(categoryResponseDto);
    }
}
