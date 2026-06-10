package com.dy.liveauction.api.websocket;

import com.dy.liveauction.service.websocket.OnlineUserTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final OnlineUserTracker tracker;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String destination = accessor.getDestination();

        if (destination != null && destination.startsWith("/topic/auction/")) {
            try {
                Long roomId = Long.parseLong(destination.replace("/topic/auction/", ""));
                tracker.userJoined(sessionId, roomId);
                broadcastOnlineCount(roomId);
            } catch (NumberFormatException ignored) {}
        }
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        Long roomId = tracker.userLeft(sessionId);
        if (roomId != null) {
            broadcastOnlineCount(roomId);
        }
    }

    private void broadcastOnlineCount(Long roomId) {
        int count = tracker.getOnlineCount(roomId);
        messagingTemplate.convertAndSend("/topic/auction/" + roomId,
                Map.of("type", "ONLINE", "count", count, "roomId", roomId));
    }
}
