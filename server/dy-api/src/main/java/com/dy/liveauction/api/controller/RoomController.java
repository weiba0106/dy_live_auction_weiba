package com.dy.liveauction.api.controller;

import com.dy.liveauction.common.result.Result;
import com.dy.liveauction.dao.entity.LiveRoom;
import com.dy.liveauction.service.cache.CacheService;
import com.dy.liveauction.service.room.LiveRoomService;
import com.dy.liveauction.service.websocket.OnlineUserTracker;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户端公开接口（无需登录）
 */
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final LiveRoomService liveRoomService;
    private final OnlineUserTracker onlineUserTracker;
    private final CacheService cacheService;

    /** 所有直播中的直播间（缓存 3 秒） */
    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        List<Map<String, Object>> rooms = cacheService.getOrLoad("cache:rooms:live",
                (Class<List<Map<String, Object>>>) (Class<?>) List.class,
                Duration.ofSeconds(3),
                () -> liveRoomService.getLiveRooms().stream().map(r -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", r.getId());
                    map.put("title", r.getTitle());
                    map.put("coverImage", r.getCoverImage());
                    map.put("videoUrl", r.getVideoUrl());
                    map.put("status", r.getStatus());
                    map.put("notice", r.getNotice());
                    map.put("online", onlineUserTracker.getOnlineCount(r.getId()));
                    return map;
                }).collect(Collectors.toList()));
        return Result.ok(rooms);
    }

    /** 直播间在线人数 */
    @GetMapping("/{roomId}/online")
    public Result<Map<String, Object>> online(@PathVariable Long roomId) {
        return Result.ok(Map.of("roomId", roomId, "online", onlineUserTracker.getOnlineCount(roomId)));
    }
}
