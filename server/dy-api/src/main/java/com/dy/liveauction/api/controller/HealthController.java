package com.dy.liveauction.api.controller;

import com.dy.liveauction.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查接口 —— 用于验证项目启动成功
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Result<String> health() {
        return Result.ok("ok");
    }
}
