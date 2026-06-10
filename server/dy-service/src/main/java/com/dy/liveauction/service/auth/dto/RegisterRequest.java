package com.dy.liveauction.service.auth.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String nickname;
    private Integer role;  // 1=主播, 2=用户
}
