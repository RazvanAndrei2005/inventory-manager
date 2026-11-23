package com.inventory.manager.service;

import com.inventory.manager.dto.JwtResponseDto;
import com.inventory.manager.dto.LoginRequestDto;
import com.inventory.manager.dto.RefreshTokenRequestDto;
import com.inventory.manager.dto.RegisterRequestDto;
import com.inventory.manager.entity.RefreshToken;
import com.inventory.manager.entity.User;
import com.inventory.manager.exception.DuplicateResourceException;
import com.inventory.manager.exception.InvalidTokenException;
import com.inventory.manager.repository.UserRepository;
import com.inventory.manager.security.CustomUserDetails;
import com.inventory.manager.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.inventory.manager.entity.Role.ROLE_USER;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider jwtTokenProvider,
                       RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public void register(RegisterRequestDto registerRequestDto) {
        if (userRepository.existsByUsername(registerRequestDto.getUsername())) {
            throw new DuplicateResourceException("Username '" + registerRequestDto.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(registerRequestDto.getEmail())) {
            throw new DuplicateResourceException("Email '" + registerRequestDto.getEmail() + "' is already registered");
        }
        User user = new User();
        user.setUsername(registerRequestDto.getUsername());
        user.setEmail(registerRequestDto.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequestDto.getPassword()));
        user.setRole(ROLE_USER);
        userRepository.save(user);
    }

    @Transactional
    public JwtResponseDto login(LoginRequestDto loginRequestDto) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        loginRequestDto.getUsername(),
                        loginRequestDto.getPassword()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String accessToken = jwtTokenProvider.generateToken(authentication);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getUser());
        String username = userDetails.getUsername();
        String role = userDetails.getAuthorities().iterator().next().getAuthority();

        return new JwtResponseDto(
                accessToken,
                refreshToken.getToken(),
                "Bearer",
                jwtTokenProvider.getJwtExpirationInMs(),
                username,
                role
        );
    }

    @Transactional(noRollbackFor = {InvalidTokenException.class, DisabledException.class})
    public JwtResponseDto refreshToken(RefreshTokenRequestDto request) {
        RefreshToken rotated = refreshTokenService.rotateRefreshToken(request.getRefreshToken());
        User user = rotated.getUser();
        String accessToken = jwtTokenProvider.generateTokenFromUsername(user.getUsername());

        return new JwtResponseDto(
                accessToken,
                rotated.getToken(),
                "Bearer",
                jwtTokenProvider.getJwtExpirationInMs(),
                user.getUsername(),
                user.getRole().name()
        );
    }

    @Transactional
    public void logout(RefreshTokenRequestDto request) {
        refreshTokenService.revokeRefreshToken(request.getRefreshToken());
    }
}
