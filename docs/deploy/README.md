# Docker 部署指南

## 前置条件

- Docker 20.10+
- Docker Compose 2.0+

## 一键部署

```bash
# 在项目根目录执行
docker-compose up -d
```

首次启动会：
1. 拉取 MySQL 8.0 / Redis 7 / OpenJDK 17 基础镜像
2. 自动建表（`docs/init.sql` 挂载到 MySQL 容器初始化目录）
3. 编译后端 Maven 项目（约 2-5 分钟）
4. 编译前端 npm 项目（约 1-2 分钟）
5. 启动所有服务

## 访问地址

| 服务 | 地址 |
|------|------|
| 主播管理后台 | http://localhost:3000 |
| 用户端 | http://localhost:4000 |
| 后端 REST API | http://localhost:8080 |
| API 文档 | http://localhost:8080/doc.html |

## 停止 / 重启

```bash
docker-compose down           # 停止并删除容器
docker-compose down -v        # 同时删除数据库卷（数据丢失）
docker-compose restart        # 重启所有服务
docker-compose up -d --build  # 重新构建并启动
```

## 查看日志

```bash
docker-compose logs -f backend          # 后端日志
docker-compose logs -f frontend-user    # 用户端日志
docker-compose logs -f                 # 全部日志
```

## 服务依赖关系

```
MySQL ──┬── backend
Redis ──┘      │
          ┌────┴────┐
          ▼         ▼
    frontend-admin  frontend-user
```

后端通过 `SPRING_DATASOURCE_URL` 环境变量连接 `mysql:3306`，通过 `REDIS_SDK_CONFIG_HOST=redis` 连接 Redis。

## 自定义配置

修改 `docker-compose.yml` 中的环境变量：

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `SPRING_DATASOURCE_URL` | MySQL 连接串 | `jdbc:mysql://mysql:3306/dy_live_auction?...` |
| `SPRING_DATASOURCE_USERNAME` | MySQL 用户名 | `root` |
| `SPRING_DATASOURCE_PASSWORD` | MySQL 密码 | `123456` |
| `REDIS_SDK_CONFIG_HOST` | Redis 地址 | `redis` |
| `REDIS_SDK_CONFIG_PASSWORD` | Redis 密码 | 空 |
| `TZ` | 时区 | `Asia/Shanghai` |
