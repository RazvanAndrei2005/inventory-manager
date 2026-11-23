package com.inventory.manager.integration;

import com.inventory.manager.dto.OrderItemRequestDto;
import com.inventory.manager.dto.OrderRequestDto;
import com.inventory.manager.dto.UpdateOrderStatusDto;
import com.inventory.manager.entity.Category;
import com.inventory.manager.entity.OrderStatus;
import com.inventory.manager.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createOrder_capturesPriceAtPurchaseTime_deductsStock_andRecordsSaleMovement() throws Exception {
        String userToken = registerAndGetToken("buyer", "buyer@example.com", "password123");
        String managerToken = loginAndGetToken("manager", "manager123");

        Category category = categoryRepository.save(new Category(null, "Electronics", "Devices"));
        Product laptop = productRepository.save(new Product(null, "Laptop", "SKU-ORD-1", new BigDecimal("500.00"), 10, null, category));

        OrderRequestDto request = new OrderRequestDto(List.of(new OrderItemRequestDto(laptop.getId(), 3)));

        String responseBody = mockMvc.perform(post("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(1500.00))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].unitPrice").value(500.00))
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(responseBody).get("id").asLong();

        // Stock deducted
        assertThat(productRepository.findById(laptop.getId()).orElseThrow().getStockQuantity()).isEqualTo(7);

        // Price changes later -> existing order still keeps 500.00
        laptop = productRepository.findById(laptop.getId()).orElseThrow();
        laptop.setPrice(new BigDecimal("999.00"));
        productRepository.save(laptop);

        mockMvc.perform(get("/api/orders/{id}", orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].unitPrice").value(500.00))
                .andExpect(jsonPath("$.totalAmount").value(1500.00));

        // SALE movement recorded
        mockMvc.perform(get("/api/stock-movements")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
                        .param("productId", laptop.getId().toString())
                        .param("type", "SALE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].quantityChange").value(-3))
                .andExpect(jsonPath("$.content[0].quantityAfter").value(7));
    }

    @Test
    void statusFlowAndCancel_enforceTransitionsAndRestoreStock() throws Exception {
        String userToken = registerAndGetToken("buyer2", "buyer2@example.com", "password123");
        String otherUserToken = registerAndGetToken("other", "other@example.com", "password123");
        String managerToken = loginAndGetToken("manager", "manager123");

        Category category = categoryRepository.save(new Category(null, "Accessories", "Gear"));
        Product mouse = productRepository.save(new Product(null, "Mouse", "SKU-ORD-2", new BigDecimal("25.00"), 10, null, category));

        OrderRequestDto request = new OrderRequestDto(List.of(new OrderItemRequestDto(mouse.getId(), 4)));
        String createBody = mockMvc.perform(post("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long orderId = objectMapper.readTree(createBody).get("id").asLong();

        // Other user cannot view or cancel -> 403
        mockMvc.perform(get("/api/orders/{id}", orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherUserToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/orders/{id}/cancel", orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherUserToken))
                .andExpect(status().isForbidden());

        // Regular user cannot list all orders or update status -> 403
        mockMvc.perform(get("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/orders/{id}/status", orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusDto(OrderStatus.CONFIRMED))))
                .andExpect(status().isForbidden());

        // Invalid jump PENDING -> SHIPPED -> 400
        mockMvc.perform(patch("/api/orders/{id}/status", orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusDto(OrderStatus.SHIPPED))))
                .andExpect(status().isBadRequest());

        // Valid: PENDING -> CONFIRMED
        mockMvc.perform(patch("/api/orders/{id}/status", orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusDto(OrderStatus.CONFIRMED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // Owner cancels CONFIRMED order -> 200 and stock restored to 10
        mockMvc.perform(post("/api/orders/{id}/cancel", orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(productRepository.findById(mouse.getId()).orElseThrow().getStockQuantity()).isEqualTo(10);

        // Cannot transition from CANCELLED -> 400
        mockMvc.perform(patch("/api/orders/{id}/status", orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusDto(OrderStatus.SHIPPED))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_returns400_whenInsufficientStockOrEmptyItems() throws Exception {
        String userToken = registerAndGetToken("buyer3", "buyer3@example.com", "password123");
        Category category = categoryRepository.save(new Category(null, "Books", "Reading"));
        Product book = productRepository.save(new Product(null, "Book", "SKU-ORD-3", new BigDecimal("15.00"), 2, null, category));

        mockMvc.perform(post("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderRequestDto(List.of()))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderRequestDto(List.of(new OrderItemRequestDto(book.getId(), 5))))))
                .andExpect(status().isBadRequest());
    }
}
