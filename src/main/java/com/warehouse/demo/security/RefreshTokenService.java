package com.warehouse.demo.security;

import com.warehouse.demo.dto.request.RefreshTokenRequest;
import com.warehouse.demo.dto.response.LoginResponse;
import com.warehouse.demo.exception.BusinessLogicException;
import com.warehouse.demo.exception.EntityNotFoundException;
import com.warehouse.demo.model.RefreshToken;
import com.warehouse.demo.model.User;
import com.warehouse.demo.repository.RefreshTokenRepository;
import com.warehouse.demo.repository.UserRepository;
import com.warehouse.demo.security.jwt.JWTService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${app.jwt.refresh_expiration}")
    private Duration refreshExpiration;
    private final UserRepository userRepository;
    private final RefreshTokenRepository repository;
    private final JWTService jwtService;

    public String createRefreshToken(String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException(
                        MessageFormat.format("Пользователь с именем {0} не найден", username)
                ));

        String raw = UUID.randomUUID().toString();
        String hash = hash(raw);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hash);
        refreshToken.setExpiresAt(Instant.now().plus(refreshExpiration));
        repository.save(refreshToken);

        return raw;
    }

    @SneakyThrows
    private String hash(String raw) {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = messageDigest.digest(raw.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(bytes);
    }

    @Transactional
    public LoginResponse rotateRefreshToken(RefreshTokenRequest refreshTokenRequest) {

        String hash = hash(refreshTokenRequest.refreshToken());
        RefreshToken oldRefreshToken = findByTokenHash(hash);
        if (oldRefreshToken.getExpiresAt().isBefore(Instant.now()) || oldRefreshToken.getRevoked()) {
            throw new BusinessLogicException("Refresh-токен недействителен");
        }

        String username = oldRefreshToken.getUser().getUsername();
        oldRefreshToken.setRevoked(true);
        repository.save(oldRefreshToken);

        String accessToken = jwtService.generateToken(username);
        String refreshToken = createRefreshToken(username);
        return new LoginResponse(accessToken, refreshToken);

    }

    private RefreshToken findByTokenHash(String tokenHash) {
        RefreshToken refreshToken = repository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessLogicException("Refresh-токен не найден или недействителен"));
        return refreshToken;
    }

    public void revokeToken(RefreshTokenRequest refreshTokenRequest) {

        String hash = hash(refreshTokenRequest.refreshToken());
        RefreshToken token = findByTokenHash(hash);
        token.setRevoked(true);
        repository.save(token);
    }
}
