package com.codeduel.codeduel.common.security;

import java.net.URI;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.codeduel.codeduel.auth.service.CustomUserDetailsService;
import com.codeduel.codeduel.common.util.JwtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
            @Nullable Exception exception) {
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws Exception {

        URI uri = request.getURI();
        String query = uri.getQuery();
        String token = null;

        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length > 1 && "token".equals(pair[0])) {
                    token = pair[1];
                    break;
                }
            }
        }

        if (token != null) {
            try {
                String username = jwtService.extractUsername(token);
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
                
                if (jwtService.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    
                    // Put in attributes with key "user" matching CustomHandshakeHandler lookup
                    attributes.put("user", authentication);
                    log.info("WebSocket handshake authenticated successfully for user: {}", username);
                } else {
                    log.warn("Invalid JWT token provided for WebSocket handshake");
                    return false;
                }
            } catch (Exception e) {
                log.error("Authentication failed during WebSocket handshake", e);
                return false;
            }
        } else {
            log.warn("No JWT token found in query parameters for WebSocket handshake");
            return false;
        }

        return true;
    }
}
