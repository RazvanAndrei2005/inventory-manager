package com.inventory.manager.service;

import com.inventory.manager.dto.UserResponseDto;
import com.inventory.manager.entity.User;
import com.inventory.manager.exception.ResourceNotFoundException;
import com.inventory.manager.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponseDto getCurrentUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return new UserResponseDto(user.getId(), user.getUsername(), user.getEmail(), user.getRole(), user.isEnabled());
    }
}