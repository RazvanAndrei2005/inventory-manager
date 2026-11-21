package com.inventory.manager.integration;

import com.inventory.manager.dto.LoginRequestDto;
import com.inventory.manager.dto.UpdateRoleDto;
import com.inventory.manager.dto.UpdateStatusDto;
import com.inventory.manager.entity.Role;
import com.inventory.manager.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void adminEndpoints_return401WhenUnauthenticated_and403ForNonAdmin() throws Exception {
        String managerToken = loginAndGetToken("manager", "manager123");
        String userToken = registerAndGetToken("regular_user", "regular@example.com", "password123");

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllUsersAndGetById_return200ForAdmin_and404WhenMissing() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        User manager = userRepository.findByUsername("manager").orElseThrow();

        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))));

        mockMvc.perform(get("/api/admin/users/{userId}", manager.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("manager"))
                .andExpect(jsonPath("$.role").value("ROLE_MANAGER"));

        mockMvc.perform(get("/api/admin/users/{userId}", 99999L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void updateUserRole_returns200_409OnSelfChange_and400OnNullRole() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        registerAndGetToken("target_user", "target@example.com", "password123");

        User admin = userRepository.findByUsername("admin").orElseThrow();
        User target = userRepository.findByUsername("target_user").orElseThrow();

        // Admin changing another user's role -> 200
        mockMvc.perform(patch("/api/admin/users/{userId}/role", target.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateRoleDto(Role.ROLE_MANAGER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ROLE_MANAGER"));

        // Admin changing own role -> 409
        mockMvc.perform(patch("/api/admin/users/{userId}/role", admin.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateRoleDto(Role.ROLE_USER))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // Null role -> 400
        mockMvc.perform(patch("/api/admin/users/{userId}/role", target.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateRoleDto(null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void updateUserStatus_returns200_blocksDisabledLogin_and409OnSelfChange() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        registerAndGetToken("status_user", "status@example.com", "password123");

        User admin = userRepository.findByUsername("admin").orElseThrow();
        User target = userRepository.findByUsername("status_user").orElseThrow();

        // Disable user -> 200
        mockMvc.perform(patch("/api/admin/users/{userId}/status", target.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateStatusDto(false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        // Disabled user cannot log in -> 403
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDto("status_user", "password123"))))
                .andExpect(status().isForbidden());

        // Admin changing own status -> 409
        mockMvc.perform(patch("/api/admin/users/{userId}/status", admin.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateStatusDto(false))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
