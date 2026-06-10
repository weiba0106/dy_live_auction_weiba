-- ============================================================
-- 直播竞拍全栈系统 - 数据库初始化脚本
-- MySQL 5.7+ / MariaDB 10.3+
-- ============================================================

CREATE DATABASE IF NOT EXISTS dy_live_auction DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE dy_live_auction;

-- ============================================================
-- 1. 用户表
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id              BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(50)      NOT NULL UNIQUE,
    password_hash   VARCHAR(255)     NOT NULL,
    nickname        VARCHAR(50)      NOT NULL,
    avatar          VARCHAR(500)     DEFAULT '',
    role            TINYINT          NOT NULL DEFAULT 2 COMMENT '1:主播/商家, 2:普通用户',
    balance         DECIMAL(12,2)    NOT NULL DEFAULT 0.00 COMMENT '账户余额(模拟)',
    created_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================================
-- 2. 直播间表
-- ============================================================
CREATE TABLE IF NOT EXISTS live_rooms (
    id              BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    merchant_id     BIGINT UNSIGNED  NOT NULL UNIQUE,
    title           VARCHAR(100)     NOT NULL COMMENT '直播间标题',
    cover_image     VARCHAR(500)     DEFAULT '' COMMENT '封面图',
    status          TINYINT          NOT NULL DEFAULT 1 COMMENT '1:未开播, 2:直播中, 3:已结束',
    video_url       VARCHAR(500)     DEFAULT '' COMMENT '模拟直播视频地址',
    notice          VARCHAR(500)     DEFAULT '' COMMENT '直播间公告',
    created_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (merchant_id) REFERENCES users(id),
    INDEX idx_merchant (merchant_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='直播间表';

-- ============================================================
-- 3. 竞拍商品表（含规则，核心表）
-- ============================================================
CREATE TABLE IF NOT EXISTS auction_items (
    id                  BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    room_id             BIGINT UNSIGNED  NOT NULL,
    merchant_id         BIGINT UNSIGNED  NOT NULL,
    name                VARCHAR(200)     NOT NULL COMMENT '商品名称',
    description         TEXT             COMMENT '商品描述',
    images              JSON             COMMENT '商品图片列表 ["url1","url2"]',
    start_price         DECIMAL(12,2)    NOT NULL DEFAULT 0.00 COMMENT '起拍价',
    increment_amount    DECIMAL(12,2)    NOT NULL COMMENT '加价幅度(步长)',
    max_price           DECIMAL(12,2)    DEFAULT NULL COMMENT '封顶价(NULL=不设上限)',
    duration_minutes    INT              NOT NULL COMMENT '竞拍时长(分钟)',
    delay_seconds       INT              NOT NULL DEFAULT 10 COMMENT '延时秒数(10-30)',
    current_price       DECIMAL(12,2)    NOT NULL DEFAULT 0.00 COMMENT '当前最高出价',
    current_bidder_id   BIGINT UNSIGNED  DEFAULT NULL COMMENT '当前最高出价者',
    bid_count           INT UNSIGNED     NOT NULL DEFAULT 0 COMMENT '总出价次数',
    start_time          DATETIME         DEFAULT NULL COMMENT '竞拍开始时间',
    planned_end_time    DATETIME         DEFAULT NULL COMMENT '计划结束时间',
    actual_end_time     DATETIME         DEFAULT NULL COMMENT '实际结束时间(延时后)',
    status              TINYINT          NOT NULL DEFAULT 1 COMMENT '1:待开始, 2:竞拍中, 3:已结束(流拍), 4:已成交, 5:已取消',
    winner_id           BIGINT UNSIGNED  DEFAULT NULL COMMENT '最终得主',
    cancel_reason       VARCHAR(500)     DEFAULT NULL COMMENT '取消原因',
    created_at          DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (room_id) REFERENCES live_rooms(id),
    FOREIGN KEY (merchant_id) REFERENCES users(id),
    INDEX idx_room (room_id),
    INDEX idx_merchant (merchant_id),
    INDEX idx_status (status),
    INDEX idx_room_status (room_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='竞拍商品表(含规则)';

-- ============================================================
-- 4. 出价记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS bids (
    id              BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    item_id         BIGINT UNSIGNED  NOT NULL,
    user_id         BIGINT UNSIGNED  NOT NULL,
    bid_amount      DECIMAL(12,2)    NOT NULL COMMENT '出价金额(严格模式: 起拍价+N×步长)',
    bid_time        DATETIME(3)      NOT NULL COMMENT '出价时间(毫秒精度)',
    is_valid        TINYINT          NOT NULL DEFAULT 1 COMMENT '1:有效, 0:被超越',
    client_ip       VARCHAR(45)      DEFAULT NULL,
    created_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (item_id) REFERENCES auction_items(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_item (item_id),
    INDEX idx_item_user (item_id, user_id),
    INDEX idx_item_time (item_id, bid_time DESC),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出价记录表';

-- ============================================================
-- 5. 订单表
-- ============================================================
CREATE TABLE IF NOT EXISTS orders (
    id              BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    item_id         BIGINT UNSIGNED  NOT NULL,
    buyer_id        BIGINT UNSIGNED  NOT NULL COMMENT '买家',
    seller_id       BIGINT UNSIGNED  NOT NULL COMMENT '卖家',
    final_price     DECIMAL(12,2)    NOT NULL COMMENT '成交价',
    status          TINYINT          NOT NULL DEFAULT 1 COMMENT '1:待付款, 2:已付款, 3:已取消',
    paid_at         DATETIME         DEFAULT NULL COMMENT '付款时间',
    created_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (item_id) REFERENCES auction_items(id),
    FOREIGN KEY (buyer_id) REFERENCES users(id),
    FOREIGN KEY (seller_id) REFERENCES users(id),
    INDEX idx_buyer (buyer_id),
    INDEX idx_seller (seller_id),
    INDEX idx_item (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ============================================================
-- 6. 通知记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS notifications (
    id              BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT UNSIGNED  NOT NULL,
    item_id         BIGINT UNSIGNED  NOT NULL,
    type            TINYINT          NOT NULL COMMENT '1:被超越, 2:竞拍延时, 3:竞拍结束, 4:竞拍获胜, 5:商品上架提醒',
    content         VARCHAR(500)     NOT NULL COMMENT '通知内容',
    is_read         TINYINT          NOT NULL DEFAULT 0,
    created_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (item_id) REFERENCES auction_items(id),
    INDEX idx_user_read (user_id, is_read, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知记录表';
