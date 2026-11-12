package com.inventory.manager.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class ProductRequestDto {
    @NotBlank(message = "A Name must be introduced")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;
    @NotBlank(message = "A Sku must be introduced")
    @Size(max = 50, message = "SKU must not exceed 50 characters")
    private String sku;
    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    @Digits(integer = 10, fraction = 2, message = "Price must have at most 10 integer digits and 2 decimals")
    private BigDecimal price;
    @NotNull(message = "Stock quantity is required")
    @PositiveOrZero(message = "Stock quantity cannot be negative")
    private Integer stockQuantity;
    @NotNull(message = "Category ID is required")
    private Long categoryId;
}
