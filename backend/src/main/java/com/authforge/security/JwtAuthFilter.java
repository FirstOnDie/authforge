package com.authforge.security;

import com.authforge.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter.
 *
 * Intercepts every request, checks for a Bearer token in the
 * Authorization header, validates it, and sets the Spring Security
 * context if valid.
 *
 * Flow:
 * 1. Extract "Authorization: Bearer <token>" header
 * 2. Parse the JWT to get the user email
 * 3. Load the user from the database
 * 4. Validate the token against the user
 * 5. Set the authentication in SecurityContext
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthFilter(JwtTokenProvider jwtTokenProvider, CustomUserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // 1. Extract the token from the header
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);

        try {
            // 2. Extract email from the token
            String email = jwtTokenProvider.extractEmail(jwt);

            // 3. If we have a valid email and no existing authentication
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // 4. Load user details from DB
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // 5. Validate the token
                if (jwtTokenProvider.isTokenValid(jwt, userDetails)) {

                    // 6. Create authentication and set it in context
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            log.warn("Could not authenticate user from JWT: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
