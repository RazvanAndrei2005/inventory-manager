package com.inventory.manager.controller;

import com.inventory.manager.dto.ProductRequestDto;
import com.inventory.manager.dto.ProductResponseDto;
import com.inventory.manager.service.ProductService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
    public ResponseEntity<Page<ProductResponseDto>> getAllProducts(@RequestParam(defaultValue="0")int page, @RequestParam(defaultValue = "10")int size, @RequestParam(defaultValue = "id")String sortBy, @RequestParam(defaultValue = "asc")String sortDir) {
        Page<ProductResponseDto> products=productService.getAllProducts(page,size,sortBy,sortDir);
        return ResponseEntity.status(HttpStatus.OK).body(products);
    }
    @GetMapping("/search")
    public ResponseEntity<Page<ProductResponseDto>> searchProductByName(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Page<ProductResponseDto> result = productService.getProductsByName(name, page, size, sortBy, sortDir);
        return ResponseEntity.ok(result);
    }
    @GetMapping("/filter")
    public ResponseEntity<Page<ProductResponseDto>> searchProductByPrice(@RequestParam double minValue,
                                                                         @RequestParam double maxValue,
                                                                         @RequestParam(defaultValue = "0") int page,
                                                                         @RequestParam(defaultValue = "10") int size,
                                                                         @RequestParam(defaultValue = "id") String sortBy,
                                                                         @RequestParam(defaultValue = "asc") String sortDir){
        Page<ProductResponseDto>result=productService.filterByPrice(minValue,maxValue,page,size,sortBy,sortDir);
        return ResponseEntity.ok(result);
    }
    @GetMapping("/low-stock")
    public ResponseEntity<List<ProductResponseDto>> getLowStockProducts(@RequestParam(defaultValue = "5")Integer threshold) {
        List<ProductResponseDto>list=productService.getLowStockProducts(threshold);
        return ResponseEntity.ok(list);
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
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<ProductResponseDto>> getProductsByCategory(@PathVariable Long categoryId, @ParameterObject  @PageableDefault(size=10,sort="name") Pageable pageable) {
        Page<ProductResponseDto>productResponseDtos=productService.getProductByCategoryId(categoryId,pageable);
        return  ResponseEntity.ok(productResponseDtos);
    }

}
