package com.inventory.manager.service;

import com.inventory.manager.dto.StockAdjustmentRequestDto;
import com.inventory.manager.dto.StockMovementResponseDto;
import com.inventory.manager.entity.Category;
import com.inventory.manager.entity.Product;
import com.inventory.manager.entity.Role;
import com.inventory.manager.entity.StockMovement;
import com.inventory.manager.entity.StockMovementType;
import com.inventory.manager.entity.User;
import com.inventory.manager.exception.BadRequestException;
import com.inventory.manager.exception.ResourceNotFoundException;
import com.inventory.manager.repository.ProductRepository;
import com.inventory.manager.repository.StockMovementRepository;
import com.inventory.manager.repository.UserRepository;
import com.inventory.manager.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockMovementServiceTest {

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StockMovementService stockMovementService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private User sampleUser() {
        return new User(1L, "admin", "admin@example.com", "pw", Role.ROLE_ADMIN, true);
    }

    private Product sampleProduct(int stock) {
        Category category = new Category(1L, "Electronics", "Devices");
        return new Product(10L, "Laptop", "SKU-001", new BigDecimal("999.99"), stock, 0L, category);
    }

    @Test
    void recordMovement_savesMovement_whenValid() {
        Product product = sampleProduct(15);
        User user = sampleUser();
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        StockMovement result = stockMovementService.recordMovement(
                product, StockMovementType.RESTOCK, 5, 15, "Supplier delivery", user);

        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(captor.capture());
        StockMovement saved = captor.getValue();
        assertThat(saved.getProduct()).isSameAs(product);
        assertThat(saved.getType()).isEqualTo(StockMovementType.RESTOCK);
        assertThat(saved.getQuantityChange()).isEqualTo(5);
        assertThat(saved.getQuantityAfter()).isEqualTo(15);
        assertThat(saved.getReason()).isEqualTo("Supplier delivery");
        assertThat(saved.getCreatedBy()).isSameAs(user);
        assertThat(result).isSameAs(saved);
    }

    @Test
    void recordMovement_throwsBadRequestException_whenQuantityChangeIsZero() {
        Product product = sampleProduct(10);
        User user = sampleUser();

        assertThatThrownBy(() -> stockMovementService.recordMovement(
                product, StockMovementType.ADJUSTMENT, 0, 10, "No-op", user))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("zero");

        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void recordMovement_throwsBadRequestException_whenQuantityAfterIsNegative() {
        Product product = sampleProduct(5);
        User user = sampleUser();

        assertThatThrownBy(() -> stockMovementService.recordMovement(
                product, StockMovementType.ADJUSTMENT, -6, -1, "Too low", user))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("negative");

        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void recordMovementForCurrentUser_resolvesUserFromSecurityContext() {
        User user = sampleUser();
        CustomUserDetails details = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));

        Product product = sampleProduct(10);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        StockMovement result = stockMovementService.recordMovementForCurrentUser(
                product, StockMovementType.INITIAL_STOCK, 10, 10, "Initial stock");

        assertThat(result.getCreatedBy()).isSameAs(user);
        assertThat(result.getType()).isEqualTo(StockMovementType.INITIAL_STOCK);
    }

    @Test
    void recordAdjustment_updatesProductStockAndSavesMovement() {
        Product product = sampleProduct(10);
        User user = sampleUser();
        StockAdjustmentRequestDto request = new StockAdjustmentRequestDto(10L, -3, "Damaged in transit");

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(inv -> {
            StockMovement m = inv.getArgument(0);
            m.setId(50L);
            m.setCreatedAt(Instant.now());
            return m;
        });

        StockMovementResponseDto response = stockMovementService.recordAdjustment(1L, request);

        assertThat(product.getStockQuantity()).isEqualTo(7);
        verify(productRepository).save(product);
        assertThat(response.getId()).isEqualTo(50L);
        assertThat(response.getType()).isEqualTo(StockMovementType.ADJUSTMENT);
        assertThat(response.getQuantityChange()).isEqualTo(-3);
        assertThat(response.getQuantityAfter()).isEqualTo(7);
        assertThat(response.getReason()).isEqualTo("Damaged in transit");
        assertThat(response.getCreatedByUsername()).isEqualTo("admin");
    }

    @Test
    void recordAdjustment_throwsBadRequestException_whenResultingStockIsNegative() {
        Product product = sampleProduct(2);
        StockAdjustmentRequestDto request = new StockAdjustmentRequestDto(10L, -5, "Correction");
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> stockMovementService.recordAdjustment(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient stock");

        verify(productRepository, never()).save(any());
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void recordAdjustment_throwsResourceNotFoundException_whenProductMissing() {
        StockAdjustmentRequestDto request = new StockAdjustmentRequestDto(99L, 5, "Correction");
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockMovementService.recordAdjustment(1L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getMovements_returnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Product product = sampleProduct(15);
        User user = sampleUser();
        StockMovement movement = new StockMovement(
                100L, product, StockMovementType.RESTOCK, 5, 15, "Restock", user, Instant.now());

        when(stockMovementRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(movement), pageable, 1));

        Page<StockMovementResponseDto> result = stockMovementService.getMovements(
                10L, StockMovementType.RESTOCK, null, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        StockMovementResponseDto dto = result.getContent().getFirst();
        assertThat(dto.getId()).isEqualTo(100L);
        assertThat(dto.getProductSku()).isEqualTo("SKU-001");
        assertThat(dto.getCreatedByUsername()).isEqualTo("admin");
    }
}
