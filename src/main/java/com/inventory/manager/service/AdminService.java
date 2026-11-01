package com.inventory.manager.service;

import com.inventory.manager.dto.UserResponseDto;
import com.inventory.manager.entity.Role;
import com.inventory.manager.entity.User;
import com.inventory.manager.exception.ResourceNotFoundException;
import com.inventory.manager.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {
    UserRepository userRepository;
    public AdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    public List<UserResponseDto> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(user -> new UserResponseDto(user.getId(), user.getUsername(), user.getEmail(), user.getRole()))
                .collect(Collectors.toList());
    }
    public UserResponseDto updateUserRole(Long userId, Role newRole){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setRole(newRole);
        User updatedUser = userRepository.save(user);
        return new UserResponseDto(updatedUser.getId(), updatedUser.getUsername(), updatedUser.getEmail(), updatedUser.getRole());
    }
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        userRepository.delete(user);
    }
    public UserResponseDto getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return new UserResponseDto(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }
}
