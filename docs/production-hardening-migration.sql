-- Production hardening migration for the real-business bidding flow.
-- Run this on an existing dy_live_auction database before deploying the code changes.

ALTER TABLE users
    ADD COLUMN frozen_balance DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '竞拍冻结金额'
    AFTER balance;

ALTER TABLE auction_items
    ADD INDEX idx_status_end_time (status, actual_end_time);

ALTER TABLE orders
    ADD UNIQUE KEY uk_item (item_id);
