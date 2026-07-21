# Redis Lua 出价裁决改造说明

本分支在上一版 `DB CAS + 资金冻结` 的基础上，引入 Redis Lua 作为热门拍品的实时裁决层。

## 已落地能力

- 启动竞拍后，拍品运行态写入 `auction:{itemId}:state`。
- 出价时由 Lua 原子校验：
  - 竞拍状态必须为进行中。
  - 当前时间不能超过实际结束时间。
  - 出价人不能已经是当前最高出价者。
  - 出价必须满足 `currentPrice + incrementAmount`。
  - 出价不能超过封顶价。
  - 结束前 `delaySeconds` 内出价会自动延时。
  - 达到封顶价会把 Redis 状态原子改为已成交。
- Lua 使用“分”为单位比较金额，避免浮点精度误差。
- Lua 成功后会写入 Redis Stream：`auction:bid:stream`，作为后续异步落库/补偿的事件来源。
- Java 侧保留 MySQL 同步写账本：
  - 冻结新最高出价金额。
  - 释放被超越用户冻结金额。
  - 插入 `bids` 记录。
  - 按“更高价才更新”同步 `auction_items` 快照，避免旧事件覆盖新价格。
- 配置开关：

```yaml
auction:
  bid:
    redis-lua-enabled: true
```

关闭后会回到上一版 DB-CAS 出价流程。

## Redis Key

```text
auction:{itemId}:state        Hash，拍品实时状态
auction:{itemId}:ranking      ZSet，实时排行榜
auction:{itemId}:bidders      Set，参与用户
auction:bid:stream            Stream，成功出价事件
auction:end:zset              ZSet，按实际结束时间排序的拍品
```

## 后续建议

当前分支已经把裁决放到 Redis Lua，但 MySQL 账本仍同步写入。真实大促形态建议继续补：

- `auction:bid:stream` 消费者，支持失败重试和 Pending 事件恢复。
- 出价事件幂等字段，如 `bids.request_id` 或 `bids.redis_version`。
- Redis 与 MySQL 定时对账任务。
- 基于 `auction:end:zset` 的 Lua 原子结束任务。
- STOMP 握手阶段 JWT 鉴权，完整启用 WebSocket 出价。
