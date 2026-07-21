package com.dy.liveauction.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dy.liveauction.dao.entity.Bid;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface BidMapper extends BaseMapper<Bid> {

    @Update("""
            UPDATE bids
            SET is_valid = 0
            WHERE item_id = #{itemId}
              AND is_valid = 1
            """)
    int invalidateActiveBids(@Param("itemId") Long itemId);
}
