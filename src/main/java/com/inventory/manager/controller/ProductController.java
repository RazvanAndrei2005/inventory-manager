package com.inventory.manager.controller;

import com.inventory.manager.dto.ProductRequestDto;
import com.inventory.manager.dto.ProductResponseDto;
import com.inventory.manager.service.ProductService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ProductResponseDto> createProduct(@Valid @RequestBody ProductRequestDto productRequestDto) {
        ProductResponseDto productResponseDto = productService.createProduct(productRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(productResponseDto);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<Page<ProductResponseDto>> getAllProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer minStock,
            @RequestParam(required = false) Integer maxStock,
            @ParameterObject @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<ProductResponseDto> products = productService.getFilteredProducts(name, sku, minPrice, maxPrice, categoryId, minStock, maxStock, pageable);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<List<ProductResponseDto>> getLowStockProducts(@RequestParam(defaultValue = "5") Integer threshold) {
        List<ProductResponseDto> list = productService.getLowStockProducts(threshold);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ProductResponseDto> getProductById(@PathVariable Long id) {
        ProductResponseDto productResponseDto = productService.getProductById(id);
        return ResponseEntity.ok(productResponseDto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ProductResponseDto> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequestDto productRequestDto) {
        ProductResponseDto productResponseDto = productService.updateProduct(id, productRequestDto);
        return ResponseEntity.ok(productResponseDto);
    }

    @PatchMapping("/{id}/restock")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ProductResponseDto> restockProduct(@PathVariable Long id, @RequestParam Integer quantity) {
        ProductResponseDto productResponseDto = productService.restockProduct(id, quantity);
        return ResponseEntity.ok(productResponseDto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/category/{categoryId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<Page<ProductResponseDto>> getProductsByCategory(
            @PathVariable Long categoryId,
            @ParameterObject @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        Page<ProductResponseDto> productResponseDtos = productService.getProductByCategoryId(categoryId, pageable);
        return ResponseEntity.ok(productResponseDtos);
    }
}
