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
        registry.enableSimpleBroker("/topic");

        //Any message sent from the client that starts with /app will be mapped to @MessageMapping controllers.
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