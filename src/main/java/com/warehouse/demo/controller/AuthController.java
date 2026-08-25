package com.warehouse.demo.controller;

import com.warehouse.demo.dto.request.LoginRequest;
import com.warehouse.demo.dto.request.RefreshTokenRequest;
import com.warehouse.demo.dto.response.LoginResponse;
import com.warehouse.demo.security.RefreshTokenService;
import com.warehouse.demo.security.jwt.JWTService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/login")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/auth")
    public ResponseEntity<LoginResponse> auth(@RequestBody @Valid LoginRequest loginRequest) {

        UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(
                loginRequest.username(),
                loginRequest.password()
        );
        Authentication authentication = authenticationManager.authenticate(authRequest);
        String username = authentication.getName();
        String accessToken = jwtService.generateToken(username);
        String refreshToken = refreshTokenService.createRefreshToken(username);

        return ResponseEntity.ok(new LoginResponse(accessToken, refreshToken));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody @Valid RefreshTokenRequest tokenRequest) {
        return ResponseEntity.ok(refreshTokenService.rotateRefreshToken(tokenRequest));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody @Valid RefreshTokenRequest tokenRequest) {
        refreshTokenService.revokeToken(tokenRequest);
        return ResponseEntity.noContent().build();
    }
}
