package com.inventory.manager.controller;

import com.inventory.manager.dto.UpdateRoleDto;
import com.inventory.manager.dto.UserResponseDto;
import com.inventory.manager.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        List<UserResponseDto> userResponseDtoList = adminService.getAllUsers();
        return ResponseEntity.ok(userResponseDtoList);
    }

    @PatchMapping("/{userId}/role")
    public ResponseEntity<UserResponseDto> updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateRoleDto updateRoleDto) {
        UserResponseDto userResponseDto = adminService.updateUserRole(userId, updateRoleDto.getRole());
        return ResponseEntity.ok(userResponseDto);
    }
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long userId)
    {
        UserResponseDto userResponseDto = adminService.getUserById(userId);
        return ResponseEntity.ok(userResponseDto);
    }
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.noContent().build();}
}