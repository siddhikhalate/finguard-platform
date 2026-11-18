package com.finguard.auth.controller;

import com.finguard.auth.dto.AuthResponse;
import com.finguard.auth.dto.LoginRequest;
import com.finguard.auth.dto.RegisterRequest;
import com.finguard.auth.dto.TokenRefreshRequest;
import com.finguard.auth.dto.TokenRefreshResponse;
import com.finguard.auth.dto.UserSummaryDto;
import com.finguard.auth.security.JwtTokenProvider;
import com.finguard.auth.security.UserPrincipal;
import com.finguard.auth.service.AuthService;
import com.finguard.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication & Identity", description = "Endpoints for user login, registration, token refresh, and identity inspection")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider tokenProvider;

    @PostMapping("/login")
    @Operation(summary = "Authenticate user credentials", description = "Validates username/password and issues access and refresh tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @PostMapping("/register")
    @Operation(summary = "Register new user account", description = "Creates a new user record with default ROLE_USER permissions")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("User registered successfully", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh expired access token", description = "Generates a new access token using a valid non-expired refresh token")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        TokenRefreshResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed successfully", response));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Retrieve current authenticated user", description = "Returns identity and role profile of the currently signed-in user")
    public ResponseEntity<ApiResponse<UserSummaryDto>> getCurrentUser(@AuthenticationPrincipal UserPrincipal currentUser) {
        UserSummaryDto userSummary = authService.getCurrentUser(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.ok(userSummary));
    }

    @GetMapping("/validate")
    @Operation(summary = "Validate JWT Token", description = "Token introspection endpoint used by API Gateway to verify tokens")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (tokenProvider.validateToken(token)) {
                return ResponseEntity.ok(ApiResponse.ok("Token is valid", Map.of(
                        "valid", true,
                        "userId", tokenProvider.getUserIdFromToken(token),
                        "username", tokenProvider.getUsernameFromToken(token),
                        "roles", tokenProvider.getRolesFromToken(token)
                )));
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.ok("Token is invalid or missing", Map.of("valid", false)));
    }
}
