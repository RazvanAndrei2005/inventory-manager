package com.inventory.manager.integration;

import com.inventory.manager.dto.LoginRequestDto;
import com.inventory.manager.dto.RegisterRequestDto;
import com.inventory.manager.entity.Role;
import com.inventory.manager.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

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
    void register_returns409_whenEmailAlreadyExists() throws Exception {
        RegisterRequestDto first = new RegisterRequestDto("userone", "same@example.com", "password123");
        RegisterRequestDto second = new RegisterRequestDto("usertwo", "same@example.com", "password123");

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
    void login_returns200WithJwt_whenCredentialsAreValid() throws Exception {
        LoginRequestDto request = new LoginRequestDto("admin", "admin123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
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
}
