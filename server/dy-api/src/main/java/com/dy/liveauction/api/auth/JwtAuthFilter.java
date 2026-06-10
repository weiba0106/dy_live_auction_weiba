package com.dy.liveauction.api.auth;

import com.dy.liveauction.service.auth.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT 鉴权过滤器 —— 拦截所有请求，验证 token
 *
 * 白名单路径不走鉴权（登录、注册、健康检查、WebSocket）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements Filter {

    private final JwtUtils jwtUtils;

    private static final String[] WHITELIST = {
            "/api/auth/login",
            "/api/auth/register",
            "/api/rooms",
            "/api/room/",
            "/api/health",
            "/ws"
    };

    /**
     * 只对 GET 请求放行白名单的路径前缀 ——
     * POST/PUT/DELETE 等写操作必须走 JWT 鉴权
     */
    private static final String[] GET_ONLY_WHITELIST = {
            "/api/auction/",
    };

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getRequestURI();

        // 白名单直接放行
        for (String wl : WHITELIST) {
            if (path.startsWith(wl)) {
                chain.doFilter(request, response);
                return;
            }
        }

        // GET-only 白名单：只对 GET 请求放行，写操作必须走鉴权
        for (String wl : GET_ONLY_WHITELIST) {
            if (path.startsWith(wl)) {
                if ("GET".equalsIgnoreCase(req.getMethod())) {
                    chain.doFilter(request, response);
                    return;
                }
                // POST/PUT/DELETE 等写操作不走白名单，继续往下到 JWT 校验
                break;
            }
        }

        // Swagger / Knife4j 文档放行
        if (path.contains("swagger") || path.contains("doc.html") || path.contains("v3/api-docs")) {
            chain.doFilter(request, response);
            return;
        }

        // 校验 token
        String token = extractToken(req);
        if (token == null || !jwtUtils.validateToken(token)) {
            res.setContentType("application/json;charset=UTF-8");
            res.setStatus(401);
            res.getWriter().write("{\"code\":401,\"message\":\"未登录或 token 已过期\"}");
            return;
        }

        // 将 userId 和 role 写入 request attribute，Controller 中可以直接取
        try {
            var claims = jwtUtils.parseToken(token);
            req.setAttribute("userId", Long.valueOf(claims.getSubject()));
            req.setAttribute("role", claims.get("role", Integer.class));
        } catch (Exception e) {
            res.setStatus(401);
            res.getWriter().write("{\"code\":401,\"message\":\"token 无效\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
