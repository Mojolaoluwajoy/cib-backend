package org.app.corporateinternetbanking.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.app.corporateinternetbanking.user.domain.entity.User;
import org.app.corporateinternetbanking.user.domain.repository.UserRepository;
import org.app.corporateinternetbanking.user.enums.UserStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@AllArgsConstructor
@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(7);
        String email = jwtService.extractUsername(token);

        if (email != null && !email.isBlank()) {

            // Check token validity first
            if (!jwtService.isTokenValid(token)) {
                log.warn("Invalid or expired JWT token for: {}", email);
                sendUnauthorized(response, "Token is invalid or expired");
                return;
            }

            // Check if user still exists in database
            User user;
            try {
                user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            } catch (UsernameNotFoundException e) {
                log.warn("User no longer exists in database: {}", email);
                sendUnauthorized(response, "Your account no longer exists");
                return;
            }

            // Check if user has been deactivated
            if (user.getStatus() == UserStatus.INACTIVE) {
                log.warn("Deactivated user attempted access: {}", email);
                sendUnauthorized(response, "Your account has been deactivated");
                return;
            }

            // Check if user's organization has been disabled
            if (user.getOrganization() != null &&
                    user.getOrganization().getOrganizationStatus() != null) {
                String orgStatus = user.getOrganization()
                        .getOrganizationStatus().name();
                if (orgStatus.equals("DISABLED")) {
                    log.warn("User from disabled org attempted access: {}", email);
                    sendUnauthorized(response,
                            "Your organization has been disabled");
                    return;
                }
            }

            // All checks passed — authenticate the request
            try {
                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(email);
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities()
                        );
                authToken.setDetails(new WebAuthenticationDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } catch (Exception e) {
                log.error("Authentication failed: {}", e.getMessage());
                sendUnauthorized(response, "Authentication failed");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void sendUnauthorized(HttpServletResponse response,
                                  String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"success\":false,\"message\":\"" + message + "\"}"
        );
    }
}