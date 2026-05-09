package com.barometer.crm.service;

import com.barometer.crm.dto.LoginRequest;
import com.barometer.crm.dto.LoginResponse;
import com.barometer.crm.dto.RefreshRequest;
import com.barometer.crm.dto.RegisterRequest;
import com.barometer.crm.exception.EntityNotFoundException;
import com.barometer.crm.model.User;
import com.barometer.crm.repository.UserRepository;
import com.barometer.crm.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        String role = (request.getRole() == null || request.getRole().isBlank()) ? "sales_head" : request.getRole();
        user.setRole(role);
        // derive initials from name
        String[] parts = request.getName().trim().split("\\s+");
        String initials = parts.length >= 2
                ? String.valueOf(parts[0].charAt(0)) + parts[parts.length - 1].charAt(0)
                : request.getName().substring(0, Math.min(2, request.getName().length()));
        user.setAvatarInitials(initials.toUpperCase());
        user.setActive(true);

        User saved = userRepository.save(user);

        String accessToken  = jwtService.generateAccessToken(saved.getId(), saved.getEmail(), saved.getRole());
        String refreshToken = jwtService.generateRefreshToken(saved.getId());

        return new LoginResponse(
                accessToken,
                refreshToken,
                saved.getId(),
                saved.getEmail(),
                saved.getName(),
                saved.getRole(),
                saved.getAvatarInitials()
        );
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!user.isActive()) {
            throw new BadCredentialsException("Account is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String accessToken  = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        return new LoginResponse(
                accessToken,
                refreshToken,
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getAvatarInitials()
        );
    }

    public LoginResponse refresh(RefreshRequest request) {
        String token = request.getRefreshToken();

        if (!jwtService.isTokenValid(token) || !jwtService.isRefreshToken(token)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        String userId = jwtService.extractUserId(token);
        User user = userRepository.findById(java.util.Objects.requireNonNull(userId))
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!user.isActive()) {
            throw new BadCredentialsException("Account is disabled");
        }

        String newAccessToken  = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String newRefreshToken = jwtService.generateRefreshToken(user.getId());

        return new LoginResponse(
                newAccessToken,
                newRefreshToken,
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getAvatarInitials()
        );
    }

    public List<Map<String, Object>> listUsers() {
        return userRepository.findAll().stream()
                .map(u -> Map.<String, Object>of(
                        "id",             u.getId(),
                        "email",          u.getEmail(),
                        "name",           u.getName(),
                        "role",           u.getRole(),
                        "avatarInitials", u.getAvatarInitials() != null ? u.getAvatarInitials() : "",
                        "active",         u.isActive()
                ))
                .toList();
    }
}
