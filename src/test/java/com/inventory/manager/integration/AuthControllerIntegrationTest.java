package com.inventory.manager.integration;

import com.inventory.manager.dto.LoginRequestDto;
import com.inventory.manager.dto.RefreshTokenRequestDto;
import com.inventory.manager.dto.RegisterRequestDto;
import com.inventory.manager.entity.Role;
import com.inventory.manager.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void register_returns201_whenPayloadIsValid() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto("newuser", "newuser@example.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().string("User was created Successfully"));
    }

    @Test
    void register_returns400_whenPayloadIsInvalid() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto("ab", "not-an-email", "123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void register_returns409_whenUsernameAlreadyExists() throws Exception {
        RegisterRequestDto first = new RegisterRequestDto("duplicate", "first@example.com", "password123");
        RegisterRequestDto second = new RegisterRequestDto("duplicate", "second@example.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void register_returns409_whenEmailAlreadyExistsWithDifferentCase() throws Exception {
        RegisterRequestDto first = new RegisterRequestDto("userone", "same@example.com", "password123");
        RegisterRequestDto second = new RegisterRequestDto("usertwo", "SAME@Example.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void login_returns200WithJwtAndRefreshToken_whenCredentialsAreValid() throws Exception {
        LoginRequestDto request = new LoginRequestDto("admin", "admin123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900000))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
    }

    @Test
    void login_returns401_whenCredentialsAreInvalid() throws Exception {
        LoginRequestDto request = new LoginRequestDto("admin", "wrong-password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void login_returns403_whenUserIsDisabled() throws Exception {
        User disabled = new User();
        disabled.setUsername("disableduser");
        disabled.setEmail("disabled@example.com");
        disabled.setPassword(passwordEncoder.encode("password123"));
        disabled.setRole(Role.ROLE_USER);
        disabled.setEnabled(false);
        userRepository.save(disabled);

        LoginRequestDto request = new LoginRequestDto("disableduser", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void refresh_rotatesTokens_andDetectsReuse() throws Exception {
        String loginBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDto("admin", "admin123"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String refreshToken1 = objectMapper.readTree(loginBody).get("refreshToken").asString();

        // 1. Valid refresh rotates token
        String refreshBody = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequestDto(refreshToken1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.expiresIn").value(900000))
                .andReturn().getResponse().getContentAsString();
        JsonNode refreshedJson = objectMapper.readTree(refreshBody);
        String refreshToken2 = refreshedJson.get("refreshToken").asString();
        assertThat(refreshToken2).isNotEqualTo(refreshToken1);

        // 2. Reusing old refreshToken1 returns 401 and revokes refreshToken2
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequestDto(refreshToken1))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequestDto(refreshToken2))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_revokesRefreshToken_andBlocksSubsequentRefresh() throws Exception {
        String loginBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDto("admin", "admin123"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String refreshToken = objectMapper.readTree(loginBody).get("refreshToken").asString();

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequestDto(refreshToken))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequestDto(refreshToken))))
                .andExpect(status().isUnauthorized());
    }
}
