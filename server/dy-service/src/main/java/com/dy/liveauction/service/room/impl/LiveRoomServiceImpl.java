package com.dy.liveauction.service.room.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dy.liveauction.common.exception.BizException;
import com.dy.liveauction.dao.entity.LiveRoom;
import com.dy.liveauction.dao.mapper.LiveRoomMapper;
import com.dy.liveauction.service.room.LiveRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LiveRoomServiceImpl implements LiveRoomService {

    private final LiveRoomMapper liveRoomMapper;

    @Override
    @Transactional
    public LiveRoom createRoom(Long merchantId, String title, String coverImage, String videoUrl, String notice) {
        LiveRoom exist = liveRoomMapper.selectOne(
                new LambdaQueryWrapper<LiveRoom>().eq(LiveRoom::getMerchantId, merchantId));
        if (exist != null) {
            throw new BizException(400, "您已创建过直播间，不能重复创建");
        }
        LiveRoom room = new LiveRoom();
        room.setMerchantId(merchantId);
        room.setTitle(title != null ? title : "我的直播间");
        room.setCoverImage(coverImage != null ? coverImage : "");
        room.setVideoUrl(videoUrl != null ? videoUrl : "");
        room.setNotice(notice != null ? notice : "");
        room.setStatus(1);  // 未开播
        liveRoomMapper.insert(room);
        return room;
    }

    @Override
    @Transactional
    public void startLive(Long roomId, Long merchantId) {
        LiveRoom room = mustGetMyRoom(roomId, merchantId);
        if (room.getStatus() == 2) {
            throw new BizException(400, "直播间已在直播中");
        }
        room.setStatus(2);
        liveRoomMapper.updateById(room);
    }

    @Override
    @Transactional
    public void stopLive(Long roomId, Long merchantId) {
        LiveRoom room = mustGetMyRoom(roomId, merchantId);
        if (room.getStatus() != 2) {
            throw new BizException(400, "直播间未在直播中");
        }
        room.setStatus(3);
        liveRoomMapper.updateById(room);
    }

    @Override
    public LiveRoom getMyRoom(Long merchantId) {
        return liveRoomMapper.selectOne(
                new LambdaQueryWrapper<LiveRoom>().eq(LiveRoom::getMerchantId, merchantId));
    }

    @Override
    public List<LiveRoom> getLiveRooms() {
        return liveRoomMapper.selectList(
                new LambdaQueryWrapper<LiveRoom>().eq(LiveRoom::getStatus, 2));
    }

    private LiveRoom mustGetMyRoom(Long roomId, Long merchantId) {
        LiveRoom room = liveRoomMapper.selectById(roomId);
        if (room == null) {
            throw new BizException(404, "直播间不存在");
        }
        if (!room.getMerchantId().equals(merchantId)) {
            throw new BizException(403, "无权操作此直播间");
        }
        return room;
    }
}
