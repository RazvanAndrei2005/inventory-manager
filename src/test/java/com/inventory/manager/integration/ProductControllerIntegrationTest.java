package com.inventory.manager.integration;

import com.inventory.manager.dto.ProductRequestDto;
import com.inventory.manager.entity.Category;
import com.inventory.manager.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void getAllProducts_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createProduct_returns201_forManager_403ForUser_409ForDuplicateSku_404ForMissingCategory() throws Exception {
        String managerToken = loginAndGetToken("manager", "manager123");
        String userToken = registerAndGetToken("prod_user", "prod_user@example.com", "password123");
        Category category = categoryRepository.save(new Category(null, "Electronics", "Gadgets"));

        ProductRequestDto request = new ProductRequestDto("Laptop", "SKU-100", new BigDecimal("1200.00"), 10, category.getId());

        mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.sku").value("SKU-100"))
                .andExpect(jsonPath("$.stockQuantity").value(10));

        // Duplicate SKU -> 409
        mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // Missing category -> 404
        ProductRequestDto missingCategoryReq = new ProductRequestDto("Mouse", "SKU-101", new BigDecimal("25.00"), 5, 99999L);
        mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(missingCategoryReq)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void createProduct_returns400_whenValidationFails() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        Category category = categoryRepository.save(new Category(null, "Electronics", "Gadgets"));

        ProductRequestDto invalid = new ProductRequestDto("", "", new BigDecimal("-5.00"), -1, category.getId());

        mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void filterAndQueryProducts_returnExpectedResults_and400OnInvalidSort() throws Exception {
        String userToken = registerAndGetToken("filter_user", "filter_user@example.com", "password123");
        Category electronics = categoryRepository.save(new Category(null, "Electronics", "Gadgets"));
        Category books = categoryRepository.save(new Category(null, "Books", "Reading"));

        Product laptop = productRepository.save(new Product(null, "Gaming Laptop", "SKU-L1", new BigDecimal("1500.00"), 10, null, electronics));
        productRepository.save(new Product(null, "USB Cable", "SKU-C1", new BigDecimal("10.00"), 2, null, electronics));
        productRepository.save(new Product(null, "Java Book", "SKU-B1", new BigDecimal("45.00"), 30, null, books));

        mockMvc.perform(get("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .param("name", "laptop")
                        .param("minPrice", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].sku").value("SKU-L1"));

        mockMvc.perform(get("/api/products/{id}", laptop.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Gaming Laptop"));

        mockMvc.perform(get("/api/products/{id}", 99999L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/products/low-stock")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .param("threshold", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].sku").value("SKU-C1"));

        mockMvc.perform(get("/api/products/category/{categoryId}", books.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].sku").value("SKU-B1"));

        mockMvc.perform(get("/api/products/category/{categoryId}", 99999L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .param("sort", "nonExistentField"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void updateRestockAndDeleteProduct_enforceRolesAndBusinessRules() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        String managerToken = loginAndGetToken("manager", "manager123");
        String userToken = registerAndGetToken("stock_user", "stock_user@example.com", "password123");

        Category category = categoryRepository.save(new Category(null, "Peripherals", "Accessories"));
        Product product = productRepository.save(new Product(null, "Keyboard", "SKU-K1", new BigDecimal("80.00"), 5, null, category));

        // Update -> 200 for manager, 403 for user
        ProductRequestDto updateReq = new ProductRequestDto("Mechanical Keyboard", "SKU-K1", new BigDecimal("95.00"), 5, category.getId());
        mockMvc.perform(put("/api/products/{id}", product.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/products/{id}", product.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mechanical Keyboard"))
                .andExpect(jsonPath("$.price").value(95.00));

        // Restock -> 400 on <= 0, 200 on > 0
        mockMvc.perform(patch("/api/products/{id}/restock", product.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
                        .param("quantity", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(patch("/api/products/{id}/restock", product.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
                        .param("quantity", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(15));

        // Delete -> 403 for manager, 204 for admin, 404 after deletion
        mockMvc.perform(delete("/api/products/{id}", product.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/products/{id}", product.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/products/{id}", product.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }
}
