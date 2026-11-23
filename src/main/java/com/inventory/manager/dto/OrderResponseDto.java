package com.inventory.manager.dto;

import com.inventory.manager.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDto {
    private Long id;
    private Long userId;
    private String username;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private List<OrderItemResponseDto> items;
    private Instant createdAt;
    private Instant updatedAt;
}
