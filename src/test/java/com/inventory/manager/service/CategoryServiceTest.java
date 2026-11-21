package com.inventory.manager.service;

import com.inventory.manager.dto.CategoryRequestDto;
import com.inventory.manager.dto.CategoryResponseDto;
import com.inventory.manager.entity.Category;
import com.inventory.manager.exception.ConflictException;
import com.inventory.manager.exception.DuplicateResourceException;
import com.inventory.manager.exception.ResourceNotFoundException;
import com.inventory.manager.repository.CategoryRepository;
import com.inventory.manager.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void createCategory_returnsSavedDto_whenNameIsUnique() {
        CategoryRequestDto request = new CategoryRequestDto("Electronics", "Devices and gadgets");
        Category saved = new Category(1L, "Electronics", "Devices and gadgets");

        when(categoryRepository.existsByName("Electronics")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

        CategoryResponseDto result = categoryService.createCategory(request);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Electronics");
        assertThat(result.getDescription()).isEqualTo("Devices and gadgets");
    }

    @Test
    void createCategory_throwsDuplicateResourceException_whenNameExists() {
        CategoryRequestDto request = new CategoryRequestDto("Electronics", "Devices");
        when(categoryRepository.existsByName("Electronics")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Electronics");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void getAllCategories_returnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Category cat = new Category(1L, "Electronics", "Devices");
        when(categoryRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(cat), pageable, 1));

        Page<CategoryResponseDto> page = categoryService.getAllCategories(pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().getName()).isEqualTo("Electronics");
    }

    @Test
    void getCategoryById_returnsDto_whenFound() {
        Category cat = new Category(1L, "Electronics", "Devices");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));

        CategoryResponseDto result = categoryService.getCategoryById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Electronics");
    }

    @Test
    void getCategoryById_throwsResourceNotFoundException_whenMissing() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategoryById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateCategory_updatesFields_whenValid() {
        Category existing = new Category(1L, "Electronics", "Old desc");
        CategoryRequestDto request = new CategoryRequestDto("Gadgets", "New desc");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByName("Gadgets")).thenReturn(false);
        when(categoryRepository.save(existing)).thenReturn(existing);

        CategoryResponseDto result = categoryService.updateCategory(1L, request);

        assertThat(result.getName()).isEqualTo("Gadgets");
        assertThat(result.getDescription()).isEqualTo("New desc");
    }

    @Test
    void updateCategory_allowsSameNameWithoutDuplicateCheck() {
        Category existing = new Category(1L, "Electronics", "Old desc");
        CategoryRequestDto request = new CategoryRequestDto("Electronics", "Updated desc");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(existing)).thenReturn(existing);

        CategoryResponseDto result = categoryService.updateCategory(1L, request);

        assertThat(result.getName()).isEqualTo("Electronics");
        assertThat(result.getDescription()).isEqualTo("Updated desc");
        verify(categoryRepository, never()).existsByName(any());
    }

    @Test
    void updateCategory_throwsDuplicateResourceException_whenNewNameExists() {
        Category existing = new Category(1L, "Electronics", "Old desc");
        CategoryRequestDto request = new CategoryRequestDto("Books", "New desc");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByName("Books")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.updateCategory(1L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Books");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void deleteCategory_deletes_whenNoProductsAssigned() {
        Category existing = new Category(1L, "Electronics", "Devices");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.existsByCategoryId(1L)).thenReturn(false);

        categoryService.deleteCategory(1L);

        verify(categoryRepository).delete(existing);
    }

    @Test
    void deleteCategory_throwsConflictException_whenProductsAssigned() {
        Category existing = new Category(1L, "Electronics", "Devices");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.existsByCategoryId(1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.deleteCategory(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("associated products");

        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void deleteCategory_throwsResourceNotFoundException_whenMissing() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategory(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
