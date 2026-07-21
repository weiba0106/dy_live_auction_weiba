# 出价链路生产化详细设计

本文档说明直播竞拍系统在真实业务中的出价链路设计。目标是保证：

- 价格不会被并发覆盖。
- 最高出价人唯一且可追溯。
- 每次有效出价都有资金占用。
- 被超越用户资金及时释放。
- 竞拍延时和封顶成交不会产生竞态。
- Redis、MySQL、消息事件之间最终一致。

## 1. 业务核心不变量

真实竞拍系统最重要的是维护几个不变量：

```text
同一拍品的有效出价必须线性递增。
同一拍品任意时刻最多只有一个当前最高出价人。
当前最高出价人的保证金/出价资金必须处于冻结状态。
被超越用户的冻结金额必须释放或转为其他明确状态。
成交订单金额必须等于最终最高有效出价。
同一拍品最多生成一张成交订单。
```

所有并发控制、Lua 脚本、数据库约束、资金流水和补偿任务都围绕这些不变量设计。

## 2. 当前两种出价模式

系统建议保留两种模式：

### 2.1 DB-CAS 模式

适合普通拍品、小规模真实业务、Redis 故障降级或灰度前期。

流程：

```text
用户出价
  ↓
参数校验
  ↓
获取拍品级锁 lock:bid:{itemId}
  ↓
锁内重查 auction_items
  ↓
校验状态、当前价、结束时间、加价幅度、封顶价
  ↓
冻结用户资金
  ↓
UPDATE auction_items ... WHERE status=2 AND current_price=oldPrice
  ↓
释放旧最高出价人冻结资金
  ↓
插入 bids
  ↓
广播 WebSocket
```

关键 SQL：

```sql
UPDATE auction_items
SET current_price = :newPrice,
    current_bidder_id = :userId,
    bid_count = bid_count + 1,
    actual_end_time = :actualEndTime
WHERE id = :itemId
  AND status = 2
  AND current_price = :oldPrice
  AND actual_end_time > :now;
```

影响行数为 1 才代表出价成功。影响行数为 0 说明状态或价格已经变化，必须让用户刷新后重试。

### 2.2 Redis Lua 裁决模式

适合热门拍品、大促直播间、高并发瞬时出价。

流程：

```text
用户出价
  ↓
冻结资金
  ↓
Redis Lua 原子裁决
  ↓
返回最新价、旧最高出价人、是否延时、是否成交、版本号
  ↓
释放旧最高出价人冻结资金
  ↓
写 bids 账本
  ↓
同步 auction_items 快照
  ↓
广播 WebSocket
```

Lua 负责原子判断：

```text
拍品状态是否竞拍中
是否超过结束时间
是否已经是当前最高出价人
是否满足加价幅度
是否超过封顶价
是否触发自动延时
是否达到封顶成交
```

Redis 内使用“分”为单位保存金额：

```text
100.25 元 -> 10025 分
```

避免 Lua 浮点计算带来的精度问题。

## 3. Redis Key 设计

```text
auction:{itemId}:state
auction:{itemId}:ranking
auction:{itemId}:bidders
auction:bid:stream
auction:end:zset
```

### 3.1 auction:{itemId}:state

Hash：

```text
itemId
status
currentPriceCents
currentBidderId
bidCount
incrementCents
maxPriceCents
actualEndTimeMs
delaySeconds
version
```

`version` 每次成功出价递增，用于 MySQL 回写和事件消费幂等。

### 3.2 auction:{itemId}:ranking

ZSet：

```text
score = 出价金额分
member = userId
```

用于实时排行榜展示。排行榜不是账本，不能替代 `bids` 表。

### 3.3 auction:bid:stream

Stream 记录成功出价事件：

```text
event = BID_ACCEPTED
requestId
itemId
userId
amountCents
oldPriceCents
oldBidderId
bidCount
actualEndTimeMs
delayed
sold
version
```

生产环境中，MySQL 落库、订单生成、通知等应逐步迁移为 Stream 消费。

## 4. 资金冻结与出价的关系

真实业务中，出价不是只比较价格，还必须占用资金。

推荐原则：

```text
出价裁决前先冻结资金。
Lua 裁决失败则事务回滚冻结。
Lua 裁决成功则新最高出价资金保持冻结。
旧最高出价人被超越后释放旧冻结。
成交后冻结金额转为支付抵扣或履约保证。
```

如果没有冻结资金，用户可能在多个拍品同时高价出价，最终无法履约。

## 5. 保证金与全额冻结的区别

竞拍业务常见两种资金模型。

### 5.1 全额冻结

用户每次最高出价都冻结完整出价金额。

优点：

- 成交后履约风险最低。
- 支付时可以直接从冻结金额结算。

缺点：

- 用户资金占用大。
- 会降低参与率。

### 5.2 保证金冻结

用户参拍前冻结固定保证金，例如 100 元或起拍价的 10%。

优点：

- 用户参与门槛低。
- 更贴近真实拍卖业务。

缺点：

- 成交后仍需要用户支付尾款。
- 支付超时、保证金罚没、退款规则更复杂。

如果后续引入“保证金”，建议从当前 `frozen_balance` 升级为独立保证金流水和冻结单，而不是继续只改用户余额字段。

## 6. 推荐表结构扩展

### 6.1 bids 增加幂等字段

```sql
ALTER TABLE bids
    ADD COLUMN request_id VARCHAR(64) DEFAULT NULL,
    ADD COLUMN redis_version BIGINT DEFAULT NULL,
    ADD COLUMN event_id VARCHAR(64) DEFAULT NULL,
    ADD UNIQUE KEY uk_request_id (request_id),
    ADD UNIQUE KEY uk_item_version (item_id, redis_version);
```

作用：

- 防重复请求。
- 防 Stream 重复消费。
- 防旧事件覆盖新状态。

### 6.2 资金冻结单

```sql
CREATE TABLE fund_freezes (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    freeze_no VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT UNSIGNED NOT NULL,
    item_id BIGINT UNSIGNED NOT NULL,
    order_id BIGINT UNSIGNED DEFAULT NULL,
    amount DECIMAL(12,2) NOT NULL,
    type TINYINT NOT NULL COMMENT '1:出价冻结, 2:保证金冻结',
    status TINYINT NOT NULL COMMENT '1:冻结中, 2:已释放, 3:已结算, 4:已罚没',
    biz_no VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_biz_type (biz_no, type),
    INDEX idx_user_status (user_id, status),
    INDEX idx_item_status (item_id, status)
);
```

### 6.3 资金流水

```sql
CREATE TABLE fund_flows (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    flow_no VARCHAR(64) NOT NULL UNIQUE,
    freeze_no VARCHAR(64) DEFAULT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    item_id BIGINT UNSIGNED DEFAULT NULL,
    order_id BIGINT UNSIGNED DEFAULT NULL,
    amount DECIMAL(12,2) NOT NULL,
    direction TINYINT NOT NULL COMMENT '1:入账, 2:出账',
    type TINYINT NOT NULL COMMENT '1:冻结, 2:解冻, 3:结算, 4:罚没, 5:退款',
    status TINYINT NOT NULL COMMENT '1:处理中, 2:成功, 3:失败',
    biz_no VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_biz_type (biz_no, type),
    INDEX idx_user_time (user_id, created_at)
);
```

## 7. 出价成功后的事件处理

生产推荐最终链路：

```text
Lua 成功
  ↓
XADD auction:bid:stream
  ↓
BidEventConsumer 消费
  ↓
幂等插入 bids
  ↓
更新 auction_items 快照
  ↓
如果 sold=true，发布成交事件
  ↓
OrderConsumer 生成订单
  ↓
NotificationConsumer 通知用户
```

当前分支仍同步写 MySQL，这是为了降低一次性改造风险。后续可以逐步把同步写账本迁到消费者中。

## 8. 自动延时

规则：

```text
如果距离 actualEndTime 剩余秒数 <= delaySeconds
则 actualEndTime += delaySeconds
```

延时必须在裁决层原子完成。否则可能出现：

```text
线程 A 判断未延时
线程 B 判断延时
定时任务按旧时间结束
```

Redis Lua 中更新 `actualEndTimeMs` 的同时，也要更新：

```text
auction:end:zset score = newActualEndTimeMs
```

## 9. 封顶价成交

达到封顶价时：

```text
Lua 原子把 status 从 ACTIVE 改为 SOLD
从 auction:end:zset 移除 itemId
写 BID_ACCEPTED 事件，sold=1
后续消费者生成订单
```

注意：订单生成必须幂等，不能因为多个事件、多个节点、重复消费生成多张订单。

## 10. Redis 故障策略

真实业务不要悄悄切换裁判。

推荐策略：

```text
Redis Lua 模式开启时，Redis 不可用 -> 暂停热门拍品出价，提示系统繁忙
普通拍品可配置走 DB-CAS
不同拍品可以按 itemId 或 roomId 灰度选择引擎
```

不推荐同一个拍品在故障期间自动从 Redis 裁决切到 MySQL 裁决，因为可能出现两个实时真相。

## 11. 对账任务

建议每分钟或每几分钟执行：

```text
Redis 当前价 vs MySQL 当前价
Redis 当前最高出价人 vs MySQL 当前最高出价人
bids 当前有效出价是否只有一条
当前最高出价人是否存在冻结单
已成交拍品是否有订单
订单金额是否等于最终成交价
```

发现差异：

- 小差异可自动修复。
- 涉及资金和订单的差异必须报警并保留审计记录。

## 12. 用户侧体验

出价失败常见提示：

```text
当前价格已变化，请重新出价
竞拍已结束
您已是当前最高出价者，无需重复出价
出价低于最低加价
余额不足，请先充值或释放其他冻结
系统繁忙，请稍后重试
```

出价成功：

```text
出价成功，当前最高价 ¥xxx
```

被超越：

```text
您的出价已被超越，冻结资金已释放
```

## 13. 当前代码与生产目标差距

当前分支已经实现：

- Redis Lua 裁决。
- Redis 实时状态。
- Redis Stream 事件留痕。
- 同步 MySQL 账本。
- DB-CAS 回退路径。

仍需补充：

- Stream 消费者。
- 出价请求幂等字段。
- 资金冻结单和资金流水。
- Redis 状态恢复。
- 出价对账任务。
- Redis 原子结束任务。
- 风控和限流。
