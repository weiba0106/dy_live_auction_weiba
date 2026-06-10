package com.dy.liveauction.service.auction.dto;

import com.dy.liveauction.dao.entity.AuctionItem;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AuctionDetailVO {
    private Long id;
    private Long roomId;
    private String name;
    private String description;
    private String images;
    private BigDecimal startPrice;
    private BigDecimal incrementAmount;
    private BigDecimal maxPrice;
    private Integer durationMinutes;
    private Integer delaySeconds;
    private BigDecimal currentPrice;
    private String currentBidderName;
    private Integer bidCount;
    private LocalDateTime startTime;
    private LocalDateTime plannedEndTime;
    private LocalDateTime actualEndTime;
    private Integer status;
    private String statusDesc;

    public static AuctionDetailVO from(AuctionItem item) {
        AuctionDetailVO vo = new AuctionDetailVO();
        vo.setId(item.getId());
        vo.setRoomId(item.getRoomId());
        vo.setName(item.getName());
        vo.setDescription(item.getDescription());
        vo.setImages(item.getImages());
        vo.setStartPrice(item.getStartPrice());
        vo.setIncrementAmount(item.getIncrementAmount());
        vo.setMaxPrice(item.getMaxPrice());
        vo.setDurationMinutes(item.getDurationMinutes());
        vo.setDelaySeconds(item.getDelaySeconds());
        vo.setCurrentPrice(item.getCurrentPrice());
        vo.setBidCount(item.getBidCount());
        vo.setStartTime(item.getStartTime());
        vo.setPlannedEndTime(item.getPlannedEndTime());
        vo.setActualEndTime(item.getActualEndTime());
        vo.setStatus(item.getStatus());
        vo.setStatusDesc(statusText(item.getStatus()));
        return vo;
    }

    private static String statusText(int status) {
        switch (status) {
            case 1: return "待开始";
            case 2: return "竞拍中";
            case 3: return "已结束(流拍)";
            case 4: return "已成交";
            case 5: return "已取消";
            default: return "未知";
        }
    }
}
