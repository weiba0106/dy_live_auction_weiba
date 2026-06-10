package com.dy.liveauction.service.room;

import com.dy.liveauction.dao.entity.LiveRoom;
import java.util.List;

public interface LiveRoomService {

    LiveRoom createRoom(Long merchantId, String title, String coverImage, String videoUrl, String notice);

    void startLive(Long roomId, Long merchantId);

    void stopLive(Long roomId, Long merchantId);

    LiveRoom getMyRoom(Long merchantId);

    List<LiveRoom> getLiveRooms();
}
