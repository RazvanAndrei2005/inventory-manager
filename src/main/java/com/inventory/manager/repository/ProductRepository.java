package com.inventory.manager.repository;

import com.inventory.manager.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.domain.Specification;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    boolean existsBySku(String sku);
    boolean existsByCategoryId(Long categoryId);

    @EntityGraph(attributePaths = {"category"})
    Optional<Product> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"category"})
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    List<Product> findByStockQuantityLessThanEqual(Integer threshold);

    @EntityGraph(attributePaths = {"category"})
    Page<Product> findByCategoryId(Long categoryID, Pageable pageable);
}
