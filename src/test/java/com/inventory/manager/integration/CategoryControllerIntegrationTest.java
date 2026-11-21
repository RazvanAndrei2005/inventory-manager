package com.inventory.manager.integration;

import com.inventory.manager.dto.CategoryRequestDto;
import com.inventory.manager.entity.Category;
import com.inventory.manager.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void getAllCategories_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createCategory_returns201_forManagerAndAdmin_and403_forUser() throws Exception {
        String managerToken = loginAndGetToken("manager", "manager123");
        String userToken = registerAndGetToken("cat_user", "cat_user@example.com", "password123");

        CategoryRequestDto request = new CategoryRequestDto("Electronics", "Electronic devices");

        mockMvc.perform(post("/api/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Electronics"))
                .andExpect(jsonPath("$.description").value("Electronic devices"));
    }

    @Test
    void createCategory_returns400_whenNameIsBlank() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        CategoryRequestDto request = new CategoryRequestDto("", "Some description");

        mockMvc.perform(post("/api/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createCategory_returns409_whenNameAlreadyExists() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        categoryRepository.save(new Category(null, "Books", "Printed books"));

        CategoryRequestDto request = new CategoryRequestDto("Books", "Duplicate");

        mockMvc.perform(post("/api/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void getCategoriesAndGetById_return200_and404WhenMissing() throws Exception {
        String userToken = registerAndGetToken("reader", "reader@example.com", "password123");
        Category saved = categoryRepository.save(new Category(null, "Furniture", "Office furniture"));

        mockMvc.perform(get("/api/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Furniture"));

        mockMvc.perform(get("/api/categories/{id}", saved.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Furniture"));

        mockMvc.perform(get("/api/categories/{id}", 99999L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void updateCategory_returns200_forManager_and404WhenMissing() throws Exception {
        String managerToken = loginAndGetToken("manager", "manager123");
        Category saved = categoryRepository.save(new Category(null, "OldName", "Old desc"));

        CategoryRequestDto update = new CategoryRequestDto("NewName", "New desc");

        mockMvc.perform(put("/api/categories/{id}", saved.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("NewName"))
                .andExpect(jsonPath("$.description").value("New desc"));

        mockMvc.perform(put("/api/categories/{id}", 99999L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCategory_returns204_forAdmin_403ForManager_and409WhenHasProducts() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        String managerToken = loginAndGetToken("manager", "manager123");
        Category emptyCat = categoryRepository.save(new Category(null, "EmptyCat", "No products"));
        Category usedCat = categoryRepository.save(new Category(null, "UsedCat", "Has products"));
        productRepository.save(new Product(null, "Chair", "SKU-CHAIR", new BigDecimal("50.00"), 10, null, usedCat));

        mockMvc.perform(delete("/api/categories/{id}", emptyCat.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/categories/{id}", usedCat.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        mockMvc.perform(delete("/api/categories/{id}", emptyCat.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }
}
