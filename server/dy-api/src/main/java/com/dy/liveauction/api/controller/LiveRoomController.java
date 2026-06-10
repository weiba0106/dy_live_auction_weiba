package com.dy.liveauction.api.controller;

import com.dy.liveauction.common.result.Result;
import com.dy.liveauction.dao.entity.LiveRoom;
import com.dy.liveauction.service.room.LiveRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 直播间接口
 */
@RestController
@RequestMapping("/api/admin/room")
@RequiredArgsConstructor
public class LiveRoomController {

    private final LiveRoomService liveRoomService;

    /** 创建直播间 */
    @PostMapping
    public Result<LiveRoom> create(@RequestParam String title,
                                    @RequestParam(required = false) String coverImage,
                                    @RequestParam(required = false) String videoUrl,
                                    @RequestParam(required = false) String notice,
                                    HttpServletRequest request) {
        Long merchantId = (Long) request.getAttribute("userId");
        return Result.ok(liveRoomService.createRoom(merchantId, title, coverImage, videoUrl, notice));
    }

    /** 开播 */
    @PostMapping("/{id}/start")
    public Result<Void> start(@PathVariable Long id, HttpServletRequest request) {
        Long merchantId = (Long) request.getAttribute("userId");
        liveRoomService.startLive(id, merchantId);
        return Result.ok();
    }

    /** 下播 */
    @PostMapping("/{id}/stop")
    public Result<Void> stop(@PathVariable Long id, HttpServletRequest request) {
        Long merchantId = (Long) request.getAttribute("userId");
        liveRoomService.stopLive(id, merchantId);
        return Result.ok();
    }

    /** 我的直播间 */
    @GetMapping
    public Result<LiveRoom> myRoom(HttpServletRequest request) {
        Long merchantId = (Long) request.getAttribute("userId");
        return Result.ok(liveRoomService.getMyRoom(merchantId));
    }

    /** 所有直播中的直播间（用户端浏览用） */
    @GetMapping("/live")
    public Result<List<LiveRoom>> liveRooms() {
        return Result.ok(liveRoomService.getLiveRooms());
    }
}
