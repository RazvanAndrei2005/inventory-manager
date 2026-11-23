package com.inventory.manager.service;

import com.inventory.manager.entity.RefreshToken;
import com.inventory.manager.entity.User;
import com.inventory.manager.exception.InvalidTokenException;
import com.inventory.manager.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshExpirationInMs;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               @Value("${jwt-refresh-expiration-milliseconds}") long refreshExpirationInMs) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshExpirationInMs = refreshExpirationInMs;
    }

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(Instant.now().plusMillis(refreshExpirationInMs));
        refreshToken.setRevoked(false);
        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional(noRollbackFor = {InvalidTokenException.class, DisabledException.class})
    public RefreshToken rotateRefreshToken(String tokenValue) {
        RefreshToken existing = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (existing.isRevoked()) {
            refreshTokenRepository.revokeAllByUserId(existing.getUser().getId());
            throw new InvalidTokenException("Refresh token has been revoked");
        }

        if (!existing.getExpiresAt().isAfter(Instant.now())) {
            existing.setRevoked(true);
            refreshTokenRepository.save(existing);
            throw new InvalidTokenException("Refresh token has expired");
        }

        if (!existing.getUser().isEnabled()) {
            existing.setRevoked(true);
            refreshTokenRepository.save(existing);
            throw new DisabledException("This account has been disabled");
        }

        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        return createRefreshToken(existing.getUser());
    }

    @Transactional
    public void revokeRefreshToken(String tokenValue) {
        RefreshToken existing = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));
        existing.setRevoked(true);
        refreshTokenRepository.save(existing);
    }
}
