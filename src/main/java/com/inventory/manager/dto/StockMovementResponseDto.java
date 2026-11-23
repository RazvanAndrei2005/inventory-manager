package com.inventory.manager.dto;

import com.inventory.manager.entity.StockMovementType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementResponseDto {
    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private StockMovementType type;
    private Integer quantityChange;
    private Integer quantityAfter;
    private String reason;
    private Long createdById;
    private String createdByUsername;
    private Instant createdAt;
}
