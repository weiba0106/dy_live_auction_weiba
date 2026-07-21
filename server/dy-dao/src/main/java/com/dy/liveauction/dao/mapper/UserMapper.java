package com.dy.liveauction.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dy.liveauction.dao.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Update("""
            UPDATE users
            SET balance = balance - #{amount},
                frozen_balance = frozen_balance + #{amount}
            WHERE id = #{userId}
              AND balance >= #{amount}
            """)
    int freezeBidFunds(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    @Update("""
            UPDATE users
            SET balance = balance + #{amount},
                frozen_balance = frozen_balance - #{amount}
            WHERE id = #{userId}
              AND frozen_balance >= #{amount}
            """)
    int releaseBidFunds(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    @Update("""
            UPDATE users
            SET frozen_balance = frozen_balance - #{amount}
            WHERE id = #{userId}
              AND frozen_balance >= #{amount}
            """)
    int settleFrozenFunds(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}
