package com.inventory.manager.service;

import com.inventory.manager.entity.RefreshToken;
import com.inventory.manager.entity.User;
import com.inventory.manager.exception.InvalidTokenException;
import com.inventory.manager.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {

    public record IssuedRefreshToken(String rawToken, User user) {
    }

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshExpirationInMs;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               @Value("${jwt-refresh-expiration-milliseconds}") long refreshExpirationInMs) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshExpirationInMs = refreshExpirationInMs;
    }

    @Transactional
    public IssuedRefreshToken createRefreshToken(User user) {
        String rawToken = UUID.randomUUID().toString();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(hashToken(rawToken));
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(Instant.now().plusMillis(refreshExpirationInMs));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);
        return new IssuedRefreshToken(rawToken, user);
    }

    @Transactional(noRollbackFor = {InvalidTokenException.class, DisabledException.class})
    public IssuedRefreshToken rotateRefreshToken(String rawToken) {
        RefreshToken existing = refreshTokenRepository.findByToken(hashToken(rawToken))
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
    public void revokeRefreshToken(String rawToken) {
        RefreshToken existing = refreshTokenRepository.findByToken(hashToken(rawToken))
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));
        existing.setRevoked(true);
        refreshTokenRepository.save(existing);
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public int purgeExpiredAndRevokedTokens() {
        return refreshTokenRepository.deleteExpiredOrRevoked(Instant.now());
    }

    static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
