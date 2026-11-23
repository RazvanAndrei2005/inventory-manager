package com.inventory.manager.dto;

import com.inventory.manager.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusDto {

    @NotNull(message = "Order status is required")
    private OrderStatus status;
}
