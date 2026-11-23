package com.inventory.manager.controller;

import com.inventory.manager.dto.OrderRequestDto;
import com.inventory.manager.dto.OrderResponseDto;
import com.inventory.manager.dto.UpdateOrderStatusDto;
import com.inventory.manager.entity.OrderStatus;
import com.inventory.manager.security.CustomUserDetails;
import com.inventory.manager.service.OrderService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<OrderResponseDto> createOrder(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody OrderRequestDto request) {
        OrderResponseDto created = orderService.createOrder(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<Page<OrderResponseDto>> getMyOrders(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(required = false) OrderStatus status,
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(orderService.getMyOrders(currentUser.getId(), status, pageable));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Page<OrderResponseDto>> getAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(orderService.getAllOrders(status, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<OrderResponseDto> getOrderById(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id) {
        boolean isPrivileged = hasPrivilegedRole(currentUser);
        return ResponseEntity.ok(orderService.getOrderById(id, currentUser.getId(), isPrivileged));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<OrderResponseDto> updateOrderStatus(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusDto request) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, request.getStatus(), currentUser.getId()));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<OrderResponseDto> cancelOrder(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id) {
        boolean isPrivileged = hasPrivilegedRole(currentUser);
        return ResponseEntity.ok(orderService.cancelOrder(id, currentUser.getId(), isPrivileged));
    }

    private boolean hasPrivilegedRole(CustomUserDetails currentUser) {
        return currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER"));
    }
}
