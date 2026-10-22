package com.inventory.manager.controller;

import com.inventory.manager.dto.ProductRequestDto;
import com.inventory.manager.dto.ProductResponseDto;
import com.inventory.manager.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;
    public ProductController(ProductService productService) {
        this.productService = productService;
    }
    @PostMapping
    public ResponseEntity<ProductResponseDto> createProduct(@Valid @RequestBody ProductRequestDto productRequestDto) {
        ProductResponseDto productResponseDto = productService.createProduct(productRequestDto);
       return ResponseEntity.status(HttpStatus.CREATED).body(productResponseDto);
    }
    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> getAllProducts() {
        List<ProductResponseDto> productResponseDtos = productService.getAllProducts();
        return ResponseEntity.status(HttpStatus.OK).body(productResponseDtos);
    }
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> getProductById(@PathVariable Long id) {
        ProductResponseDto productResponseDto=productService.getProductById(id);
        return ResponseEntity.status(HttpStatus.OK).body(productResponseDto);
    }
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDto> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequestDto productRequestDto) {
        ProductResponseDto productResponseDto = productService.updateProduct(id, productRequestDto);
        return ResponseEntity.status(HttpStatus.OK).body(productResponseDto);
    }
    @PatchMapping("/{id}/restock")
    public ResponseEntity<ProductResponseDto> restockProduct(@PathVariable Long id,@RequestParam Integer quantity) {
        ProductResponseDto productResponseDto = productService.restockProduct(id, quantity);
        return ResponseEntity.status(HttpStatus.OK).body(productResponseDto);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

}
