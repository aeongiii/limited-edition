CREATE DATABASE IF NOT EXISTS order_db;

USE order_db;

CREATE TABLE `orders` (
                          `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                          `user_id` BIGINT NOT NULL,
                          `status` ENUM('주문 완료', '배송중', '배송 완료', '취소 완료', '반품 신청', '반품 완료') NOT NULL,
                          `total_amount` INT NOT NULL,
                          `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `order_detail` (
                                `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                `order_id` BIGINT NOT NULL,
                                `product_snapshot_id` BIGINT NOT NULL,
                                `quantity` INT NOT NULL,
                                `subtotal_amount` INT NOT NULL
);