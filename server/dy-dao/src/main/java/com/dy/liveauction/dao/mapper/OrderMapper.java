package com.dy.liveauction.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dy.liveauction.dao.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Update("""
            UPDATE orders
            SET status = 2,
                paid_at = NOW(3)
            WHERE id = #{orderId}
              AND buyer_id = #{buyerId}
              AND status = 1
            """)
    int markPaidIfPending(@Param("orderId") Long orderId, @Param("buyerId") Long buyerId);
}
