package com.barometer.crm.security;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        if (!jwtService.isTokenValid(jwt) || jwtService.isRefreshToken(jwt)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String userId = jwtService.extractUserId(jwt);
        final String role    = jwtService.extractRole(jwt);

        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Store raw role (e.g. "sales_head") — no ROLE_ prefix needed for our manual checks
            String safeRole = (role != null) ? role : "";
            var authorities = List.of(new SimpleGrantedAuthority(safeRole));
            var authToken = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();
        // Only skip JWT processing for public endpoints (login + refresh).
        // /api/auth/register and /api/auth/users need JWT so the controller can check the caller's role.
        return path.equals("/api/auth/login")
            || path.equals("/api/auth/refresh")
            || path.equals("/actuator/health")
            || path.equals("/health");
    }
}
