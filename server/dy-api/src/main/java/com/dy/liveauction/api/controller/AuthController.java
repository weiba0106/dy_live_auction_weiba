package com.dy.liveauction.api.controller;

import com.dy.liveauction.common.result.Result;
import com.dy.liveauction.service.auth.AuthService;
import com.dy.liveauction.service.auth.dto.LoginRequest;
import com.dy.liveauction.service.auth.dto.LoginResponse;
import com.dy.liveauction.service.auth.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @PostMapping("/register")
    public Result<LoginResponse> register(@RequestBody RegisterRequest request) {
        return Result.ok(authService.register(request));
    }
}
