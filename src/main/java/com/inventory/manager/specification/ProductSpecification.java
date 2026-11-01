package com.inventory.manager.specification;

import com.inventory.manager.entity.Product;
import org.springframework.data.jpa.domain.Specification;

public class ProductSpecification {

    public static Specification<Product> hasName(String name) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Product> hasSku(String sku) {
        return (root, query, cb) ->
                cb.equal(root.get("sku"), sku);
    }

    public static Specification<Product> hasMinPrice(Double minPrice) {
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Product> hasMaxPrice(Double maxPrice) {
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    public static Specification<Product> hasCategoryId(Long categoryId) {
        return (root, query, cb) ->
                cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> hasMinStock(Integer minStock) {
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("stockQuantity"), minStock);
    }

    public static Specification<Product> hasMaxStock(Integer maxStock) {
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("stockQuantity"), maxStock);
    }

    public static Specification<Product> filterProduct(
            String name,
            String sku,
            Double minPrice,
            Double maxPrice,
            Long categoryId,
            Integer minStock,
            Integer maxStock) {
        Specification<Product> spec = (root, query, cb) -> cb.conjunction();
        if (name != null && !name.isBlank()) {
            spec = spec.and(hasName(name));
        }
        if (sku != null && !sku.isBlank()) {
            spec = spec.and(hasSku(sku));
        }
        if (minPrice != null) {
            spec = spec.and(hasMinPrice(minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and(hasMaxPrice(maxPrice));
        }
        if (categoryId != null) {
            spec = spec.and(hasCategoryId(categoryId));
        }
        if (minStock != null) {
            spec = spec.and(hasMinStock(minStock));
        }
        if (maxStock != null) {
            spec = spec.and(hasMaxStock(maxStock));
        }
        return spec;
    }
}
