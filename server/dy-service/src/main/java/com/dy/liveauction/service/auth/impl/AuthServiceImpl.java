package com.dy.liveauction.service.auth.impl;

import cn.hutool.crypto.SecureUtil;
import com.dy.liveauction.common.exception.BizException;
import com.dy.liveauction.common.exception.ErrorCode;
import com.dy.liveauction.dao.entity.User;
import com.dy.liveauction.dao.mapper.UserMapper;
import com.dy.liveauction.service.auth.AuthService;
import com.dy.liveauction.service.auth.JwtUtils;
import com.dy.liveauction.service.auth.dto.LoginRequest;
import com.dy.liveauction.service.auth.dto.LoginResponse;
import com.dy.liveauction.service.auth.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getUsername, request.getUsername()));

        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }

        String hash = SecureUtil.sha256(request.getPassword());
        if (!hash.equals(user.getPasswordHash())) {
            throw new BizException(ErrorCode.PASSWORD_ERROR);
        }

        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(token, user.getId(), user.getNickname(), user.getRole());
    }

    @Override
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        // 检查用户名是否已存在
        User exist = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getUsername, request.getUsername()));
        if (exist != null) {
            throw new BizException(400, "用户名已被注册");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(SecureUtil.sha256(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setAvatar("");
        user.setRole(request.getRole() != null ? request.getRole() : 2);
        user.setBalance(new java.math.BigDecimal("100000.00"));  // 默认余额 10万
        userMapper.insert(user);

        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(token, user.getId(), user.getNickname(), user.getRole());
    }
}
