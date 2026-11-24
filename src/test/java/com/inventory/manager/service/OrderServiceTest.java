package com.inventory.manager.service;

import com.inventory.manager.dto.OrderItemRequestDto;
import com.inventory.manager.dto.OrderRequestDto;
import com.inventory.manager.dto.OrderResponseDto;
import com.inventory.manager.entity.Category;
import com.inventory.manager.entity.Order;
import com.inventory.manager.entity.OrderItem;
import com.inventory.manager.entity.OrderStatus;
import com.inventory.manager.entity.Product;
import com.inventory.manager.entity.Role;
import com.inventory.manager.entity.StockMovementType;
import com.inventory.manager.entity.User;
import com.inventory.manager.exception.BadRequestException;
import com.inventory.manager.exception.ResourceNotFoundException;
import com.inventory.manager.repository.OrderRepository;
import com.inventory.manager.repository.ProductRepository;
import com.inventory.manager.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StockMovementService stockMovementService;

    @InjectMocks
    private OrderService orderService;

    private User sampleUser(Long id, Role role) {
        return new User(id, "user" + id, "user" + id + "@example.com", "pw", role, true);
    }

    private Product sampleProduct(Long id, String price, int stock) {
        Category cat = new Category(1L, "Electronics", "Devices");
        return new Product(id, "Product-" + id, "SKU-" + id, new BigDecimal(price), stock, 0L, cat);
    }

    @Test
    void createOrder_deductsStock_capturesPrice_andRecordsSaleMovements() {
        User user = sampleUser(1L, Role.ROLE_USER);
        Product laptop = sampleProduct(10L, "1000.00", 5);
        Product mouse = sampleProduct(11L, "25.50", 20);

        OrderRequestDto request = new OrderRequestDto(List.of(
                new OrderItemRequestDto(11L, 3),
                new OrderItemRequestDto(10L, 2)
        ));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(10L)).thenReturn(Optional.of(sampleProduct(10L, "10.00", 5)));
        when(productRepository.findById(10L)).thenReturn(Optional.of(laptop));
        when(productRepository.findById(11L)).thenReturn(Optional.of(mouse));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(50L);
            o.setCreatedAt(Instant.now());
            o.setUpdatedAt(Instant.now());
            return o;
        });

        OrderResponseDto response = orderService.createOrder(1L, request);

        assertThat(response.getId()).isEqualTo(50L);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getTotalAmount()).isEqualByComparingTo("2076.50");
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems().get(0).getProductId()).isEqualTo(10L);
        assertThat(response.getItems().get(0).getUnitPrice()).isEqualByComparingTo("1000.00");
        assertThat(response.getItems().get(1).getProductId()).isEqualTo(11L);
        assertThat(response.getItems().get(1).getLineTotal()).isEqualByComparingTo("76.50");

        assertThat(laptop.getStockQuantity()).isEqualTo(3);
        assertThat(mouse.getStockQuantity()).isEqualTo(17);

        verify(stockMovementService).recordMovement(laptop, StockMovementType.SALE, -2, 3, "Order #50", user);
        verify(stockMovementService).recordMovement(mouse, StockMovementType.SALE, -3, 17, "Order #50", user);
    }

    @Test
    void createOrder_throwsBadRequestException_whenInsufficientStock() {
        User user = sampleUser(1L, Role.ROLE_USER);
        Product laptop = sampleProduct(10L, "1000.00", 1);
        OrderRequestDto request = new OrderRequestDto(List.of(new OrderItemRequestDto(10L, 5)));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(10L)).thenReturn(Optional.of(sampleProduct(10L, "10.00", 5)));
        when(productRepository.findById(10L)).thenReturn(Optional.of(laptop));

        assertThatThrownBy(() -> orderService.createOrder(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient stock");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_throwsBadRequestException_whenDuplicateProductId() {
        User user = sampleUser(1L, Role.ROLE_USER);
        OrderRequestDto request = new OrderRequestDto(List.of(
                new OrderItemRequestDto(10L, 1),
                new OrderItemRequestDto(10L, 2)
        ));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(10L)).thenReturn(Optional.of(sampleProduct(10L, "10.00", 5)));

        assertThatThrownBy(() -> orderService.createOrder(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Duplicate product ID");
    }

    @Test
    void createOrder_throwsResourceNotFoundException_whenProductMissing() {
        User user = sampleUser(1L, Role.ROLE_USER);
        OrderRequestDto request = new OrderRequestDto(List.of(new OrderItemRequestDto(99L, 1)));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(1L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getOrderById_enforcesOwnershipUnlessPrivileged() {
        User owner = sampleUser(1L, Role.ROLE_USER);
        Order order = new Order();
        order.setId(50L);
        order.setUser(owner);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("100.00"));
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        assertThat(orderService.getOrderById(50L, 1L, false).getId()).isEqualTo(50L);
        assertThat(orderService.getOrderById(50L, 2L, true).getId()).isEqualTo(50L);

        assertThatThrownBy(() -> orderService.getOrderById(50L, 2L, false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateOrderStatus_allowsValidFlow_andRejectsInvalidJumps() {
        User owner = sampleUser(1L, Role.ROLE_USER);
        Order order = new Order();
        order.setId(50L);
        order.setUser(owner);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("100.00"));

        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        // Invalid jump: PENDING -> SHIPPED
        assertThatThrownBy(() -> orderService.updateOrderStatus(50L, OrderStatus.SHIPPED, 99L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid order status transition");

        // Valid: PENDING -> CONFIRMED -> SHIPPED -> DELIVERED
        assertThat(orderService.updateOrderStatus(50L, OrderStatus.CONFIRMED, 99L).getStatus())
                .isEqualTo(OrderStatus.CONFIRMED);
        assertThat(orderService.updateOrderStatus(50L, OrderStatus.SHIPPED, 99L).getStatus())
                .isEqualTo(OrderStatus.SHIPPED);
        assertThat(orderService.updateOrderStatus(50L, OrderStatus.DELIVERED, 99L).getStatus())
                .isEqualTo(OrderStatus.DELIVERED);

        // Terminal: DELIVERED -> CANCELLED
        assertThatThrownBy(() -> orderService.updateOrderStatus(50L, OrderStatus.CANCELLED, 99L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void cancelOrder_restoresStockAndRecordsAdjustment() {
        User owner = sampleUser(1L, Role.ROLE_USER);
        Product laptop = sampleProduct(10L, "1000.00", 3);

        Order order = new Order();
        order.setId(50L);
        order.setUser(owner);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setTotalAmount(new BigDecimal("2000.00"));
        OrderItem item = new OrderItem(1L, order, laptop, 2, new BigDecimal("1000.00"));
        order.addItem(item);

        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(orderRepository.save(order)).thenReturn(order);

        OrderResponseDto cancelled = orderService.cancelOrder(50L, 1L, false);

        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(laptop.getStockQuantity()).isEqualTo(5);
        verify(stockMovementService).recordMovement(
                laptop, StockMovementType.ADJUSTMENT, 2, 5, "Order #50 cancelled", owner);
    }
}
