package com.dy.liveauction.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dy.liveauction.dao.entity.AuctionItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper
public interface AuctionItemMapper extends BaseMapper<AuctionItem> {

    @Update("""
            UPDATE auction_items
            SET start_time = #{startTime},
                planned_end_time = #{plannedEndTime},
                actual_end_time = #{actualEndTime},
                status = 2
            WHERE id = #{itemId}
              AND status = 1
            """)
    int startIfPending(@Param("itemId") Long itemId,
                       @Param("startTime") LocalDateTime startTime,
                       @Param("plannedEndTime") LocalDateTime plannedEndTime,
                       @Param("actualEndTime") LocalDateTime actualEndTime);

    @Update("""
            UPDATE auction_items
            SET cancel_reason = #{reason},
                status = 5
            WHERE id = #{itemId}
              AND status IN (1, 2)
            """)
    int cancelIfOpen(@Param("itemId") Long itemId, @Param("reason") String reason);

    @Update("""
            UPDATE auction_items
            SET current_price = #{newPrice},
                current_bidder_id = #{userId},
                bid_count = bid_count + 1,
                actual_end_time = #{actualEndTime}
            WHERE id = #{itemId}
              AND status = 2
              AND current_price = #{oldPrice}
              AND actual_end_time > #{now}
            """)
    int updateBidIfCurrent(@Param("itemId") Long itemId,
                           @Param("oldPrice") BigDecimal oldPrice,
                           @Param("newPrice") BigDecimal newPrice,
                           @Param("userId") Long userId,
                           @Param("actualEndTime") LocalDateTime actualEndTime,
                           @Param("now") LocalDateTime now);

    @Update("""
            UPDATE auction_items
            SET status = #{newStatus},
                winner_id = #{winnerId}
            WHERE id = #{itemId}
              AND status = 2
            """)
    int finishIfActive(@Param("itemId") Long itemId,
                       @Param("newStatus") Integer newStatus,
                       @Param("winnerId") Long winnerId);
}
