# 直播竞拍系统接口响应文档（master 分支）

本文面向测开人员，按 `master` 分支实际代码整理接口的正常响应、异常响应和测试关注点。业务流程说明见 `docs/business-flow-master.md`。

## 1. 全局响应规则

### 1.1 成功响应格式

所有 REST Controller 成功时统一返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

说明：

- `code=200` 表示业务成功。
- `data` 的结构随接口不同而变化。
- 无返回体业务数据时，`data=null`。

### 1.2 master 分支异常响应规则

master 分支全局异常处理器的实际行为：

| 异常来源 | HTTP 状态 | body.code | body.message |
| --- | --- | --- | --- |
| JWT 过滤器拦截，未传 token 或 token 过期 | 401 | 401 | 未登录或 token 已过期 |
| JWT 过滤器解析 token 失败 | 401 | 401 | token 无效 |
| Controller/Service 抛 `BizException(UNAUTHORIZED)` | 401 | 401 | 未登录 |
| Controller/Service 抛其他 `BizException` | 400 | 业务 code | 业务错误消息 |
| `IllegalArgumentException` | 400 | 400 | 异常消息 |
| 未捕获异常 | 500 | 500 | 系统繁忙，请稍后再试 |

重要差异：

- master 分支中，`BizException(403, "...")`、`BizException(404, "...")`、`BizException(409, "...")` 的 HTTP 状态仍是 `400`，但 body 里的 `code` 分别是 `403/404/409`。
- JWT 过滤器直接写响应，master 中 401 body 没有 `data` 字段。
- 请求参数缺失、路径参数类型错误、JSON 格式错误没有单独处理，通常会落入系统兜底，返回 HTTP 500/code=500；测试时需要记录该实际表现。

### 1.3 常用异常响应示例

未登录或 token 过期：

```json
{
  "code": 401,
  "message": "未登录或 token 已过期"
}
```

业务异常：

```json
{
  "code": 2001,
  "message": "竞拍不存在",
  "data": null
}
```

系统兜底异常：

```json
{
  "code": 500,
  "message": "系统繁忙，请稍后再试",
  "data": null
}
```

## 2. 认证接口

### 2.1 登录

- 方法：`POST`
- 路径：`/api/auth/login`
- 鉴权：否
- 请求体：

```json
{
  "username": "buyer01",
  "password": "123456"
}
```

正常响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.demo",
    "userId": 2,
    "nickname": "买家01",
    "role": 2
  }
}
```

异常响应：

| 场景 | HTTP 状态 | body.code | body.message |
| --- | --- | --- | --- |
| 用户名不存在 | 400 | 1001 | 用户不存在 |
| 密码错误 | 400 | 1002 | 密码错误 |
| 请求体为空或 JSON 格式错误 | 500 | 500 | 系统繁忙，请稍后再试 |

### 2.2 注册

- 方法：`POST`
- 路径：`/api/auth/register`
- 鉴权：否
- 请求体：

```json
{
  "username": "buyer01",
  "password": "123456",
  "nickname": "买家01",
  "role": 2
}
```

正常响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.demo",
    "userId": 2,
    "nickname": "买家01",
    "role": 2
  }
}
```

异常响应：

| 场景 | HTTP 状态 | body.code | body.message |
| --- | --- | --- | --- |
| 用户名重复 | 400 | 400 | 用户名已被注册 |
| 请求体为空或 JSON 格式错误 | 500 | 500 | 系统繁忙，请稍后再试 |

测试关注点：

- `nickname` 不传时默认等于 `username`。
- `role` 不传时默认普通用户 `2`。
- 新注册用户默认余额 `100000.00`。

## 3. 健康检查

### 3.1 健康检查

- 方法：`GET`
- 路径：`/api/health`
- 鉴权：否

正常响应：

```json
{
  "code": 200,
  "message": "success",
  "data": "ok"
}
```

异常响应：

| 场景 | HTTP 状态 | body.code | body.message |
| --- | --- | --- | --- |
| 应用内部异常 | 500 | 500 | 系统繁忙，请稍后再试 |

## 4. 用户端直播间公开接口

### 4.1 查询直播中房间列表

- 方法：`GET`
- 路径：`/api/rooms`
- 鉴权：否

正常响应：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "title": "测试直播间",
      "coverImage": "https://example.com/cover.png",
      "videoUrl": "https://example.com/live.m3u8",
      "status": 2,
      "notice": "今晚八点开拍",
      "online": 36
    }
  ]
}
```

异常响应：

| 场景 | HTTP 状态 | body.code | body.message |
| --- | --- | --- | --- |
| Redis/数据库/系统异常 | 500 | 500 | 系统繁忙，请稍后再试 |

测试关注点：

- 只返回 `status=2` 的直播间。
- 列表缓存 3 秒。
- 无直播中房间时 `data=[]`。

### 4.2 查询直播间在线人数

- 方法：`GET`
- 路径：`/api/rooms/{roomId}/online`
- 鉴权：否

正常响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "roomId": 1,
    "online": 36
  }
}
```

异常响应：

| 场景 | HTTP 状态 | body.code | body.message |
| --- | --- | --- | --- |
| `roomId` 不是数字 | 500 | 500 | 系统繁忙，请稍后再试 |

测试关注点：

- master 不校验直播间是否存在，不存在的 `roomId` 通常返回 `online=0`。

## 5. 主播直播间接口

以下接口路径位于 `/api/admin/room`，需要请求头：

```text
Authorization: Bearer {token}
```

### 5.1 创建直播间

- 方法：`POST`
- 路径：`/api/admin/room?title=测试直播间&coverImage=...&videoUrl=...&notice=...`
- 鉴权：是

正常响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "createdAt": "2026-07-22T09:00:00",
    "updatedAt": "2026-07-22T09:00:00",
    "merchantId": 1,
    "title": "测试直播间",
    "coverImage": "https://example.com/cover.png",
    "status": 1,
    "videoUrl": "https://example.com/live.m3u8",
    "notice": "今晚八点开拍"
  }
}
```

异常响应：

| 场景 | HTTP 状态 | body.code | body.message |
| --- | --- | --- | --- |
| 未传 token/token 过期 | 401 | 401 | 未登录或 token 已过期 |
| 重复创建直播间 | 400 | 400 | 您已创建过直播间，不能重复创建 |
| 缺少必填 query 参数 `title` | 500 | 500 | 系统繁忙，请稍后再试 |

### 5.2 开播

- 方法：`POST`
- 路径：`/api/admin/room/{id}/start`
- 鉴权：是

正常响应：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

异常响应：

| 场景 | HTTP 状态 | body.code | body.message |
| --- | --- | --- | --- |
| 未登录 | 401 | 401 | 未登录或 token 已过期 |
| 直播间不存在 | 400 | 404 | 直播间不存在 |
| 操作他人直播间 | 400 | 403 | 无权操作此直播间 |
| 直播间已在直播中 | 400 | 400 | 直播间已在直播中 |
| `id` 不是数字 | 500 | 500 | 系统繁忙，请稍后再试 |

### 5.3 下播

- 方法：`POST`
- 路径：`/api/admin/room/{id}/stop`
- 鉴权：是

正常响应：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

异常响应：

| 场景 | HTTP 状态 | body.code | body.message |
| --- | --- | --- | --- |
| 未登录 | 401 | 401 | 未登录或 token 已过期 |
| 直播间不存在 | 400 | 404 | 直播间不存在 |
| 操作他人直播间 | 400 | 403 | 无权操作此直播间 |
| 直播间未在直播中 | 400 | 400 | 直播间未在直播中 |

### 5.4 查询我的直播间

- 方法：`GET`
- 路径：`/api/admin/room`
- 鉴权：是

正常响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "merchantId": 1,
    "title": "测试直播间",
    "coverImage": "https://example.com/cover.png",
    "status": 2,
    "videoUrl": "https://example.com/live.m3u8",
    "notice": "今晚八点开拍"
  }
}
```

异常响应：

| 场景 | HTTP 状态 | body.code | body.message |
| --- | --- | --- | --- |
| 未登录 | 401 | 401 | 未登录或 token 已过期 |
| 当前用户未创建直播间 | 200 | 200 | success，`data=null` |

### 5.5 查询所有直播中直播间（后台路径）

- 方法：`GET`
- 路径：`/api/admin/room/live`
- 鉴权：是

正常响应：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "merchantId": 1,
      "title": "测试直播间",
      "coverImage": "https://example.com/cover.png",
      "status": 2,
      "videoUrl": "https://example.com/live.m3u8",
      "notice": "今晚八点开拍"
    }
  ]
}
```

异常响应：

| 场景 | HTTP 状态 | body.code | body.message |
| --- | --- | --- | --- |
| 未登录 | 401 | 401 | 未登录或 token 已过期 |

## 6. 主播竞拍后台接口

以下接口路径位于 `/api/admin`，需要登录。

### 6.1 发布竞拍

- 方法：`POST`
- 路径：`/api/admin/auction`
- 鉴权：是
- 请求体：

```json
{
  "roomId": 1,
  "name": "限量手办",
  "description": "直播间竞拍商品",
  "images": "https://example.com/item.png",
  "startPrice": 100.00,
  "incrementAmount": 10.00,
  "maxPrice": 1000.00,
  "durationMinutes": 5,
  "delaySeconds": 10
}
```

正常响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 10,
    "roomId": 1,
    "merchantId": 1,
    "name": "限量手办",
    "description": "直播间竞拍商品",
    "images": "[\"https://example.com/item.png\"]",
    "startPrice": 100.00,
    "incrementAmount": 10.00,
    "maxPrice": 1000.00,
    "durationMinutes": 5,
    "delaySeconds": 10,
    "currentPrice": 100.00,
    "currentBidderId": null,
    "bidCount": null,
    "startTime": null,
    "plannedEndTime": null,
    "actualEndTime": null,
    "status": 1,
    "winnerId": null,
    "cancelReason": null
  }
}
```

异常响应：

| 场景 | HTTP 状态 | body.code | body.message |
| --- | --- | --- | --- |
| 未登录 | 401 | 401 | 未登录或 token 已过期 |
| 直播间不存在 | 400 | 404 | 直播间不存在 |
| 直播间不属于当前用户 | 400 | 403 | 无权在此直播间发布竞拍 |
| 直播间未开播 | 400 | 400 | 直播间未开播，无法发布竞拍 |
| 请求体为空/字段类型错误 | 500 | 500 | 系统繁忙，请稍后再试 |

测试关注点：

- `startPrice` 为空时按 `0` 保存。
- `delaySeconds` 为空时默认 `10`。
- 只有直播中的直播间才能发布竞拍。

### 6.2 修改竞拍规则

- 方法：`PUT`
- 路径：`/api/admin/auction/{id}`
- 鉴权：是

正常响应：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

异常响应：

| 场景 | HTTP 状态 | body.code | body.message |
| --- | --- | --- | --- |
| 未登录 | 401 | 401 | 未登录或 token 已过期 |
| 竞拍不存在 | 400 | 2001 | 竞拍不存在 |
| 竞拍不是待开始状态 | 400 | 400 | 只有待开始的竞拍可以修改规则 |
| `id` 不是数字 | 500 | 500 | 系统繁忙，请稍后再试 |

实现边界：

- master 未校验该竞拍是否属于当前登录主播。

### 6.3 查询我发布的竞拍列表

- 方法：`GET`
- 路径：`/api/admin/auctions?page=1&size=10`
- 鉴权：是

正常响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 10,
        "roomId": 1,
        "name": "限量手办",
        "currentPrice": 100.00,
        "bidCount": 0,
        "status": 1,
        "statusDesc": "待开始"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

异常响应：

| 场景 | HTTP 状态 | body.code | body.message |
| --- | --- | --- | --- |
| 未登录 | 401 | 401 | 未登录或 token 已过期 |
| `page/size` 不是数字 | 500 | 500 | 系统繁忙，请稍后再试 |

### 6.4 查询后台竞拍详情

- 方法：`GET`
- 路径：`/api/admin/auction/{id}`
- 鉴权：是

正常响应同用户端竞拍详情。

异常响应：

| 场景 | HTTP 状态 | body.code | body.message |
| --- | --- | --- | --- |
| 未登录 | 401 | 401 | 未登录或 token 已过期 |
| 竞拍不存在 | 400 | 2001 | 竞拍不存在 |
| `id` 不是数字 | 500 | 500 | 系统繁忙，请稍后再试 |

实现边界：

- master 未校验该竞拍是否属于当前登录主播。

### 6.5 开始竞拍

- 方法：`POST`
- 路径：`/api/admin/auction/{id}/start`
- 鉴权：是

正常响应：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

异常响应：

| 场景 | HTTP 状态 | body.code | body.message |
| --- | --- | --- | --- |
| 未登录 | 401 | 401 | 未登录或 token 已过期 |
| 竞拍不存在 | 400 | 2001 | 竞拍不存在 |
| 非待开始状态执行开始 | 400 | 400 | 当前状态不允许开始 |
| `id` 不是数字 | 500 | 500 | 系统繁忙，请稍后再试 |

测试关注点：

- 成功后状态变为 `2`。
- 写入 `startTime/plannedEndTime/actualEndTime`。
- WebSocket 广播 `STARTED`。

### 6.6 取消竞拍

- 方法：`POST`
- 路径：`/api/admin/auction/{id}/cancel?reason=商品临时下架`
- 鉴权：是

正常响应：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

异常响应：

| 场景 | HTTP 状态 | body.code | body.message |
| --- | --- | --- | --- |
| 未登录 | 401 | 401 | 未登录或 token 已过期 |
| 缺少 `reason` | 500 | 500 | 系统繁忙，请稍后再试 |
| 竞拍不存在 | 400 | 2001 | 竞拍不存在 |
| 状态不允许取消 | 400 | 400 | 当前状态不允许取消 |

测试关注点：

- 成功后状态变为 `5`。
- `cancelReason` 保存取消原因。
- WebSocket 广播 `CANCELLED`。
- 参与过该竞拍的用户会收到站内通知。

### 6.7 查询我卖出的订单

- 方法：`GET`
- 路径：`/api/admin/orders`
- 鉴权：是

正常响应：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 20,
      "itemId": 10,
      "buyerId": 2,
      "sellerId": 1,
      "finalPrice": 280.00,
      "status": 1,
      "paidAt": null
    }
  ]
}
```

异常响应：

| 场景 | HTTP 状态 | body.code | body.message |
| --- | --- | --- | --- |
| 未登录 | 401 | 401 | 未登录或 token 已过期 |
| 查询异常 | 500 | 500 | 系统繁忙，请稍后再试 |

## 7. 用户竞拍接口

### 7.1 查询直播间竞拍商品

- 方法：`GET`
- 路径：`/api/room/{roomId}/auctions`
- 鉴权：否

正常响应：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 10,
      "roomId": 1,
      "name": "限量手办",
      "description": "直播间竞拍商品",
      "images": "[\"https://example.com/item.png\"]",
      "startPrice": 100.00,
      "incrementAmount": 10.00,
      "maxPrice": 1000.00,
      "durationMinutes": 5,
      "delaySeconds": 10,
      "currentPrice": 180.00,
      "currentBidderName": null,
      "bidCount": 8,
      "startTime": "2026-07-22T10:00:00",
      "plannedEndTime": "2026-07-22T10:05:00",
      "actualEndTime": "2026-07-22T10:05:10",
      "status": 2,
      "statusDesc": "竞拍中"
    }
  ]
}
```

异常响应：

| 场景 | HTTP 状态 | body.code | body.message |
| --- | --- | --- | --- |
| `roomId` 不是数字 | 500 | 500 | 系统繁忙，请稍后再试 |
| 查询异常 | 500 | 500 | 系统繁忙，请稍后再试 |

### 7.2 查询竞拍详情

- 方法：`GET`
- 路径：`/api/auction/{id}`
- 鉴权：否

正常响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 10,
    "roomId": 1,
    "name": "限量手办",
    "currentPrice": 180.00,
    "bidCount": 8,
    "status": 2,
    "statusDesc": "竞拍中"
  }
}
```

异常响应：

| 场景 | HTTP 状态 | body.code | body.message |
| --- | --- | --- | --- |
| 竞拍不存在 | 400 | 2001 | 竞拍不存在 |
| `id` 不是数字 | 500 | 500 | 系统繁忙，请稍后再试 |

### 7.3 出价

- 方法：`POST`
- 路径：`/api/auction/{id}/bid?amount=190.00`
- 鉴权：是

正常响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 10,
    "roomId": 1,
    "merchantId": 1,
    "name": "限量手办",
    "currentPrice": 190.00,
    "currentBidderId": 2,
    "bidCount": 9,
    "actualEndTime": "2026-07-22T10:05:10",
    "status": 2
  }
}
```

异常响应：

| 场景 | HTTP 状态 | body.code | body.message |
| --- | --- | --- | --- |
| 未登录 | 401 | 401 | 未登录或 token 已过期 |
| 缺少 `amount` | 500 | 500 | 系统繁忙，请稍后再试 |
| `amount` 不是数字 | 500 | 500 | 系统繁忙，请稍后再试 |
| 竞拍不存在 | 400 | 2001 | 竞拍不存在 |
| 竞拍未开始/不是竞拍中 | 400 | 2002 | 竞拍尚未开始 |
| 出价低于 `当前价 + 加价幅度` | 400 | 2006 | 加价幅度不符合规则（当前价: x, 最低出价: y） |
| 出价超过封顶价 | 400 | 2005 | 出价超过封顶价x，不可超出 |
| 余额不足 | 400 | 1003 | 余额不足 |
| 获取 Redisson 锁失败 | 400 | 2007 | 操作太频繁，请稍后再试 |

测试关注点：

- 出价金额等于封顶价时会自动成交。
- 距离结束时间小于等于 `delaySeconds` 时，实际结束时间会向后延长。
- master 出价只校验余额，不冻结余额；扣款在订单支付时发生。
- REST 出价使用 query 参数 `amount`，不是 JSON body。

### 7.4 查询我的出价记录

- 方法：`GET`
- 路径：`/api/user/bids`
- 鉴权：是

正常响应：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 30,
      "itemId": 10,
      "userId": 2,
      "bidAmount": 190.00,
      "bidTime": "2026-07-22T10:02:00",
      "isValid": 1,
      "clientIp": null
    }
  ]
}
```

异常响应：

| 场景 | HTTP 状态 | body.code | body.message |
| --- | --- | --- | --- |
| 未登录 | 401 | 401 | 未登录或 token 已过期 |
| 查询异常 | 500 | 500 | 系统繁忙，请稍后再试 |

### 7.5 查询我的订单

- 方法：`GET`
- 路径：`/api/user/orders`
- 鉴权：是

正常响应：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 20,
      "itemId": 10,
      "buyerId": 2,
      "sellerId": 1,
      "finalPrice": 280.00,
      "status": 1,
      "paidAt": null
    }
  ]
}
```

异常响应：

| 场景 | HTTP 状态 | body.code | body.message |
| --- | --- | --- | --- |
| 未登录 | 401 | 401 | 未登录或 token 已过期 |
| 查询异常 | 500 | 500 | 系统繁忙，请稍后再试 |

### 7.6 支付订单

- 方法：`POST`
- 路径：`/api/order/{id}/pay`
- 鉴权：是

正常响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 20,
    "itemId": 10,
    "buyerId": 2,
    "sellerId": 1,
    "finalPrice": 280.00,
    "status": 2,
    "paidAt": "2026-07-22T10:12:00"
  }
}
```

异常响应：

| 场景 | HTTP 状态 | body.code | body.message |
| --- | --- | --- | --- |
| 未登录 | 401 | 401 | 未登录或 token 已过期 |
| 订单不存在 | 400 | 3001 | 订单不存在 |
| 操作他人订单 | 400 | 403 | 无权操作此订单 |
| 订单已支付/非待付款 | 400 | 3002 | 订单已支付 |
| 余额不足 | 400 | 1003 | 余额不足 |
| `id` 不是数字 | 500 | 500 | 系统繁忙，请稍后再试 |

测试关注点：

- 支付成功后买家余额扣减。
- master 未看到卖家入账逻辑。

### 7.7 查询竞拍排行榜

- 方法：`GET`
- 路径：`/api/auction/{id}/ranking`
- 鉴权：否

正常响应：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "rank": 1,
      "userId": 2,
      "amount": 280.00
    }
  ]
}
```

异常响应：

| 场景 | HTTP 状态 | body.code | body.message |
| --- | --- | --- | --- |
| `id` 不是数字 | 500 | 500 | 系统繁忙，请稍后再试 |
| Redis 异常 | 500 | 500 | 系统繁忙，请稍后再试 |

测试关注点：

- 排行榜最多返回 Top 20。
- 无出价记录时 `data=[]`。

## 8. WebSocket 出价与消息

### 8.1 WebSocket 出价

- 发送地址：`/app/bid`
- 鉴权：代码中未做 token 校验，payload 需要包含 `userId`
- payload：

```json
{
  "itemId": 10,
  "userId": 2,
  "amount": 190.00
}
```

业务效果：

- 与 REST 出价复用同一个 `auctionService.placeBid`。
- 成功后通过 `/topic/auction/{roomId}` 广播最新价格。
- 失败时异常会在服务端日志中体现；客户端如何收到错误取决于 STOMP 错误处理。

### 8.2 直播间订阅消息

- 公共订阅：`/topic/auction/{roomId}`
- 点对点被超越通知：`/user/queue/outbid`

消息类型：

| 类型 | 触发场景 | 接收人 |
| --- | --- | --- |
| `BID` | 有用户成功出价 | 直播间所有订阅者 |
| `DELAYED` | 临近结束出价导致延时 | 直播间所有订阅者 |
| `STARTED` | 主播开始竞拍 | 直播间所有订阅者 |
| `SOLD` | 竞拍成交 | 直播间所有订阅者 |
| `ENDED` | 竞拍流拍 | 直播间所有订阅者 |
| `CANCELLED` | 主播取消竞拍 | 直播间所有订阅者 |
| `OUTBID` | 原最高出价人被超越 | 原最高出价人 |
| `ONLINE` | 用户订阅/断开直播间 topic | 直播间所有订阅者 |

## 9. 测试建议

- 先准备两个主播账号、两个买家账号，覆盖资源归属和他人资源操作。
- 主播完整链路至少测一次：创建直播间 -> 开播 -> 发布竞拍 -> 开始竞拍 -> 用户出价 -> 成交 -> 订单生成。
- 出价重点测：低于加价幅度、等于最低有效价、超过封顶价、等于封顶价、余额不足、临近结束延时。
- 订单重点测：买家本人支付、他人订单支付、重复支付、余额不足。
- master 的参数错误多会返回 500，这是当前实现行为；如果业务期望 400，需要后续改异常处理器。
