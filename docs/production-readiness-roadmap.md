# 直播竞拍系统生产化评估与改造路线

本文档基于两个改造分支：

- `codex/production-bidding-hardening`：DB CAS、拍品级锁、资金冻结、订单幂等。
- `codex/redis-lua-bid-adjudication`：Redis Lua 实时出价裁决、Redis Stream 事件留痕、MySQL 同步账本。

结论先行：当前代码已经具备“真实业务架构雏形”，但还不能直接称为完整生产级竞拍系统。它可以支撑课程答辩、技术展示、小流量压测和方案验证；若要用于高并发真实交易，还需要补齐事件消费、资金流水、对账恢复、结束裁决、风控限流和可观测性。

## 1. 当前系统已经解决的问题

### 1.1 出价并发正确性

上一版原始流程里，不同用户同时对同一拍品出价时，只通过 `lock:bid:{itemId}:{userId}` 做用户维度锁，无法保护同一拍品的价格线性推进。

现已改造为两层能力：

- DB-CAS 路径：同一拍品用 `lock:bid:{itemId}` 串行化，并用 SQL 条件保护 `status=2`、`current_price=oldPrice`、`actual_end_time>now`。
- Redis-Lua 路径：出价由 Lua 在 Redis 内原子判断和更新，避免 Java 端“先读后写”的竞态。

### 1.2 资金占用

原始项目只校验 `users.balance >= amount`，无法防止用户同时在多个拍品上超额出价。

现已加入：

- `users.frozen_balance`。
- 出价成功前冻结新最高出价金额。
- 被超越后释放旧最高价冻结金额。
- 成交支付时从冻结金额结算，而不是再次扣可用余额。

这让“当前最高出价对应资金已被占用”成为系统不变量。

### 1.3 订单幂等

原始项目在竞拍结束事件触发时直接插入订单，多次结束、重复事件、多节点定时器都可能生成重复订单。

现已加入：

- `orders.item_id` 唯一约束。
- `createOrder` 先查已有订单，插入遇到唯一键冲突后再次查询。
- 支付时使用 `status=1` 条件更新，避免重复支付状态变更。

### 1.4 Redis 实时裁决雏形

当前 Redis Lua 分支已实现：

- `auction:{itemId}:state` 保存实时状态。
- `auction:{itemId}:ranking` 保存实时排行榜。
- `auction:{itemId}:bidders` 保存参与用户。
- `auction:bid:stream` 记录成功出价事件。
- `auction:end:zset` 维护实际结束时间。
- Lua 原子校验状态、结束时间、当前最高价、加价幅度、封顶价、重复最高价用户、自动延时和封顶成交。

金额在 Lua 内以“分”为单位计算，避免浮点精度问题。

## 2. 当前仍不是生产完备的原因

### 2.1 Redis Stream 还只是事件留痕

Lua 已经写入 `auction:bid:stream`，但当前 Java 代码仍在请求线程里同步写 MySQL 账本。

真实高并发下，请求链路应尽量短：

```text
用户出价 -> 冻结资金 -> Redis Lua 裁决 -> 返回/广播
                           ↓
                     Stream/MQ 异步落库
```

现在缺少：

- `XGROUP CREATE` 初始化消费者组。
- `XREADGROUP` 消费 `auction:bid:stream`。
- `XACK` 确认消费。
- Pending List 重试。
- 死信队列。
- 消费者重启恢复。
- 消费端幂等。

### 2.2 出价事件缺少强幂等字段

生产系统必须假设事件会重复、乱序、延迟。

当前 `bids` 表还没有：

- `request_id`：客户端或服务端生成的请求唯一号。
- `redis_version`：Redis 拍品状态版本。
- `event_id`：Stream 消息 ID 或业务事件 ID。

建议新增：

```sql
ALTER TABLE bids
    ADD COLUMN request_id VARCHAR(64) DEFAULT NULL,
    ADD COLUMN redis_version BIGINT DEFAULT NULL,
    ADD UNIQUE KEY uk_request_id (request_id),
    ADD UNIQUE KEY uk_item_version (item_id, redis_version);
```

有了这些字段，消费者重复处理同一事件时，可以安全跳过。

### 2.3 资金系统还只是简化模型

当前资金冻结直接更新 `users.balance` 和 `users.frozen_balance`。这适合教学项目，但不适合真实交易审计。

真实业务至少需要资金流水表：

```sql
CREATE TABLE fund_flows (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    flow_no VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT UNSIGNED NOT NULL,
    item_id BIGINT UNSIGNED DEFAULT NULL,
    order_id BIGINT UNSIGNED DEFAULT NULL,
    amount DECIMAL(12,2) NOT NULL,
    type TINYINT NOT NULL COMMENT '1:冻结, 2:解冻, 3:结算扣款, 4:退款',
    status TINYINT NOT NULL COMMENT '1:处理中, 2:成功, 3:失败',
    biz_no VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_biz_type (biz_no, type),
    INDEX idx_user_time (user_id, created_at)
);
```

资金相关动作必须满足：

- 每次冻结、解冻、结算都有流水。
- 资金动作可重试。
- 重复事件不会重复扣款或重复解冻。
- 用户余额可以通过流水重算和审计。

### 2.4 竞拍结束还没有完全 Redis 化

当前 Lua 出价会维护 `auction:end:zset`，但结束任务仍未完整迁移为 Redis 原子结束。

生产建议：

```text
定时器扫描 auction:end:zset
  ↓
取 score <= now 的 itemId
  ↓
Lua 二次确认 status=ACTIVE 且 actualEndTime<=now
  ↓
Redis 原子改为 SOLD/ENDED
  ↓
写 AUCTION_ENDED 事件
  ↓
消费者落 MySQL、生成订单、通知用户
```

关键点是“二次确认”。不能只因为 ZSet 到期就结束，因为最后几秒出价可能已经延时。

### 2.5 Redis 恢复和对账缺失

当 Redis 重启、主从切换或数据丢失时，竞拍实时状态可能不完整。

生产系统需要恢复策略：

- Redis 开启 AOF，至少 `appendfsync everysec`。
- 服务启动时扫描 MySQL 中 `status=2` 的拍品，重建 `auction:{itemId}:state`。
- 重放未确认的 Stream 事件，补齐 MySQL 账本。
- 对比 Redis 当前价与 MySQL 最新价，发现差异后报警或自动修复。

需要定期对账：

```text
Redis currentPrice vs MySQL auction_items.current_price
Redis currentBidderId vs MySQL auction_items.current_bidder_id
MySQL SOLD 拍品是否有 orders
orders.final_price 是否等于 auction_items.current_price
users.frozen_balance 是否等于未结算最高出价之和
bids 当前有效记录是否只有一条
```

### 2.6 WebSocket 鉴权不完整

当前已经禁止 WebSocket 出价从 payload 读取 `userId`，改为从 `Principal` 获取。

但还缺 STOMP 握手鉴权：

- CONNECT 帧解析 JWT。
- 校验 token。
- 将 `userId` 写入 `Principal`。
- 拒绝匿名连接发送 `/app/bid`。

否则 WebSocket 出价通道不能作为生产主入口。当前生产主入口仍应使用 REST 出价。

### 2.7 风控和限流缺失

真实直播竞拍一定会遇到脚本刷价、恶意抬价、撞库账号和异常设备。

至少需要：

- 用户维度限流：例如同一用户每秒最多 N 次出价。
- IP 维度限流。
- 设备指纹维度限流。
- 同一拍品重复无效出价限制。
- 黑名单/灰名单。
- 异常价格拦截：远超合理区间、频繁触发封顶、短时间多账号同设备。
- 商家/买家关联关系风控。

### 2.8 可观测性不足

生产环境需要指标、日志和告警：

- 出价成功 QPS、失败 QPS。
- Redis Lua 耗时 P50/P95/P99。
- MySQL 落库耗时。
- Stream Pending 数量。
- 订单生成失败数。
- 资金冻结失败数。
- Redis 与 MySQL 对账差异数。
- WebSocket 广播失败数。
- 单拍品热点排行。

没有这些指标，系统即使跑起来，也很难在故障时判断问题在哪里。

## 3. 推荐生产化路线

### 阶段一：稳态交易闭环

目标：适合小规模真实业务。

已基本完成：

- DB-CAS 出价。
- 拍品级锁。
- 资金冻结。
- 订单幂等。
- 取消/结束状态条件更新。

建议补充：

- 资金流水表。
- `bids` 幂等字段。
- 更完整的异常提示。
- 基础限流。

### 阶段二：Redis Lua 热点裁决

目标：热门拍品出价实时裁决。

已完成：

- Lua 原子裁决。
- Redis 实时状态。
- Redis Stream 事件留痕。
- MySQL 同步账本。

建议补充：

- 出价请求 `request_id`。
- Redis Lua 脚本版本管理。
- Lua 单元测试/集成测试。
- Redis 不可用时的明确降级策略。

### 阶段三：异步事件账本

目标：缩短用户请求链路，削峰落库。

需要实现：

- `BidEventConsumer`。
- Redis Stream 消费者组。
- Pending 重试和死信。
- 消费幂等。
- 批量落库或限速落库。
- 事件处理失败告警。

请求链路变为：

```text
冻结资金 -> Lua 裁决 -> 返回结果
                 ↓
          Stream 消费者落 MySQL
```

### 阶段四：Redis 原子结束

目标：解决自动延时和多节点结束竞态。

需要实现：

- `auction:end:zset` 到期扫描。
- 结束 Lua 脚本。
- `AUCTION_SOLD` / `AUCTION_ENDED` 事件。
- 订单消费者。
- 通知消费者。

### 阶段五：恢复、对账、风控和监控

目标：接近生产可运维状态。

需要实现：

- Redis 状态重建。
- Stream 事件重放。
- MySQL/Redis 对账任务。
- 资金对账。
- 用户/IP/设备限流。
- 风控规则。
- Prometheus 指标或等价监控。
- 告警策略。

## 4. 当前版本适用边界

### 适合

- 课程答辩。
- 技术展示。
- 单机或小规模压测。
- 验证 Redis Lua 裁决方案。
- 作为真实业务第一阶段的原型。

### 不适合直接用于

- 大促级直播间。
- 真实资金交易。
- 多机房部署。
- 对账审计严格的交易系统。
- Redis 高可用和数据恢复未配置的环境。

## 5. 下一步最值得做的三件事

如果继续推进，优先级建议如下：

1. 加 `bids.request_id` 和 `bids.redis_version`，让出价事件具备幂等基础。
2. 实现 `auction:bid:stream` 消费者，把同步 MySQL 账本逐步迁到异步事件链路。
3. 新增资金流水表，把冻结、解冻、结算从简单余额字段升级为可审计资金账本。

这三件事做完，系统才真正从“高性能裁决 PoC”走向“真实业务交易系统”。
