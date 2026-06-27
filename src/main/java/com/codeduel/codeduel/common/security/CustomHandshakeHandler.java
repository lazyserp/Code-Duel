package com.codeduel.codeduel.common.security;

import java.security.Principal;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

public class CustomHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected @Nullable Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        
        Object authenticatedUser = attributes.get("user");
        if (authenticatedUser instanceof Principal) {
            return (Principal) authenticatedUser;
        }
        return null;
    }
}
