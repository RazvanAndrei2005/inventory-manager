package com.inventory.manager.service;

import com.inventory.manager.dto.StockAdjustmentRequestDto;
import com.inventory.manager.dto.StockMovementResponseDto;
import com.inventory.manager.entity.Product;
import com.inventory.manager.entity.StockMovement;
import com.inventory.manager.entity.StockMovementType;
import com.inventory.manager.entity.User;
import com.inventory.manager.exception.BadRequestException;
import com.inventory.manager.exception.ResourceNotFoundException;
import com.inventory.manager.repository.ProductRepository;
import com.inventory.manager.repository.StockMovementRepository;
import com.inventory.manager.repository.UserRepository;
import com.inventory.manager.security.CustomUserDetails;
import com.inventory.manager.specification.StockMovementSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class StockMovementService {

    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public StockMovementService(StockMovementRepository stockMovementRepository,
                                ProductRepository productRepository,
                                UserRepository userRepository) {
        this.stockMovementRepository = stockMovementRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public StockMovement recordMovement(Product product,
                                        StockMovementType type,
                                        int quantityChange,
                                        int quantityAfter,
                                        String reason,
                                        User createdBy) {
        if (quantityChange == 0) {
            throw new BadRequestException("Quantity change cannot be zero");
        }
        if (quantityAfter < 0) {
            throw new BadRequestException("Stock quantity cannot be negative");
        }
        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setType(type);
        movement.setQuantityChange(quantityChange);
        movement.setQuantityAfter(quantityAfter);
        movement.setReason(reason);
        movement.setCreatedBy(createdBy);
        return stockMovementRepository.save(movement);
    }

    @Transactional
    public StockMovement recordMovementForCurrentUser(Product product,
                                                      StockMovementType type,
                                                      int quantityChange,
                                                      int quantityAfter,
                                                      String reason) {
        User currentUser = resolveCurrentUser();
        return recordMovement(product, type, quantityChange, quantityAfter, reason, currentUser);
    }

    @Transactional
    public StockMovementResponseDto recordAdjustment(Long userId, StockAdjustmentRequestDto request) {
        if (request.getQuantityChange() == null || request.getQuantityChange() == 0) {
            throw new BadRequestException("Quantity change cannot be zero");
        }
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));
        int newStock = product.getStockQuantity() + request.getQuantityChange();
        if (newStock < 0) {
            throw new BadRequestException("Insufficient stock for adjustment: resulting quantity would be " + newStock);
        }
        product.setStockQuantity(newStock);
        productRepository.save(product);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        StockMovement movement = recordMovement(
                product,
                StockMovementType.ADJUSTMENT,
                request.getQuantityChange(),
                newStock,
                request.getReason(),
                user
        );
        return mapToDto(movement);
    }

    @Transactional(readOnly = true)
    public Page<StockMovementResponseDto> getMovements(Long productId,
                                                       StockMovementType type,
                                                       Long createdById,
                                                       Instant from,
                                                       Instant to,
                                                       Pageable pageable) {
        Specification<StockMovement> spec = StockMovementSpecification.filterMovements(
                productId, type, createdById, from, to);
        return stockMovementRepository.findAll(spec, pageable).map(this::mapToDto);
    }

    private User resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new IllegalStateException("No authenticated user found in security context");
        }
        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userDetails.getId()));
    }

    private StockMovementResponseDto mapToDto(StockMovement movement) {
        return new StockMovementResponseDto(
                movement.getId(),
                movement.getProduct().getId(),
                movement.getProduct().getName(),
                movement.getProduct().getSku(),
                movement.getType(),
                movement.getQuantityChange(),
                movement.getQuantityAfter(),
                movement.getReason(),
                movement.getCreatedBy().getId(),
                movement.getCreatedBy().getUsername(),
                movement.getCreatedAt()
        );
    }
}
