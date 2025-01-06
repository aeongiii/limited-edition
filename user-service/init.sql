CREATE DATABASE IF NOT EXISTS user_db;

USE user_db;

CREATE TABLE `user` (
                        `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                        `email` VARCHAR(255) NOT NULL UNIQUE,
                        `password` VARCHAR(255) NOT NULL,
                        `name` VARCHAR(100) NOT NULL,
                        `address` VARCHAR(255) NOT NULL,
                        `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);