# 直播竞拍全栈系统（dy_live_auction）

> 抖音电商 AI 全栈课题——「实时竞拍大师」

---

## 项目简介

一套完整的直播竞拍系统，支持主播发布拍品、配置竞拍规则、用户实时出价、自动成交、订单支付。

- **主播端**（PC 管理后台）：创建直播间 -> 发布拍品 -> 配置竞拍规则 -> 开始竞拍 -> 查看成交订单
- **用户端**（PC 浏览器）：浏览直播间 -> 查看实时价格 -> 出价 -> 实时排名 -> 支付订单
- **核心能力**：状态机管理竞拍生命周期 | Redis 分布式锁防重复出价 | WebSocket 毫秒级实时推送 | Redis 缓存加速读写 | 封顶价自动成交 | 延时机制

---

## 技术栈

| 层 | 技术 |
|----|------|
| 前端 | React 18 + TypeScript + Vite + Ant Design 5 + Zustand |
| 后端 | Java 17 + Spring Boot 2.7.18 + Maven 多模块 |
| ORM | MyBatis-Plus 3.5.5 |
| 数据库 | MySQL 8.0 |
| 缓存/锁 | Redis + Redisson 3.23（分布式锁 + Sorted Set 排行榜 + Cache-Aside） |
| 实时通信 | Spring WebSocket + STOMP + SockJS |
| 鉴权 | JWT（jjwt 0.11.5） |
| API 文档 | Knife4j |

---

## 依赖环境

| 软件 | 最低版本 | 说明 |
|------|---------|------|
| JDK | 17 | 运行后端 |
| Maven | 3.6+ | 构建后端 |
| Node.js | 18+ | 运行前端 |
| MySQL | 8.0 | 持久化数据 |
| Redis | 6.2+ | 分布式锁 + 缓存 + 排行榜 |

---

## 启动步骤

### 1. 初始化数据库
```bash
mysql -u root -p < docs/init.sql
```

### 2. 启动 Redis
```bash
redis-server
```
> 如果使用远程 Redis，修改 application.yml 中 redis.sdk.config.host 和 password。

### 3. 启动后端
```bash
cd server
mvn clean compile
mvn spring-boot:run
```
- 健康检查: http://localhost:8080/api/health
- API 文档: http://localhost:8080/doc.html

### 4. 启动前端

**管理后台**（:3000）：
```bash
cd frontend-admin
npm install
npm run dev
```

**用户端**（:4000）：
```bash
cd frontend-user
npm install
npm run dev
```

### 5. 使用流程

1. 打开 http://localhost:3000 -> 注册主播账号
2. 创建直播间 -> 开播
3. 发布竞拍 -> 开始竞拍
4. 打开 http://localhost:4000 -> 注册普通用户 -> 点进直播间
5. 出价 -> 等待竞拍结束 -> 查看订单

---

## 目录结构

```
dy_live_auction_weiba/
|
+-- server/                              # 后端（Spring Boot + Maven 多模块）
|   +-- pom.xml                          # 父 POM
|   +-- dy-common/                       # 公共模块：Result、异常、枚举
|   +-- dy-dao/                          # 持久层：实体、Mapper、MP 配置
|   +-- dy-service/                      # 业务层：状态机、出价校验、缓存、WS 广播
|   +-- dy-api/                          # 接口层：Controller、JWT、启动类
|       +-- src/main/resources/
|           +-- application.yml          # 应用配置
|
+-- frontend-admin/                      # 前端 - 主播 PC 管理后台
|   +-- src/
|   |   +-- api/                         # axios 封装 + 接口
|   |   +-- stores/                      # Zustand auth store
|   |   +-- pages/Login, Room, Auction, Order/
|   |   +-- types.ts
|   +-- vite.config.ts                   # proxy /api -> :8080
|
+-- frontend-user/                       # 前端 - 用户端
|   +-- src/
|   |   +-- api/                         # axios 封装 + 接口
|   |   +-- stores/                      # Zustand auth store
|   |   +-- pages/Login, Rooms, RoomDetail, Bids, Orders/
|   |   +-- types.ts
|   +-- index.html                       # CDN 引入 SockJS + StompJS
|   +-- vite.config.ts                   # proxy /api -> :8080
|
+-- docs/                                # 文档
|   +-- init.sql                         # 建库建表脚本
|   +-- database-design.md               # 数据库设计
|   +-- tech-selection.md                # 技术选型
|   +-- framework-design.md              # 框架设计
|   +-- state-machine-design.md          # 状态机设计
|   +-- sequence-diagrams.md             # UML 时序图 + WS 广播参考
|   +-- online-user-tracking.md          # 在线人数统计
|
+-- docker-compose.yml                   # Docker 一键部署
+-- Dockerfile.admin                     # 管理后台 Docker 镜像
+-- Dockerfile.user                      # 用户端 Docker 镜像
+-- server/Dockerfile                    # 后端 Docker 镜像
```

---

## 配置说明

### application.yml（server/dy-api/src/main/resources/）

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/dy_live_auction?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: 123456

# Redis（自定义前缀，手动创建 RedissonClient）
redis:
  sdk:
    config:
      host: localhost
      port: 6379
      password:
      pool-size: 10
      connect-timeout: 5000
      idle-timeout: 30000

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: auto
```

### vite.config.ts（前端代理）

```ts
server: {
  port: 3000,     // 管理后台 :3000, 用户端 :4000
  proxy: {
    '/api': 'http://localhost:8080',
    '/ws': { target: 'http://localhost:8080', ws: true },
  },
}
```

---

## 关键架构决策

| 决策 | 选择 | 理由 |
|------|------|------|
| 状态管理 | 状态模式（6 个状态类） | 新增状态不碰老代码 |
| 实时通信 | Spring STOMP + SockJS | 房间级隔离，千级并发够用 |
| 分布式锁 | Redisson RLock | 出价幂等性核心保障 |
| 缓存策略 | Redis Cache-Aside | 读多写少，Redis 不可用自动降级 DB |
| 排行榜 | Redis Sorted Set | O(logN) 查询 Top N |
| 在线人数 | 内存 ConcurrentHashMap | 简单可靠，WebSocket 事件驱动 |
| 鉴权 | JWT 过滤器 | 轻量，无 Spring Security 依赖 |
| ORM | MyBatis-Plus | 高并发场景精确 SQL 控制 |

---

## 系统架构

```
+------------------+  +------------------+
|  前端-管理后台    |  |  前端-用户端      |
|  React+Antd :3000|  |  React+Antd :4000|
+--------+---------+  +--------+---------+
         |                    |
         v                    v
+------------------+  +------------------+
|  REST API        |  |  WebSocket       |
|  JWT AuthFilter  |  |  STOMP Broker    |
+--------+---------+  +--------+---------+
         |                    |
         v                    v
+------------------------------------------+
|        业务服务层（dy-service）            |
|                                          |
|  AuctionService  OrderService            |
|  LiveRoomService NotificationService     |
|  AuthService     CacheService            |
|                                          |
|  AuctionStateMachine（状态模式 5 态）      |
|  AuctionWsBroadcaster（WebSocket 广播）    |
+--+----+----+----+----+----+----+----+----+
   |    |    |    |    |    |    |    |
   v    v    v    v    v    v    v    v
+------------+  +------------+  +------------+
|  MySQL 8.0 |  |  Redis     |  |  Spring    |
|  持久化存储  |  |  缓存 + 锁  |  |  Event     |
|            |  |            |  |  事件驱动    |
| 6 张业务表  |  | RLock 锁   |  | 订单 + 通知  |
|            |  | SortedSet  |  | 监听器       |
|            |  | 排行榜     |  |            |
+------------+  +------------+  +------------+
```

架构图详见 [docs/sequence-diagrams.md](docs/sequence-diagrams.md) 图零。

---

## Docker 部署

```bash
docker-compose up -d      # 一键启动
docker-compose down       # 停止
```
详见 docs/deploy/README.md
---
