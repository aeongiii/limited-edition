-- User 관련 데이터베이스 생성
CREATE DATABASE user_db;

-- Product 관련 데이터베이스 생성
CREATE DATABASE product_db;

-- Wishlist 관련 데이터베이스 생성
CREATE DATABASE wishlist_db;

-- Order 관련 데이터베이스 생성
CREATE DATABASE order_db;

-- Payment 관련 데이터베이스 생성
CREATE DATABASE payment_db;

USE user_db;

CREATE TABLE `user` (
                        `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                        `email` VARCHAR(255) NOT NULL UNIQUE,
                        `password` VARCHAR(255) NOT NULL,
                        `name` VARCHAR(100) NOT NULL,
                        `address` VARCHAR(255) NOT NULL,
                        `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


USE product_db;

CREATE TABLE `product` (
                           `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                           `name` VARCHAR(255) NOT NULL,
                           `description` TEXT NOT NULL,
                           `price` INT NOT NULL,
                           `stock_quantity` INT NOT NULL,
                           `is_visible` BOOLEAN DEFAULT TRUE,
                           `image_url` VARCHAR(255),
                           `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `product_snapshot` (
                                    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    `product_id` BIGINT NOT NULL,
                                    `name` VARCHAR(255) NOT NULL,
                                    `description` TEXT NOT NULL,
                                    `price` INT NOT NULL,
                                    `image_url` VARCHAR(255),
                                    FOREIGN KEY (`product_id`) REFERENCES `product`(`id`) ON DELETE CASCADE
);



USE wishlist_db;

CREATE TABLE `wishlist` (
                            `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                            `user_id` BIGINT NOT NULL,
                            `product_id` BIGINT NOT NULL,
                            `quantity` INT NOT NULL,
                            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);



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


USE payment_db;

CREATE TABLE `payment` (
                           `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                           `order_id` BIGINT NOT NULL,
                           `payment_status` ENUM('결제 완료', '환불 완료') NOT NULL,
                           `total_amount` INT NOT NULL,
                           `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           `updated_at` TIMESTAMP DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP
);
