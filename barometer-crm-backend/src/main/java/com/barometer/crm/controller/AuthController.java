package com.barometer.crm.controller;

import com.barometer.crm.dto.LoginRequest;
import com.barometer.crm.dto.LoginResponse;
import com.barometer.crm.dto.RefreshRequest;
import com.barometer.crm.dto.RegisterRequest;
import com.barometer.crm.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request, Authentication auth) {
        // Only sales_head (admin) can create new users
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(java.util.Map.of("error", "Authentication required"));
        }
        String role = auth.getAuthorities().stream().findFirst().map(a -> a.getAuthority()).orElse("");
        if (!"sales_head".equals(role)) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", "Only admins can create users"));
        }
        try {
            return ResponseEntity.ok(authService.register(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @GetMapping("/users")
    public ResponseEntity<?> listUsers(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(java.util.Map.of("error", "Authentication required"));
        }
        // Only admins can list all users
        String role = auth.getAuthorities().stream().findFirst().map(a -> a.getAuthority()).orElse("");
        if (!"sales_head".equals(role)) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", "Admin only"));
        }
        return ResponseEntity.ok(authService.listUsers());
    }
}
