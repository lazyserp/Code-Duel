package com.codeduel.codeduel.common.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.codeduel.codeduel.common.util.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Extract the Auhtorization header from the HTTP Headers
        final String authHeader = request.getHeader("Authorization");

        // Validate presence of Bearer prefix (including the space, length 7)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract the token after the "Bearer " prefix (7 characters)
        final String jwtToken = authHeader.substring(7);
        final String username = jwtService.extractUsername(jwtToken);

        // Authenticate only if username is extracted and not yet authenticated in the current context
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            if (jwtService.isTokenValid(jwtToken, userDetails)) {
                // Construct the token representing authenticated credentials and roles
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                // Bind request details (IP address, session ID) to the authentication token
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // Store the authentication inside the context
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // continue filter chain execution
        filterChain.doFilter(request, response);
    }
}
