CREATE DATABASE IF NOT EXISTS wishlist_db;

USE wishlist_db;

CREATE TABLE `wishlist` (
                            `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                            `user_id` BIGINT NOT NULL,
                            `product_id` BIGINT NOT NULL,
                            `quantity` INT NOT NULL,
                            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);