package com.codeduel.codeduel.common.security;

import java.security.Principal;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import org.springframework.stereotype.Component;

@Component
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


// CLIENT CONNECTS
//      |
//      | ws://localhost:8080/ws?token=eyJhbGci...
//      v
// ┌─────────────────────────────────────────────────────┐
// │  WebSocketAuthHandshakeInterceptor.beforeHandshake()│
// │                                                     │
// │  1. Extract token from URL query string             │
// │  2. Decode JWT → get username                       │
// │  3. Load user from database                         │
// │  4. Validate token signature & expiration           │
// │                                                     │
// │  IF VALID:                                          │
// │     - Create authentication object                  │
// │     - Store in attributes.put("user", auth)         │
// │     - Return TRUE                                   │
// │                                                     │
// │  IF INVALID:                                        │
// │     - Log warning                                   │
// │     - Return FALSE    (connection rejected)         │
// └──────────────────┬──────────────────────────────────┘
//                    │ Returns TRUE (success)
//                    v
// ┌─────────────────────────────────────────────────────┐
// │  CustomHandshakeHandler.determineUser()             │
// │                                                      │
// │  1. Retrieve auth from attributes.get("user")       │
// │  2. Cast to Principal                               │
// │  3. Return Principal                                │
// └──────────────────┬──────────────────────────────────┘
//                    │ Returns Principal(username="john")
//                    v
// ┌─────────────────────────────────────────────────────┐
// │  WebSocket Session Established                      │
// │  - Principal attached to session                    │
// │  - All future messages have user context            │
// └─────────────────────────────────────────────────────┘
