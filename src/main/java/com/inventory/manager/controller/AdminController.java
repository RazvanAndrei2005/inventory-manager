package com.inventory.manager.controller;

import com.inventory.manager.dto.UpdateRoleDto;
import com.inventory.manager.dto.UpdateStatusDto;
import com.inventory.manager.dto.UserResponseDto;
import com.inventory.manager.security.CustomUserDetails;
import com.inventory.manager.service.AdminService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public ResponseEntity<Page<UserResponseDto>> getAllUsers(
            @ParameterObject @PageableDefault(size = 10, sort = "username") Pageable pageable) {
        return ResponseEntity.ok(adminService.getAllUsers(pageable));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.getUserById(userId));
    }

    @PatchMapping("/{userId}/role")
    public ResponseEntity<UserResponseDto> updateUserRole(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateRoleDto updateRoleDto) {
        return ResponseEntity.ok(adminService.updateUserRole(currentUser.getId(), userId, updateRoleDto.getRole()));
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<UserResponseDto> updateUserStatus(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateStatusDto updateStatusDto) {
        return ResponseEntity.ok(adminService.updateUserStatus(currentUser.getId(), userId, updateStatusDto.getEnabled()));
    }
}