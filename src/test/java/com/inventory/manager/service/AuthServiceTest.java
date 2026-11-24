package com.inventory.manager.service;

import com.inventory.manager.dto.JwtResponseDto;
import com.inventory.manager.dto.LoginRequestDto;
import com.inventory.manager.dto.RefreshTokenRequestDto;
import com.inventory.manager.dto.RegisterRequestDto;
import com.inventory.manager.entity.RefreshToken;
import com.inventory.manager.entity.Role;
import com.inventory.manager.entity.User;
import com.inventory.manager.exception.DuplicateResourceException;
import com.inventory.manager.repository.UserRepository;
import com.inventory.manager.security.CustomUserDetails;
import com.inventory.manager.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void register_savesUserWithEncodedPasswordAndUserRole() {
        RegisterRequestDto request = new RegisterRequestDto("john", "john@example.com", "secret123");
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-secret");

        authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getUsername()).isEqualTo("john");
        assertThat(saved.getEmail()).isEqualTo("john@example.com");
        assertThat(saved.getPassword()).isEqualTo("encoded-secret");
        assertThat(saved.getRole()).isEqualTo(Role.ROLE_USER);
        assertThat(saved.isEnabled()).isTrue();
    }

    @Test
    void register_throwsDuplicateResourceException_whenUsernameExists() {
        RegisterRequestDto request = new RegisterRequestDto("john", "john@example.com", "secret123");
        when(userRepository.existsByUsername("john")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("john");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_throwsDuplicateResourceException_whenEmailExists() {
        RegisterRequestDto request = new RegisterRequestDto("john", "john@example.com", "secret123");
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("john@example.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_returnsJwtResponseDto_whenCredentialsAreValid() {
        LoginRequestDto request = new LoginRequestDto("john", "secret123");
        User user = new User(1L, "john", "john@example.com", "encoded", Role.ROLE_USER, true);
        CustomUserDetails userDetails = new CustomUserDetails(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        RefreshToken refreshToken = new RefreshToken(10L, "refresh-123", user, Instant.now().plusSeconds(3600), false, Instant.now());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtTokenProvider.generateToken(authentication)).thenReturn("jwt-token-123");
        when(jwtTokenProvider.getJwtExpirationInMs()).thenReturn(900000L);
        when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken);

        JwtResponseDto response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("jwt-token-123");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-123");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(900000L);
        assertThat(response.getUsername()).isEqualTo("john");
        assertThat(response.getRole()).isEqualTo("ROLE_USER");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(authentication);
    }

    @Test
    void login_propagatesBadCredentialsException_whenAuthenticationFails() {
        LoginRequestDto request = new LoginRequestDto("john", "wrong");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refreshToken_rotatesAndReturnsNewJwtResponseDto() {
        User user = new User(1L, "john", "john@example.com", "encoded", Role.ROLE_USER, true);
        RefreshToken rotated = new RefreshToken(11L, "refresh-456", user, Instant.now().plusSeconds(3600), false, Instant.now());

        when(refreshTokenService.rotateRefreshToken("refresh-123")).thenReturn(rotated);
        when(jwtTokenProvider.generateTokenFromUsername("john")).thenReturn("new-jwt-456");
        when(jwtTokenProvider.getJwtExpirationInMs()).thenReturn(900000L);

        JwtResponseDto response = authService.refreshToken(new RefreshTokenRequestDto("refresh-123"));

        assertThat(response.getAccessToken()).isEqualTo("new-jwt-456");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-456");
        assertThat(response.getExpiresIn()).isEqualTo(900000L);
        assertThat(response.getUsername()).isEqualTo("john");
        assertThat(response.getRole()).isEqualTo("ROLE_USER");
    }

    @Test
    void logout_revokesRefreshToken() {
        authService.logout(new RefreshTokenRequestDto("refresh-123"));
        verify(refreshTokenService).revokeRefreshToken("refresh-123");
    }
}
