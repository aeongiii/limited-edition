CREATE DATABASE IF NOT EXISTS order_db;

USE order_db;

CREATE TABLE `orders` (
                          `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                          `user_id` BIGINT NOT NULL,
                          `status` ENUM('주문중', '주문완료', '배송중', '배송완료', '취소완료', '반품신청', '반품완료') NOT NULL,
                          `total_amount` INT NOT NULL,
                          `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

                        INDEX `idx_user_created` (`user_id`, `created_at` DESC)
);

CREATE TABLE `order_detail` (
                                `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                `order_id` BIGINT NOT NULL,
                                `product_snapshot_id` BIGINT NOT NULL,
                                `quantity` INT NOT NULL,
                                `subtotal_amount` INT NOT NULL
);