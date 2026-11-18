package com.inventory.manager.service;

import com.inventory.manager.dto.UserResponseDto;
import com.inventory.manager.entity.Role;
import com.inventory.manager.entity.User;
import com.inventory.manager.exception.ConflictException;
import com.inventory.manager.exception.ResourceNotFoundException;
import com.inventory.manager.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final UserRepository userRepository;

    public AdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<UserResponseDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public UserResponseDto getUserById(Long userId) {
        return toDto(findUser(userId));
    }

    @Transactional
    public UserResponseDto updateUserRole(Long currentAdminId, Long userId, Role newRole) {
        if (userId.equals(currentAdminId)) {
            throw new ConflictException("You cannot change your own role");
        }
        User user = findUser(userId);
        user.setRole(newRole);
        return toDto(userRepository.save(user));
    }

    @Transactional
    public UserResponseDto updateUserStatus(Long currentAdminId, Long userId, boolean enabled) {
        if (userId.equals(currentAdminId)) {
            throw new ConflictException("You cannot change your own account status");
        }
        User user = findUser(userId);
        user.setEnabled(enabled);
        return toDto(userRepository.save(user));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private UserResponseDto toDto(User user) {
        return new UserResponseDto(user.getId(), user.getUsername(), user.getEmail(), user.getRole(), user.isEnabled());
    }
}