-- Run once with a MySQL account authorized to create local development schemas.
-- Hibernate creates the six application tables via spring.jpa.hibernate.ddl-auto=update.
-- Application credentials are supplied through environment variables, never this file.
CREATE DATABASE IF NOT EXISTS micro_investment
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
