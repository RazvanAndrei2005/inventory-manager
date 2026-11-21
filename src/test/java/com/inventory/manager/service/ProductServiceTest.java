package com.inventory.manager.service;

import com.inventory.manager.dto.ProductRequestDto;
import com.inventory.manager.dto.ProductResponseDto;
import com.inventory.manager.entity.Category;
import com.inventory.manager.entity.Product;
import com.inventory.manager.exception.BadRequestException;
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
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    private Category sampleCategory() {
        return new Category(1L, "Electronics", "Devices");
    }

    private Product sampleProduct(Category category) {
        return new Product(10L, "Laptop", "SKU-001", new BigDecimal("999.99"), 15, 0L, category);
    }

    @Test
    void createProduct_returnsSavedDto_whenValid() {
        Category category = sampleCategory();
        ProductRequestDto request = new ProductRequestDto("Laptop", "SKU-001", new BigDecimal("999.99"), 15, 1L);
        Product saved = sampleProduct(category);

        when(productRepository.existsBySku("SKU-001")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(saved);

        ProductResponseDto result = productService.createProduct(request);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getSku()).isEqualTo("SKU-001");
        assertThat(result.getPrice()).isEqualByComparingTo("999.99");
        assertThat(result.getStockQuantity()).isEqualTo(15);
        assertThat(result.getCategory().getId()).isEqualTo(1L);
    }

    @Test
    void createProduct_throwsDuplicateResourceException_whenSkuExists() {
        ProductRequestDto request = new ProductRequestDto("Laptop", "SKU-001", new BigDecimal("999.99"), 15, 1L);
        when(productRepository.existsBySku("SKU-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("SKU-001");

        verify(productRepository, never()).save(any());
    }

    @Test
    void createProduct_throwsResourceNotFoundException_whenCategoryMissing() {
        ProductRequestDto request = new ProductRequestDto("Laptop", "SKU-001", new BigDecimal("999.99"), 15, 99L);
        when(productRepository.existsBySku("SKU-001")).thenReturn(false);
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getFilteredProducts_returnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Product product = sampleProduct(sampleCategory());
        when(productRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(product), pageable, 1));

        Page<ProductResponseDto> result = productService.getFilteredProducts(
                "Laptop", null, null, null, null, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getName()).isEqualTo("Laptop");
    }

    @Test
    void getProductById_returnsDto_whenFound() {
        Product product = sampleProduct(sampleCategory());
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        ProductResponseDto result = productService.getProductById(10L);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getName()).isEqualTo("Laptop");
    }

    @Test
    void getProductById_throwsResourceNotFoundException_whenMissing() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getLowStockProducts_returnsMappedList() {
        Product low = new Product(11L, "Cable", "SKU-002", new BigDecimal("9.99"), 2, 0L, sampleCategory());
        when(productRepository.findByStockQuantityLessThanEqual(5)).thenReturn(List.of(low));

        List<ProductResponseDto> result = productService.getLowStockProducts(5);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getStockQuantity()).isEqualTo(2);
    }

    @Test
    void restockProduct_addsQuantity_whenValid() {
        Product product = sampleProduct(sampleCategory());
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        ProductResponseDto result = productService.restockProduct(10L, 5);

        assertThat(result.getStockQuantity()).isEqualTo(20);
        verify(productRepository).save(product);
    }

    @Test
    void restockProduct_throwsBadRequestException_whenQuantityNonPositiveOrNull() {
        assertThatThrownBy(() -> productService.restockProduct(10L, 0))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> productService.restockProduct(10L, -3))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> productService.restockProduct(10L, null))
                .isInstanceOf(BadRequestException.class);

        verify(productRepository, never()).findById(any());
    }

    @Test
    void updateProduct_updatesFields_whenValid() {
        Category category = sampleCategory();
        Product existing = sampleProduct(category);
        ProductRequestDto request = new ProductRequestDto("Laptop Pro", "SKU-001", new BigDecimal("1299.00"), 20, 1L);

        when(productRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(existing)).thenReturn(existing);

        ProductResponseDto result = productService.updateProduct(10L, request);

        assertThat(result.getName()).isEqualTo("Laptop Pro");
        assertThat(result.getPrice()).isEqualByComparingTo("1299.00");
        assertThat(result.getStockQuantity()).isEqualTo(20);
    }

    @Test
    void updateProduct_throwsDuplicateResourceException_whenNewSkuExists() {
        Product existing = sampleProduct(sampleCategory());
        ProductRequestDto request = new ProductRequestDto("Laptop", "SKU-TAKEN", new BigDecimal("999.99"), 15, 1L);

        when(productRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(productRepository.existsBySku("SKU-TAKEN")).thenReturn(true);

        assertThatThrownBy(() -> productService.updateProduct(10L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("SKU-TAKEN");

        verify(productRepository, never()).save(any());
    }

    @Test
    void deleteProduct_deletes_whenExists() {
        when(productRepository.existsById(10L)).thenReturn(true);

        productService.deleteProduct(10L);

        verify(productRepository).deleteById(10L);
    }

    @Test
    void deleteProduct_throwsResourceNotFoundException_whenMissing() {
        when(productRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> productService.deleteProduct(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productRepository, never()).deleteById(any());
    }

    @Test
    void getProductByCategoryId_returnsPage_whenCategoryExists() {
        Pageable pageable = PageRequest.of(0, 10);
        Product product = sampleProduct(sampleCategory());
        when(categoryRepository.existsById(1L)).thenReturn(true);
        when(productRepository.findByCategoryId(1L, pageable)).thenReturn(new PageImpl<>(List.of(product), pageable, 1));

        Page<ProductResponseDto> result = productService.getProductByCategoryId(1L, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getId()).isEqualTo(10L);
    }

    @Test
    void getProductByCategoryId_throwsResourceNotFoundException_whenCategoryMissing() {
        Pageable pageable = PageRequest.of(0, 10);
        when(categoryRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> productService.getProductByCategoryId(99L, pageable))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
