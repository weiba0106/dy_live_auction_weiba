package com.dy.liveauction.service.auth;

import com.dy.liveauction.service.auth.dto.LoginRequest;
import com.dy.liveauction.service.auth.dto.LoginResponse;
import com.dy.liveauction.service.auth.dto.RegisterRequest;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    LoginResponse register(RegisterRequest request);
}
