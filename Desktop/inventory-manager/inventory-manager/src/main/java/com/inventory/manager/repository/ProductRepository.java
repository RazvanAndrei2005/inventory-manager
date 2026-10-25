package com.inventory.manager.repository;

import com.inventory.manager.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product,Long> {
    boolean existsBySku(String sku);
    Page<Product> findByNameContainingIgnoreCase(String search, Pageable pageable);
    Page<Product>findByPriceBetween(double minPrice,double maxPrice ,Pageable pageable);
    List<Product> findByStockQuantityLessThanEqual(Integer threshold);
    Page<Product>findByCategoryId(Long categoryID, Pageable pageable);
}


