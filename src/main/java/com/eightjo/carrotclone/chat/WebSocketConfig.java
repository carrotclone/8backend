package com.eightjo.carrotclone.chat;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // endpoint 설정 : /ws/chat
        // 이를 통해서 ws://localhost:8080/ws-edit 으로 요청이 들어오면 websocket 통신을 진행.
        registry.addEndpoint("/ws-edit")
                .setAllowedOrigins("http://localhost:3000")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/sub"); //메세지 받기
        config.setApplicationDestinationPrefixes("/pub"); //메세지 보내기
    }

}