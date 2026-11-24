package com.inventory.manager.service;

import com.inventory.manager.dto.UserResponseDto;
import com.inventory.manager.entity.Role;
import com.inventory.manager.entity.User;
import com.inventory.manager.exception.ConflictException;
import com.inventory.manager.exception.ResourceNotFoundException;
import com.inventory.manager.repository.RefreshTokenRepository;
import com.inventory.manager.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    void getAllUsers_returnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        User user = new User(2L, "alice", "alice@example.com", "pw", Role.ROLE_USER, true);
        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(user), pageable, 1));

        Page<UserResponseDto> result = adminService.getAllUsers(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getUsername()).isEqualTo("alice");
        assertThat(result.getContent().getFirst().getRole()).isEqualTo(Role.ROLE_USER);
    }

    @Test
    void getUserById_returnsDto_whenFound() {
        User user = new User(2L, "alice", "alice@example.com", "pw", Role.ROLE_USER, true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        UserResponseDto result = adminService.getUserById(2L);

        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void getUserById_throwsResourceNotFoundException_whenMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateUserRole_updatesRole_whenTargetIsAnotherUser() {
        User user = new User(2L, "alice", "alice@example.com", "pw", Role.ROLE_USER, true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserResponseDto result = adminService.updateUserRole(1L, 2L, Role.ROLE_MANAGER);

        assertThat(result.getRole()).isEqualTo(Role.ROLE_MANAGER);
        verify(userRepository).save(user);
    }

    @Test
    void updateUserRole_throwsConflictException_whenAdminTargetsSelf() {
        assertThatThrownBy(() -> adminService.updateUserRole(1L, 1L, Role.ROLE_USER))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("own role");

        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserStatus_updatesEnabledFlag_whenTargetIsAnotherUser() {
        User user = new User(2L, "alice", "alice@example.com", "pw", Role.ROLE_USER, true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserResponseDto result = adminService.updateUserStatus(1L, 2L, false);

        assertThat(result.getEnabled()).isFalse();
        verify(userRepository).save(user);
        verify(refreshTokenRepository).revokeAllByUserId(2L);
    }

    @Test
    void updateUserStatus_throwsConflictException_whenAdminTargetsSelf() {
        assertThatThrownBy(() -> adminService.updateUserStatus(1L, 1L, false))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("own account status");

        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).save(any());
    }
}
