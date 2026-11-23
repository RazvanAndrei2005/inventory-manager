package com.inventory.manager.integration;

import com.inventory.manager.dto.ProductRequestDto;
import com.inventory.manager.dto.StockAdjustmentRequestDto;
import com.inventory.manager.entity.Category;
import com.inventory.manager.entity.Product;
import com.inventory.manager.entity.StockMovementType;
import com.inventory.manager.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StockMovementControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void stockMovementEndpoints_enforceAuthAndRoles() throws Exception {
        String userToken = registerAndGetToken("sm_user", "sm_user@example.com", "password123");
        String managerToken = loginAndGetToken("manager", "manager123");

        mockMvc.perform(get("/api/stock-movements"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/stock-movements")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/stock-movements")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        StockAdjustmentRequestDto req = new StockAdjustmentRequestDto(1L, 5, "Audit");
        mockMvc.perform(post("/api/stock-movements/adjustments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void productLifecycle_recordsMovements_andFiltersWork() throws Exception {
        String managerToken = loginAndGetToken("manager", "manager123");
        String adminToken = loginAndGetToken("admin", "admin123");
        User manager = userRepository.findByUsername("manager").orElseThrow();

        Category category = categoryRepository.save(new Category(null, "Electronics", "Devices"));
        ProductRequestDto createReq = new ProductRequestDto("Monitor", "SKU-MON-1", new BigDecimal("299.99"), 10, category.getId());

        // 1. Create product -> INITIAL_STOCK (+10, after=10)
        String createBody = mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long productId = objectMapper.readTree(createBody).get("id").asLong();

        // 2. Restock product -> RESTOCK (+5, after=15)
        mockMvc.perform(patch("/api/products/{id}/restock", productId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
                        .param("quantity", "5"))
                .andExpect(status().isOk());

        // 3. Update product stock -> ADJUSTMENT (-3, after=12)
        ProductRequestDto updateReq = new ProductRequestDto("Monitor Pro", "SKU-MON-1", new BigDecimal("319.99"), 12, category.getId());
        mockMvc.perform(put("/api/products/{id}", productId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk());

        // All 3 movements for productId, newest first
        mockMvc.perform(get("/api/stock-movements")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("productId", productId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].type").value(StockMovementType.ADJUSTMENT.name()))
                .andExpect(jsonPath("$.content[0].quantityChange").value(-3))
                .andExpect(jsonPath("$.content[0].quantityAfter").value(12))
                .andExpect(jsonPath("$.content[0].createdByUsername").value("admin"))
                .andExpect(jsonPath("$.content[1].type").value(StockMovementType.RESTOCK.name()))
                .andExpect(jsonPath("$.content[2].type").value(StockMovementType.INITIAL_STOCK.name()));

        // Filter by type & createdById & time range
        mockMvc.perform(get("/api/stock-movements")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("type", "RESTOCK")
                        .param("createdById", manager.getId().toString())
                        .param("from", Instant.now().minusSeconds(60).toString())
                        .param("to", Instant.now().plusSeconds(60).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].quantityChange").value(5))
                .andExpect(jsonPath("$.content[0].productSku").value("SKU-MON-1"));
    }

    @Test
    void createAdjustment_updatesStockAndValidatesRules() throws Exception {
        String managerToken = loginAndGetToken("manager", "manager123");
        Category category = categoryRepository.save(new Category(null, "Office", "Supplies"));
        Product product = productRepository.save(new Product(null, "Chair", "SKU-CHR-1", new BigDecimal("120.00"), 10, null, category));

        // Negative adjustment -> 201 Created, stock becomes 7
        StockAdjustmentRequestDto validNeg = new StockAdjustmentRequestDto(product.getId(), -3, "Damaged in warehouse");
        mockMvc.perform(post("/api/stock-movements/adjustments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validNeg)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("ADJUSTMENT"))
                .andExpect(jsonPath("$.quantityChange").value(-3))
                .andExpect(jsonPath("$.quantityAfter").value(7))
                .andExpect(jsonPath("$.reason").value("Damaged in warehouse"));

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updated.getStockQuantity()).isEqualTo(7);

        // Zero quantityChange -> 400
        StockAdjustmentRequestDto zeroReq = new StockAdjustmentRequestDto(product.getId(), 0, "No change");
        mockMvc.perform(post("/api/stock-movements/adjustments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroReq)))
                .andExpect(status().isBadRequest());

        // Insufficient stock -> 400
        StockAdjustmentRequestDto tooLowReq = new StockAdjustmentRequestDto(product.getId(), -10, "Shrinkage");
        mockMvc.perform(post("/api/stock-movements/adjustments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tooLowReq)))
                .andExpect(status().isBadRequest());

        // Blank reason -> 400 validation
        StockAdjustmentRequestDto blankReason = new StockAdjustmentRequestDto(product.getId(), 2, "");
        mockMvc.perform(post("/api/stock-movements/adjustments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankReason)))
                .andExpect(status().isBadRequest());

        // Missing product -> 404
        StockAdjustmentRequestDto missingProduct = new StockAdjustmentRequestDto(99999L, 2, "Found items");
        mockMvc.perform(post("/api/stock-movements/adjustments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(missingProduct)))
                .andExpect(status().isNotFound());
    }
}
