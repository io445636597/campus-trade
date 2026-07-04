-- Campus Trade Database Schema
-- 校园二手交易平台数据库

CREATE DATABASE IF NOT EXISTS `campus_trade`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `campus_trade`;

-- 用户表
CREATE TABLE `user` (
  `id`         BIGINT AUTO_INCREMENT PRIMARY KEY,
  `username`   VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
  `password`   VARCHAR(255) NOT NULL COMMENT 'BCrypt加密后的密码',
  `email`      VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱',
  `phone`      VARCHAR(20)  DEFAULT '' COMMENT '手机号',
  `avatar_url` VARCHAR(500) DEFAULT '' COMMENT '头像URL',
  `school`     VARCHAR(100) DEFAULT '' COMMENT '学校',
  `bio`        VARCHAR(255) DEFAULT '' COMMENT '个人简介',
  `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 商品表
CREATE TABLE `product` (
  `id`          BIGINT AUTO_INCREMENT PRIMARY KEY,
  `title`       VARCHAR(100)  NOT NULL COMMENT '商品标题',
  `description` TEXT          NOT NULL COMMENT '商品描述',
  `price`       DECIMAL(10,2) NOT NULL COMMENT '价格',
  `condition`   VARCHAR(20)   NOT NULL DEFAULT 'GOOD' COMMENT '成色: NEW/LIKE_NEW/GOOD/FAIR',
  `category`    VARCHAR(50)   NOT NULL COMMENT '分类: 电子产品/书籍教材/生活用品/运动器材/服饰鞋包/数码配件/其他',
  `image_url`   VARCHAR(500)  DEFAULT '' COMMENT '商品图片URL',
  `status`      VARCHAR(20)   NOT NULL DEFAULT 'AVAILABLE' COMMENT '状态: AVAILABLE(在售)/SOLD(已售)/REMOVED(已下架)',
  `view_count`  INT           DEFAULT 0 COMMENT '浏览量',
  `user_id`     BIGINT        NOT NULL COMMENT '发布者ID',
  `created_at`  DATETIME      DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_category` (`category`),
  INDEX `idx_status` (`status`),
  INDEX `idx_price` (`price`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_created_at` (`created_at`),
  FULLTEXT INDEX `ft_title_desc` (`title`, `description`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 留言表
CREATE TABLE `message` (
  `id`         BIGINT AUTO_INCREMENT PRIMARY KEY,
  `content`    TEXT     NOT NULL COMMENT '留言内容',
  `user_id`    BIGINT   NOT NULL COMMENT '留言者ID',
  `product_id` BIGINT   NOT NULL COMMENT '商品ID',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_product_id` (`product_id`),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`product_id`) REFERENCES `product`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='留言表';

-- 收藏表
CREATE TABLE `bookmark` (
  `id`         BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id`    BIGINT   NOT NULL COMMENT '收藏者ID',
  `product_id` BIGINT   NOT NULL COMMENT '商品ID',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user_product` (`user_id`, `product_id`),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`product_id`) REFERENCES `product`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';
