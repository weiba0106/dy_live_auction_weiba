# 支付临界状态与保证金处理详细设计

本文档说明竞拍成交后，订单支付快到截止时间时，支付成功、支付失败、支付超时、渠道回调延迟等临界状态应该如何处理，以及保证金应该如何释放、抵扣或罚没。

核心原则：

```text
订单状态不能只看本地接口结果。
支付结果以支付渠道最终结果为准。
保证金处理必须走幂等资金流水。
超时关闭必须二次确认支付渠道。
支付成功和超时关闭用 CAS 抢状态。
任何重复回调、重复任务、重复点击都不能重复扣款或重复退钱。
```

## 1. 业务背景

竞拍成交后，用户通常需要在规定时间内完成尾款支付。比如：

```text
成交时间：14:00:00
支付截止：14:15:00
保护期：14:15:00 - 14:18:00
```

用户可能在 `14:14:59` 发起支付，支付渠道在 `14:15:02` 才返回成功。此时如果系统在 `14:15:00` 立刻关闭订单并罚没保证金，就会出现严重争议。

所以真实业务必须引入：

- 支付确认保护期。
- 支付渠道主动查询。
- 订单中间态。
- 保证金中间态。
- 幂等资金流水。
- 超时任务和补偿任务。

## 2. 订单状态机

推荐订单状态：

```text
WAIT_PAY        待支付
PAYING          支付处理中
PAY_CONFIRMING  支付确认中
PAID            已支付
CLOSE_PENDING   关闭确认中
CLOSED          已关闭
REFUNDING       退款中
REFUNDED        已退款
```

状态说明：

| 状态 | 含义 | 用户侧展示 |
| --- | --- | --- |
| WAIT_PAY | 订单已生成，用户尚未发起支付 | 待支付 |
| PAYING | 用户已发起支付，等待渠道结果 | 支付处理中 |
| PAY_CONFIRMING | 支付截止附近或之后，系统正在确认结果 | 支付确认中 |
| PAID | 渠道确认成功，本地入账完成 | 支付成功 |
| CLOSE_PENDING | 超时任务准备关闭，但还要二次查渠道 | 关闭确认中/支付确认中 |
| CLOSED | 确认未支付或支付失败，订单关闭 | 已关闭 |
| REFUNDING | 关闭后收到迟到成功支付，正在退款 | 退款处理中 |
| REFUNDED | 退款完成 | 已退款 |

不要在支付保护期内向用户展示“已关闭”或“支付失败”。

## 3. 保证金状态机

推荐保证金状态：

```text
FROZEN      已冻结
RELEASED    已释放
OFFSET      已抵扣尾款
FORFEITED   已罚没
REFUNDING   退款中
REFUNDED    已退款
```

不同业务规则下保证金处理方式不同。

### 3.1 保证金不抵扣货款

```text
竞拍前：冻结保证金
成交后：保证金保持 FROZEN
支付成功：保证金 RELEASED
超时未支付：保证金 FORFEITED
商家取消/系统原因：保证金 RELEASED
```

### 3.2 保证金抵扣尾款

```text
竞拍前：冻结保证金
成交后：保证金保持 FROZEN
用户支付：只支付 成交价 - 保证金
支付成功：保证金 OFFSET
超时未支付：保证金 FORFEITED 或按规则部分退还
商家取消/系统原因：保证金 RELEASED
```

真实业务必须在竞拍规则里明确告知用户：

```text
保证金是否抵扣尾款
超时未支付是否罚没
罚没比例
商家取消是否退还
系统异常是否退还
退款到账周期
```

## 4. 支付流水状态

支付流水不要和订单混在一起。

推荐支付流水状态：

```text
INIT
PROCESSING
SUCCESS
FAILED
CLOSED
REFUNDING
REFUNDED
```

支付流水关键字段：

```sql
CREATE TABLE payment_records (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    pay_no VARCHAR(64) NOT NULL UNIQUE,
    order_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    channel_trade_no VARCHAR(128) DEFAULT NULL,
    status TINYINT NOT NULL,
    requested_at DATETIME NOT NULL,
    paid_at DATETIME DEFAULT NULL,
    closed_at DATETIME DEFAULT NULL,
    raw_notify TEXT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_pay (order_id, pay_no),
    INDEX idx_order_status (order_id, status),
    INDEX idx_channel_trade (channel, channel_trade_no)
);
```

## 5. 资金流水设计

保证金不能只靠订单字段判断，必须有资金流水。

推荐资金流水：

```sql
CREATE TABLE fund_flows (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    flow_no VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT UNSIGNED NOT NULL,
    order_id BIGINT UNSIGNED DEFAULT NULL,
    item_id BIGINT UNSIGNED DEFAULT NULL,
    amount DECIMAL(12,2) NOT NULL,
    direction TINYINT NOT NULL COMMENT '1:入账, 2:出账',
    type TINYINT NOT NULL COMMENT '1:冻结, 2:解冻, 3:抵扣, 4:罚没, 5:退款',
    status TINYINT NOT NULL COMMENT '1:处理中, 2:成功, 3:失败',
    biz_no VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_biz_type (biz_no, type),
    INDEX idx_user_time (user_id, created_at)
);
```

每个资金动作都必须有唯一业务号：

```text
DEPOSIT_FREEZE:{itemId}:{userId}
DEPOSIT_RELEASE:{orderId}
DEPOSIT_OFFSET:{orderId}
DEPOSIT_FORFEIT:{orderId}
PAY_SUCCESS:{payNo}
REFUND:{payNo}
```

唯一键保证重复任务不会重复释放、重复抵扣、重复罚没。

## 6. 正常支付流程

### 6.1 订单生成

竞拍成交：

```text
生成订单 WAIT_PAY
设置 pay_deadline
保证金保持 FROZEN
写订单创建事件
```

### 6.2 用户发起支付

```text
用户点击支付
  ↓
检查订单状态 WAIT_PAY
  ↓
创建 payment_record INIT/PROCESSING
  ↓
订单 WAIT_PAY -> PAYING
  ↓
调用支付渠道
  ↓
返回支付参数给前端
```

支付按钮要幂等：

```text
同一订单重复点击支付，应返回同一个未完成 pay_no
不要创建多笔有效支付单
```

### 6.3 支付成功回调

```text
收到渠道回调
  ↓
校验签名
  ↓
根据 pay_no 查询 payment_record
  ↓
查询渠道结果确认成功
  ↓
payment_record -> SUCCESS
  ↓
订单 CAS -> PAID
  ↓
保证金 RELEASE 或 OFFSET
  ↓
通知用户支付成功
```

订单 CAS：

```sql
UPDATE orders
SET status = 'PAID',
    paid_at = NOW()
WHERE id = :orderId
  AND status IN ('WAIT_PAY', 'PAYING', 'PAY_CONFIRMING', 'CLOSE_PENDING');
```

如果影响行数为 0，说明订单已经被其他任务处理，需要重新读取订单状态并执行补偿逻辑。

## 7. 支付截止临界状态

假设：

```text
pay_deadline = 14:15:00
protect_until = 14:18:00
```

### 7.1 用户在截止前发起支付

```text
14:14:59 用户发起支付
14:15:00 超时任务触发
14:15:02 渠道回调成功
```

正确处理：

```text
14:15:00 不直接关闭订单
订单 PAYING -> PAY_CONFIRMING 或 CLOSE_PENDING
主动查询支付渠道
如果渠道成功，订单 -> PAID
如果渠道处理中，延迟重查
如果保护期结束仍未成功，订单 -> CLOSED
```

### 7.2 用户未发起支付

```text
订单 WAIT_PAY
没有 payment_record 或 payment_record 未进入 PROCESSING
pay_deadline 已过
```

可以关闭订单：

```text
WAIT_PAY -> CLOSED
保证金 FORFEIT 或 RELEASE
```

但仍建议先检查是否存在渠道侧支付单，避免本地状态落后。

### 7.3 渠道回调迟到

订单已 CLOSED 后收到支付成功：

```text
如果仍在保护期内：
  可按业务规则恢复订单为 PAID，或进入人工审核

如果已过保护期：
  不建议恢复订单
  payment_record 标记 SUCCESS_LATE
  订单 -> REFUNDING
  原路退款
  保证金按订单关闭规则处理
```

是否恢复订单必须由业务规则决定，不能让回调随意改变终态。

## 8. 保护期内用户侧展示

保护期内用户看到的是中间态。

### 8.1 PAYING

展示：

```text
订单状态：支付处理中
说明：支付处理中，请勿重复支付。
按钮：查看支付结果 / 刷新状态 / 联系客服
```

支付按钮：

```text
置灰或隐藏
```

### 8.2 PAY_CONFIRMING / CLOSE_PENDING

展示：

```text
订单状态：支付确认中
说明：支付截止时间已到，系统正在确认最后一笔支付结果，预计 1-3 分钟内完成。
按钮：刷新状态 / 联系客服
```

不要展示：

```text
订单已关闭
支付失败
保证金已罚没
请重新支付
```

因为渠道可能稍后返回成功。

### 8.3 CLOSED

展示：

```text
订单状态：已关闭
说明：未在规定时间内完成支付，订单已关闭，保证金将按竞拍规则处理。
```

如果保证金罚没：

```text
保证金状态：已按规则扣除
```

如果保证金退还：

```text
保证金状态：退还中 / 已退还
```

### 8.4 PAID

展示：

```text
订单状态：支付成功
说明：订单已确认。
保证金状态：已抵扣 / 已释放
```

## 9. 超时订单检测

不能只靠一个普通定时器扫全表。推荐三层兜底。

### 9.1 Redis ZSet 延迟队列

```text
order:pay:timeout:zset
score = pay_deadline
member = orderId
```

定时任务每秒拉取到期订单：

```text
ZRANGEBYSCORE order:pay:timeout:zset -inf now LIMIT 0 100
```

处理前先抢占：

```text
ZREM 成功才处理
```

### 9.2 数据库兜底扫描

防止 Redis 丢任务：

```sql
SELECT id
FROM orders
WHERE status IN ('WAIT_PAY', 'PAYING', 'PAY_CONFIRMING', 'CLOSE_PENDING')
  AND pay_deadline < NOW()
ORDER BY pay_deadline
LIMIT 100;
```

索引：

```sql
CREATE INDEX idx_order_status_deadline
ON orders(status, pay_deadline);
```

### 9.3 支付渠道主动查询

对于 `PAYING`、`PAY_CONFIRMING`、`CLOSE_PENDING`：

```text
主动查微信/支付宝/银行卡渠道
成功 -> PAID
失败/关闭 -> CLOSED
处理中 -> 延迟重试
```

保护期结束后仍处理中：

```text
订单 CLOSED
渠道后续若成功，走退款流程
```

## 10. 超时关闭流程

```text
超时任务触发
  ↓
查询订单
  ↓
如果 PAID/CLOSED/REFUNDED，直接跳过
  ↓
如果 WAIT_PAY，检查是否存在 PROCESSING 支付单
  ↓
如果没有，CAS 关闭订单
  ↓
处理保证金
```

对于 PAYING：

```text
PAYING -> PAY_CONFIRMING
  ↓
主动查询渠道
  ↓
成功：PAID，保证金 RELEASE/OFFSET
失败：CLOSED，保证金 FORFEIT/RELEASE
处理中：加入下一次查询队列
```

## 11. 保证金处理策略

### 11.1 支付成功

如果保证金不抵扣：

```text
保证金 RELEASED
资金流水：DEPOSIT_RELEASE:{orderId}
```

如果保证金抵扣：

```text
保证金 OFFSET
尾款支付金额 = final_price - deposit_amount
资金流水：DEPOSIT_OFFSET:{orderId}
```

### 11.2 支付超时

按竞拍规则处理。

常见规则：

```text
买家原因未支付：保证金 FORFEITED
商家取消：保证金 RELEASED
系统异常：保证金 RELEASED
风控拦截：根据审核结果 FORFEITED 或 RELEASED
```

罚没也必须幂等：

```sql
UPDATE fund_freezes
SET status = 'FORFEITED'
WHERE freeze_no = :freezeNo
  AND status = 'FROZEN';
```

影响行数为 1 才写罚没流水。

### 11.3 迟到支付成功

如果订单已关闭后收到成功支付：

```text
payment_record -> SUCCESS_LATE
订单 -> REFUNDING
发起原路退款
退款成功 -> REFUNDED
```

保证金是否退还取决于规则：

- 用户确实超时：可罚没保证金，只退还尾款。
- 渠道延迟且用户截止前已发起支付：可恢复订单，或退还保证金并人工处理。

真实业务建议把“截止前已发起支付但渠道延迟”的情况纳入保护期，减少争议。

## 12. 幂等与并发控制

### 12.1 支付成功与超时关闭竞争

支付回调和超时任务可能同时发生。

支付成功：

```sql
UPDATE orders
SET status = 'PAID'
WHERE id = :orderId
  AND status IN ('WAIT_PAY', 'PAYING', 'PAY_CONFIRMING', 'CLOSE_PENDING');
```

超时关闭：

```sql
UPDATE orders
SET status = 'CLOSED'
WHERE id = :orderId
  AND status IN ('WAIT_PAY', 'PAYING', 'PAY_CONFIRMING', 'CLOSE_PENDING')
  AND pay_deadline < NOW();
```

谁影响行数为 1，谁获得状态处理权。失败的一方必须重新查订单状态，而不是继续执行资金动作。

### 12.2 资金动作幂等

所有资金动作都通过 `biz_no + type` 唯一键保护。

示例：

```text
DEPOSIT_FORFEIT:orderId=1001
```

重复执行罚没：

```text
第一次成功写流水
第二次命中唯一键，直接返回已有结果
```

### 12.3 支付回调幂等

渠道可能重复回调。

处理方式：

```text
按 pay_no 查询 payment_record
如果已 SUCCESS，直接返回成功
如果 INIT/PROCESSING，校验渠道后更新 SUCCESS
如果 CLOSED 后收到成功，进入 SUCCESS_LATE/REFUNDING
```

## 13. 用户通知

关键状态要通知用户：

```text
订单生成：请在 xx 前支付
支付处理中：请勿重复支付
支付确认中：系统正在确认支付结果
支付成功：订单已确认
订单关闭：未按时支付，订单已关闭
保证金释放：保证金已退回可用余额
保证金罚没：保证金已按规则扣除
退款中：迟到支付已发起退款
退款成功：退款已完成
```

通知渠道：

- WebSocket 实时通知。
- 站内信。
- 短信/服务号消息。
- 订单详情页状态轮询。

## 14. 后台运营视角

后台需要看到：

```text
订单状态
支付状态
支付发起时间
支付成功时间
支付截止时间
保护期截止时间
保证金状态
保证金金额
资金流水
渠道查询记录
超时任务执行记录
退款记录
```

对于争议订单，要能一眼看出：

```text
用户是否在截止前发起支付
渠道什么时候返回成功/失败
系统什么时候进入保护期
保证金为什么释放/罚没
是否发生过退款
```

## 15. 推荐实现优先级

第一阶段：

- orders 增加 `pay_deadline`、`protect_until`。
- 增加 payment_records。
- 增加 fund_freezes 和 fund_flows。
- 支付按钮幂等。
- 支付回调幂等。

第二阶段：

- Redis ZSet 超时队列。
- DB 兜底扫描。
- PAYING/PAY_CONFIRMING 主动查询渠道。
- 用户侧“支付确认中”状态。

第三阶段：

- 迟到成功支付自动退款。
- 资金对账。
- 支付渠道对账。
- 后台争议订单处理。

## 16. 简化版落地建议

如果项目短期仍是教学/答辩用途，可以先实现简化版：

```text
WAIT_PAY
PAYING
PAY_CONFIRMING
PAID
CLOSED
```

保证金规则先选一种：

```text
支付成功释放保证金
超时未支付罚没保证金
商家取消释放保证金
```

但即使是简化版，也必须做到：

- 支付回调幂等。
- 超时关闭前查支付渠道。
- 保护期内用户显示“支付确认中”。
- 保证金释放/罚没有唯一业务号。
