package com.inventory.manager.service;

import com.inventory.manager.dto.JwtResponseDto;
import com.inventory.manager.dto.LoginRequestDto;
import com.inventory.manager.dto.RegisterRequestDto;
import com.inventory.manager.entity.Role;
import com.inventory.manager.entity.User;
import com.inventory.manager.repository.UserRepository;
import com.inventory.manager.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static com.inventory.manager.entity.Role.ROLE_USER;

@Service
public class AuthService {
    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    AuthenticationManager authenticationManager;
    JwtTokenProvider jwtTokenProvider;
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }
    public  void register(RegisterRequestDto registerRequestDto) {
        boolean exists=userRepository.existsByUsername(registerRequestDto.getUsername());
        if(exists){
            throw new RuntimeException("User already exists");
        }
        boolean existsEmail=userRepository.existsByEmail(registerRequestDto.getEmail());
        if(existsEmail){
            throw new RuntimeException("Email already exists");
        }
        String encodedPassword=passwordEncoder.encode(registerRequestDto.getPassword());
        User user=new User();
        user.setUsername(registerRequestDto.getUsername());
        user.setEmail(registerRequestDto.getEmail());
        user.setPassword(encodedPassword);
        user.setRole(ROLE_USER);
        userRepository.save(user);
    }
    public JwtResponseDto  login(LoginRequestDto loginRequestDto){
        Authentication authentication=authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        loginRequestDto.getUsername(),
                        loginRequestDto.getPassword()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token=jwtTokenProvider.generateToken(authentication);
        return new JwtResponseDto(token);
    }

}
