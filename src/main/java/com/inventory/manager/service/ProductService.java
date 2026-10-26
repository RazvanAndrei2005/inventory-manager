package com.inventory.manager.service;

import com.inventory.manager.dto.CategoryResponseDto;
import com.inventory.manager.dto.ProductRequestDto;
import com.inventory.manager.dto.ProductResponseDto;
import com.inventory.manager.entity.Category;
import com.inventory.manager.entity.Product;
import com.inventory.manager.exception.DuplicateResourceException;
import com.inventory.manager.exception.ResourceNotFoundException;
import com.inventory.manager.repository.CategoryRepository;
import com.inventory.manager.repository.ProductRepository;
import com.inventory.manager.specification.ProductSpecification;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public ProductResponseDto createProduct(ProductRequestDto productRequestDto) {
        if (productRepository.existsBySku(productRequestDto.getSku())) {
            throw new DuplicateResourceException("Product with SKU '" + productRequestDto.getSku() + "' already exists");

        }
        Category category = categoryRepository.findById(productRequestDto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + productRequestDto.getCategoryId()));
        Product product = new Product();
        product.setSku(productRequestDto.getSku());
        product.setName(productRequestDto.getName());
        product.setPrice(productRequestDto.getPrice());
        product.setStockQuantity(productRequestDto.getStockQuantity());
        product.setCategory(category);
        Product savedProduct = productRepository.save(product);
        return mapToResponseDto(savedProduct);
    }

    public Page<ProductResponseDto> getAllProducts(int page,int size,String sortBy,String sortDir) {
       Sort sort=sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())?Sort.by(sortBy).ascending():Sort.by(sortBy).descending();
        Pageable pageable= PageRequest.of(page,size,sort);
        Page<Product>productPage=productRepository.findAll(pageable);
        return productPage.map(this::mapToResponseDto);

    }
    public Page<ProductResponseDto>getFilteredProducts(String name,String sku,Double minPrice,Double maxPrice,Long categoryId,Integer stockQuantity,Pageable pageable) {
    Specification<Product> spec= ProductSpecification.filterProduct(name,sku,minPrice,maxPrice,categoryId,stockQuantity);
    Page<Product>productPage=productRepository.findAll(spec,pageable);
    return productPage.map(this::mapToResponseDto);
    }
    public ProductResponseDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return mapToResponseDto(product);
    }
    public Page<ProductResponseDto> getProductsByName(String name,int page,int size,String sortBy,String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable= PageRequest.of(page,size,sort);
        Page<Product>productPage=productRepository.findByNameContainingIgnoreCase(name,pageable);
        return productPage.map(this::mapToResponseDto);
    }
    public Page<ProductResponseDto>filterByPrice(double priceMin,double priceMax,int page,int size,String sortBy,String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable= PageRequest.of(page,size,sort);
        Page<Product>productPage=productRepository.findByPriceBetween(priceMin,priceMax,pageable);
        return productPage.map(this::mapToResponseDto);
    }
    public List<ProductResponseDto> getLowStockProducts(Integer threshold) {
        return productRepository.findByStockQuantityLessThanEqual(threshold)
                .stream()
                .map(this::mapToResponseDto)
                .toList();
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
        Category category = categoryRepository.findById(productRequestDto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + productRequestDto.getCategoryId()));
        product.setName(productRequestDto.getName());
        product.setSku(productRequestDto.getSku());
        product.setPrice(productRequestDto.getPrice());
        product.setStockQuantity(productRequestDto.getStockQuantity());
        product.setCategory(category);
        Product updatedProduct = productRepository.save(product);
        return mapToResponseDto(updatedProduct);
    }

    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }
    public Page<ProductResponseDto>getProductByCategoryId(Long categoryId,Pageable pageable) {
        if(!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category not found with id: " + categoryId);
        }
        return productRepository.findByCategoryId(categoryId, pageable).map(this::mapToResponseDto);
    }
    private ProductResponseDto mapToResponseDto(Product product) {
        ProductResponseDto productResponseDto = new ProductResponseDto();
        productResponseDto.setId(product.getId());
        productResponseDto.setSku(product.getSku());
        productResponseDto.setName(product.getName());
        productResponseDto.setPrice(product.getPrice());
        productResponseDto.setStockQuantity(product.getStockQuantity());
        if (product.getCategory() != null) {
            CategoryResponseDto categoryDto = new CategoryResponseDto();
            categoryDto.setId(product.getCategory().getId());
            categoryDto.setName(product.getCategory().getName());
            categoryDto.setDescription(product.getCategory().getDescription());
            productResponseDto.setCategory(categoryDto);
        }
        return productResponseDto;
    }
}