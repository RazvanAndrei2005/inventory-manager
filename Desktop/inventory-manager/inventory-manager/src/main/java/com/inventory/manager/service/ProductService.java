package com.inventory.manager.service;

import com.inventory.manager.dto.ProductRequestDto;
import com.inventory.manager.dto.ProductResponseDto;
import com.inventory.manager.entity.Product;
import com.inventory.manager.exception.DuplicateResourceException;
import com.inventory.manager.exception.ResourceNotFoundException;
import com.inventory.manager.repository.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponseDto createProduct(ProductRequestDto productRequestDto) {
        if (productRepository.existsBySku(productRequestDto.getSku())) {
            throw new DuplicateResourceException("Product with SKU '" + productRequestDto.getSku() + "' already exists");
        }

        Product product = new Product();
        product.setSku(productRequestDto.getSku());
        product.setName(productRequestDto.getName());
        product.setPrice(productRequestDto.getPrice());
        product.setStockQuantity(productRequestDto.getStockQuantity());

        Product savedProduct = productRepository.save(product);
        return mapToResponseDto(savedProduct);
    }

    public Page<ProductResponseDto> getAllProducts(int page,int size,String sortBy,String sortDir) {
       Sort sort=sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())?Sort.by(sortBy).ascending():Sort.by(sortBy).descending();
        Pageable pageable= PageRequest.of(page,size,sort);
        Page<Product>productPage=productRepository.findAll(pageable);
        return productPage.map(this::mapToResponseDto);

    }

    public ProductResponseDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return mapToResponseDto(product);
    }

    public ProductResponseDto restockProduct(Long id, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity to add must be greater than zero!");
        }

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        product.setStockQuantity(product.getStockQuantity() + quantity);
        productRepository.save(product);
        return mapToResponseDto(product);
    }

    public ProductResponseDto updateProduct(Long id, ProductRequestDto productRequestDto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (!product.getSku().equals(productRequestDto.getSku())
                && productRepository.existsBySku(productRequestDto.getSku())) {
            throw new DuplicateResourceException("SKU '" + productRequestDto.getSku() + "' is already taken by another product!");
        }

        product.setName(productRequestDto.getName());
        product.setSku(productRequestDto.getSku());
        product.setPrice(productRequestDto.getPrice());
        product.setStockQuantity(productRequestDto.getStockQuantity());

        Product updatedProduct = productRepository.save(product);
        return mapToResponseDto(updatedProduct);
    }

    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    private ProductResponseDto mapToResponseDto(Product product) {
        ProductResponseDto productResponseDto = new ProductResponseDto();
        productResponseDto.setId(product.getId());
        productResponseDto.setSku(product.getSku());
        productResponseDto.setName(product.getName());
        productResponseDto.setPrice(product.getPrice());
        productResponseDto.setStockQuantity(product.getStockQuantity());
        return productResponseDto;
    }
}