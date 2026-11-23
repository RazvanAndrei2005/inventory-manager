package com.inventory.manager.controller;

import com.inventory.manager.dto.StockAdjustmentRequestDto;
import com.inventory.manager.dto.StockMovementResponseDto;
import com.inventory.manager.entity.StockMovementType;
import com.inventory.manager.security.CustomUserDetails;
import com.inventory.manager.service.StockMovementService;
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

import java.time.Instant;

@RestController
@RequestMapping("/api/stock-movements")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class StockMovementController {

    private final StockMovementService stockMovementService;

    public StockMovementController(StockMovementService stockMovementService) {
        this.stockMovementService = stockMovementService;
    }

    @GetMapping
    public ResponseEntity<Page<StockMovementResponseDto>> getStockMovements(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) StockMovementType type,
            @RequestParam(required = false) Long createdById,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(stockMovementService.getMovements(productId, type, createdById, from, to, pageable));
    }

    @PostMapping("/adjustments")
    public ResponseEntity<StockMovementResponseDto> createAdjustment(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody StockAdjustmentRequestDto request) {
        StockMovementResponseDto response = stockMovementService.recordAdjustment(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
