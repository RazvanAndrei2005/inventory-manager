package com.inventory.manager.specification;

import com.inventory.manager.entity.StockMovement;
import com.inventory.manager.entity.StockMovementType;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public class StockMovementSpecification {

    public static Specification<StockMovement> hasProductId(Long productId) {
        return (root, query, cb) -> cb.equal(root.get("product").get("id"), productId);
    }

    public static Specification<StockMovement> hasType(StockMovementType type) {
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    public static Specification<StockMovement> hasCreatedById(Long createdById) {
        return (root, query, cb) -> cb.equal(root.get("createdBy").get("id"), createdById);
    }

    public static Specification<StockMovement> createdFrom(Instant from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<StockMovement> createdTo(Instant to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }

    public static Specification<StockMovement> filterMovements(
            Long productId,
            StockMovementType type,
            Long createdById,
            Instant from,
            Instant to) {
        Specification<StockMovement> spec = (root, query, cb) -> cb.conjunction();
        if (productId != null) {
            spec = spec.and(hasProductId(productId));
        }
        if (type != null) {
            spec = spec.and(hasType(type));
        }
        if (createdById != null) {
            spec = spec.and(hasCreatedById(createdById));
        }
        if (from != null) {
            spec = spec.and(createdFrom(from));
        }
        if (to != null) {
            spec = spec.and(createdTo(to));
        }
        return spec;
    }
}
