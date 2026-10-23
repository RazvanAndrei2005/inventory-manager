package com.inventory.manager.service;

import com.inventory.manager.dto.ProductRequestDto;
import com.inventory.manager.dto.ProductResponseDto;
import com.inventory.manager.entity.Product;
import com.inventory.manager.repository.ProductRepository;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
    public ProductResponseDto createProduct(ProductRequestDto productRequestDto) {
      if (productRepository.existsBySku(productRequestDto.getSku())) {
          throw new RuntimeException("Sku already exists " + productRequestDto.getSku());
      }
    Product product = new Product();
    product.setSku(productRequestDto.getSku());
    product.setName(productRequestDto.getName());
    product.setPrice(productRequestDto.getPrice());
    product.setStockQuantity(productRequestDto.getStockQuantity());
    Product savedProduct = productRepository.save(product);
    ProductResponseDto productResponseDto = new ProductResponseDto();
    productResponseDto.setId(savedProduct.getId());
    productResponseDto.setStockQuantity(savedProduct.getStockQuantity());
    productResponseDto.setSku(savedProduct.getSku());
    productResponseDto.setName(savedProduct.getName());
    productResponseDto.setPrice(savedProduct.getPrice());
    return productResponseDto;
    }

    public List<ProductResponseDto> getAllProducts(){
        List<Product> productList=productRepository.findAll();
        List<ProductResponseDto> productResponseDtoList=new ArrayList<>();
        for (Product product :productList){
        ProductResponseDto productResponseDto=mapToResponseDto(product);
        productResponseDtoList.add(productResponseDto);
            }
        return productResponseDtoList;
    }
    public ProductResponseDto getProductById(Long id) {
        Product product=productRepository.findById(id).orElseThrow(()->new RuntimeException("Product not found"));
        return mapToResponseDto(product);
    }
    public ProductResponseDto restockProduct(Long id,Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Quantity to add must be greater than zero!");
        }
        Product product=productRepository.findById(id).orElseThrow(()->new RuntimeException("Product not found"));
        product.setStockQuantity(product.getStockQuantity() + quantity);
        productRepository.save(product);
        return mapToResponseDto(product);
    }
    public ProductResponseDto updateProduct(Long id, ProductRequestDto productRequestDto) {
        Product product=productRepository.findById(id).orElseThrow(()->new RuntimeException("Product not found"));
        if (!product.getSku().equals(productRequestDto.getSku())
                && productRepository.existsBySku(productRequestDto.getSku())) {
            throw new RuntimeException("SKU " + productRequestDto.getSku() + " is already taken by another product!");
        }
        product.setName(productRequestDto.getName());
        product.setSku(productRequestDto.getSku());
        product.setPrice(productRequestDto.getPrice());
        productRepository.save(product);
        return mapToResponseDto(product);
    }
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found with id: " + id);
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
