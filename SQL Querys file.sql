CREATE DATABASE foodwala;

USE foodwala;

SHOW DATABASES;

SELECT DATABASE();

CREATE TABLE restaurant (
    restaurant_id INT PRIMARY KEY AUTO_INCREMENT,
    restaurant_name VARCHAR(255) NOT NULL,
    cuisine_type VARCHAR(500),
    estimated_delivery_time INT,
    restaurant_address VARCHAR(500),
    restaurant_admin_id INT,
    cost_for_two DECIMAL(10,2),
    rating DECIMAL(3,2),
    is_active BOOLEAN,
    restaurant_image_url VARCHAR(1000)
);

DESCRIBE restaurant;

SELECT * FROM restaurant;

CREATE TABLE menu (
    menu_id INT PRIMARY KEY AUTO_INCREMENT,
    restaurant_id INT NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2),
    is_available BOOLEAN,
    food_type VARCHAR(20),
    category VARCHAR(255),
    rating DECIMAL(3,2),
    item_image_url VARCHAR(1000),

    CONSTRAINT fk_menu_restaurant
        FOREIGN KEY (restaurant_id)
        REFERENCES restaurant(restaurant_id)
);

DESCRIBE menu;

SELECT * FROM menu;

SHOW TABLES;

SELECT COUNT(*) AS total_restaurants
FROM restaurant;

SELECT COUNT(*) AS total_menu_items
FROM menu;

SELECT
    r.restaurant_id,
    r.restaurant_name,
    COUNT(m.menu_id) AS menu_items
FROM restaurant r
LEFT JOIN menu m
    ON r.restaurant_id = m.restaurant_id
GROUP BY r.restaurant_id, r.restaurant_name
ORDER BY r.restaurant_id;