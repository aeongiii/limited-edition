CREATE DATABASE IF NOT EXISTS payment_db;

USE payment_db;

CREATE TABLE `payment` (
                           `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                           `order_id` BIGINT NOT NULL,
                           `payment_status` ENUM('결제중', '결제완료') NOT NULL,
                           `total_amount` INT NOT NULL,
                           `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           `updated_at` TIMESTAMP DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP
);