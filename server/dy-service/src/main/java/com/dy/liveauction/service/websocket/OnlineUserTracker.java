package com.dy.liveauction.service.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 在线用户追踪器（内存版）
 * 跟踪每个直播间有多少在线用户，通过 WebSocket 连接/断开来计数
 */
@Slf4j
@Component
public class OnlineUserTracker {

    /** roomId -> sessionId集合 */
    private final Map<Long, Set<String>> roomSessions = new ConcurrentHashMap<>();
    /** sessionId -> roomId */
    private final Map<String, Long> sessionRoom = new ConcurrentHashMap<>();

    public void userJoined(String sessionId, Long roomId) {
        // 已跟踪的 session 忽略重复订阅（StompJS 内部可能多次发送 SUBSCRIBE 帧）
        Long existingRoom = sessionRoom.get(sessionId);
        if (existingRoom != null) {
            if (existingRoom.equals(roomId)) return; // 同一房间，忽略
            // 换了房间，先从旧房间移除
            Set<String> oldSessions = roomSessions.get(existingRoom);
            if (oldSessions != null) {
                oldSessions.remove(sessionId);
                if (oldSessions.isEmpty()) roomSessions.remove(existingRoom);
            }
        }
        sessionRoom.put(sessionId, roomId);
        roomSessions.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        log.info("用户加入房间: session={}, room={}, 在线人数={}", sessionId, roomId, getOnlineCount(roomId));
    }

    public Long userLeft(String sessionId) {
        Long roomId = sessionRoom.remove(sessionId);
        if (roomId != null) {
            Set<String> sessions = roomSessions.get(roomId);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) roomSessions.remove(roomId);
            }
            log.info("用户离开房间: session={}, room={}, 在线人数={}", sessionId, roomId, getOnlineCount(roomId));
        }
        return roomId;
    }

    public int getOnlineCount(Long roomId) {
        Set<String> sessions = roomSessions.get(roomId);
        return sessions == null ? 0 : sessions.size();
    }
}
