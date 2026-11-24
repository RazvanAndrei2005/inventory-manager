package com.inventory.manager.service;

import com.inventory.manager.entity.RefreshToken;
import com.inventory.manager.entity.Role;
import com.inventory.manager.entity.User;
import com.inventory.manager.exception.InvalidTokenException;
import com.inventory.manager.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, 604800000L);
    }

    private User sampleUser(boolean enabled) {
        return new User(1L, "alice", "alice@example.com", "pw", Role.ROLE_USER, enabled);
    }

    @Test
    void createRefreshToken_storesSha256HashAndReturnsRawToken() {
        User user = sampleUser(true);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshTokenService.IssuedRefreshToken issued = refreshTokenService.createRefreshToken(user);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();

        assertThat(issued.rawToken()).isNotBlank();
        assertThat(issued.user()).isSameAs(user);
        assertThat(saved.getToken()).isEqualTo(RefreshTokenService.hashToken(issued.rawToken()));
        assertThat(saved.getToken()).isNotEqualTo(issued.rawToken());
        assertThat(saved.getToken()).hasSize(64);
        assertThat(saved.isRevoked()).isFalse();
        assertThat(saved.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void rotateRefreshToken_revokesOldAndCreatesNewToken_whenValid() {
        User user = sampleUser(true);
        String oldHash = RefreshTokenService.hashToken("old-token");
        RefreshToken existing = new RefreshToken(10L, oldHash, user, Instant.now().plusSeconds(3600), false, Instant.now());

        when(refreshTokenRepository.findByToken(oldHash)).thenReturn(Optional.of(existing));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshTokenService.IssuedRefreshToken rotated = refreshTokenService.rotateRefreshToken("old-token");

        assertThat(existing.isRevoked()).isTrue();
        assertThat(rotated.rawToken()).isNotEqualTo("old-token");
        assertThat(rotated.user()).isSameAs(user);
    }

    @Test
    void rotateRefreshToken_revokesAllUserTokensAndThrows_whenTokenAlreadyRevoked() {
        User user = sampleUser(true);
        String revokedHash = RefreshTokenService.hashToken("revoked-token");
        RefreshToken revoked = new RefreshToken(10L, revokedHash, user, Instant.now().plusSeconds(3600), true, Instant.now());
        when(refreshTokenRepository.findByToken(revokedHash)).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken("revoked-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("revoked");

        verify(refreshTokenRepository).revokeAllByUserId(1L);
    }

    @Test
    void rotateRefreshToken_revokesAndThrows_whenTokenExpired() {
        User user = sampleUser(true);
        String expiredHash = RefreshTokenService.hashToken("expired-token");
        RefreshToken expired = new RefreshToken(10L, expiredHash, user, Instant.now().minusSeconds(10), false, Instant.now().minusSeconds(100));
        when(refreshTokenRepository.findByToken(expiredHash)).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken("expired-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expired");

        assertThat(expired.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(expired);
    }

    @Test
    void rotateRefreshToken_revokesAndThrowsDisabledException_whenUserDisabled() {
        User disabledUser = sampleUser(false);
        String hash = RefreshTokenService.hashToken("tok");
        RefreshToken existing = new RefreshToken(10L, hash, disabledUser, Instant.now().plusSeconds(3600), false, Instant.now());
        when(refreshTokenRepository.findByToken(hash)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken("tok"))
                .isInstanceOf(DisabledException.class);

        assertThat(existing.isRevoked()).isTrue();
    }

    @Test
    void revokeRefreshToken_marksRevoked_orThrowsWhenMissing() {
        User user = sampleUser(true);
        String hash = RefreshTokenService.hashToken("tok");
        RefreshToken existing = new RefreshToken(10L, hash, user, Instant.now().plusSeconds(3600), false, Instant.now());
        when(refreshTokenRepository.findByToken(hash)).thenReturn(Optional.of(existing));
        when(refreshTokenRepository.findByToken(RefreshTokenService.hashToken("missing"))).thenReturn(Optional.empty());

        refreshTokenService.revokeRefreshToken("tok");
        assertThat(existing.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(existing);

        assertThatThrownBy(() -> refreshTokenService.revokeRefreshToken("missing"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void purgeExpiredAndRevokedTokens_delegatesToRepository() {
        when(refreshTokenRepository.deleteExpiredOrRevoked(any(Instant.class))).thenReturn(3);

        int deleted = refreshTokenService.purgeExpiredAndRevokedTokens();

        assertThat(deleted).isEqualTo(3);
        verify(refreshTokenRepository).deleteExpiredOrRevoked(any(Instant.class));
    }
}
