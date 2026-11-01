package com.inventory.manager.controller;

import com.inventory.manager.dto.JwtResponseDto;
import com.inventory.manager.dto.LoginRequestDto;
import com.inventory.manager.dto.RegisterRequestDto;
import com.inventory.manager.dto.UserResponseDto;
import com.inventory.manager.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@Valid @RequestBody RegisterRequestDto registerRequestDto){
        authService.register(registerRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body("User was created Successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponseDto> loginUser(@Valid @RequestBody LoginRequestDto loginRequestDto){
        JwtResponseDto jwtResponseDto = authService.login(loginRequestDto);
        return ResponseEntity.ok(jwtResponseDto);
    }
}
