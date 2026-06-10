package com.dy.liveauction.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket + STOMP 配置
 *
 * 直播间隔离方案：
 *   前端订阅 /topic/auction/{roomId} → 只收到该房间的消息
 *   前端发送 /app/bid/{roomId} → 路由到对应 Controller
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 客户端订阅的前缀（服务端 -> 客户端）
        registry.enableSimpleBroker("/topic");
        // 客户端发送消息的前缀（客户端 -> 服务端）
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 连接端点，允许跨域
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();  // 降级兼容：不原生支持 WebSocket 的浏览器走 HTTP 长轮询
    }
}
