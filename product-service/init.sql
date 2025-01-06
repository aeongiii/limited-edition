CREATE DATABASE IF NOT EXISTS product_db;

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