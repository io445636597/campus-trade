-- CampusTrade H2 数据库初始化脚本（开发环境）
-- H2 运行在 MODE=MySQL 兼容模式下

CREATE TABLE IF NOT EXISTS `user` (
  `id`         BIGINT AUTO_INCREMENT PRIMARY KEY,
  `username`   VARCHAR(50)  NOT NULL UNIQUE,
  `password`   VARCHAR(255) NOT NULL,
  `email`      VARCHAR(100) NOT NULL UNIQUE,
  `phone`      VARCHAR(20)  DEFAULT '',
  `avatar_url` VARCHAR(500) DEFAULT '',
  `school`     VARCHAR(100) DEFAULT '',
  `bio`        VARCHAR(255) DEFAULT '',
  `created_at` TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `product` (
  `id`          BIGINT AUTO_INCREMENT PRIMARY KEY,
  `title`       VARCHAR(100)   NOT NULL,
  `description` TEXT           NOT NULL,
  `price`       DECIMAL(10,2)  NOT NULL,
  `condition`   VARCHAR(20)    NOT NULL DEFAULT 'GOOD',
  `category`    VARCHAR(50)    NOT NULL,
  `image_url`   VARCHAR(500)   DEFAULT '',
  `status`      VARCHAR(20)    NOT NULL DEFAULT 'AVAILABLE',
  `view_count`  INT            DEFAULT 0,
  `user_id`     BIGINT         NOT NULL,
  `created_at`  TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  TIMESTAMP      DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS `idx_product_category` ON `product` (`category`);
CREATE INDEX IF NOT EXISTS `idx_product_status` ON `product` (`status`);
CREATE INDEX IF NOT EXISTS `idx_product_price` ON `product` (`price`);
CREATE INDEX IF NOT EXISTS `idx_product_user_id` ON `product` (`user_id`);
CREATE INDEX IF NOT EXISTS `idx_product_created_at` ON `product` (`created_at`);

CREATE TABLE IF NOT EXISTS `message` (
  `id`         BIGINT AUTO_INCREMENT PRIMARY KEY,
  `content`    TEXT    NOT NULL,
  `user_id`    BIGINT  NOT NULL,
  `product_id` BIGINT  NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`product_id`) REFERENCES `product`(`id`) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS `idx_message_product_id` ON `message` (`product_id`);

CREATE TABLE IF NOT EXISTS `bookmark` (
  `id`         BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id`    BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (`user_id`, `product_id`),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`product_id`) REFERENCES `product`(`id`) ON DELETE CASCADE
);
