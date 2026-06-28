package com.codeduel.codeduel.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.codeduel.codeduel.common.security.CustomHandshakeHandler;
import com.codeduel.codeduel.common.security.WebSocketAuthHandshakeInterceptor;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer{

    private final WebSocketAuthHandshakeInterceptor webSocketAuthHandshakeInterceptor;
    private final CustomHandshakeHandler customHandshakeHandler;
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        
        //  This prefix will be used for broadcasting data to clients (e.g., subscribing to /topic/match/{matchId})
        // /topic/* = Server → Clients (broadcast)
        registry.enableSimpleBroker("/topic");

        //Any message sent from the client that starts with /app will be mapped to @MessageMapping controllers.
        // app/* = Client → Server (one-way)
        registry.setApplicationDestinationPrefixes("/app");
        
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*")
                .addInterceptors(webSocketAuthHandshakeInterceptor)
                .setHandshakeHandler(customHandshakeHandler);        
    }
}

// CLIENT (Browser)                                   SERVER (Spring Boot)
//      |                                                    |
//      | 1. Connect to ws://localhost:8080/ws               |
//      |--------------------------------------------------> |
//      |                                                    |
//      |                    [WebSocketAuthHandshakeInterceptor]
//      |                    - Validates JWT token
//      |                    - Rejects if invalid
//      |                                                    |
//      |                    [CustomHandshakeHandler]
//      |                    - Extracts username from JWT
//      |                    - Creates Principal
//      |                                                    |
//      | 2. Connection Established (with username attached)|
//      |<--------------------------------------------------|
//      |                                                    |
//      | 3. Subscribe to /topic/match/abc-123              |
//      |-------------------------------------------------->|
//      |                                                    |
//      | 4. Send to /app/match/ready                       |
//      |-------------------------------------------------->|
//      |                                                    |
//      |                    [@MessageMapping("/match/ready")]
//      |                    - Process ready signal
//      |                    - Add username to Redis set
//      |                    - Check if both ready
//      |                                                    |
//      | 5. Broadcast to /topic/match/abc-123              |
//      |<--------------------------------------------------|
//      |   Message: {matchId: "abc-123", status: "ACTIVE"} |
//      |                                                    |
