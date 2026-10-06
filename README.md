# FoodWala - Mock Data Population Guide

This guide explains how to create a simple Eclipse Dynamic Web Project,
connect it to MySQL, create the `restaurant` and `menu` tables, add the
required Java libraries, and populate the tables from the mock JSON data
hosted on GitHub.

The project is intentionally simple. It is only for demonstrating
database setup and mock-data population. It does not build a complete
food-ordering application.

---

## 1. What We Are Building

The final flow is:

```text
GitHub Mock JSON
      |
      v
Dynamic Web Project
      |
      v
Java Importer Classes
      |
      +----------------------+
      |                      |
      v                      v
RestaurantDataImporter   MenuDataImporter
      |                      |
      v                      v
restaurant table          menu table
      |                      |
      +----------+-----------+
                 |
                 v
          MySQL database
             foodwala
```

The project contains:

```text
FoodWala
|
+-- Java Resources
|   |
|   +-- src/main/java
|       |
|       +-- com.foodwala.util
|           |
|           +-- DBConnection.java
|           +-- RestaurantDataImporter.java
|           +-- MenuDataImporter.java
|
+-- src/main/webapp
    |
    +-- WEB-INF
        |
        +-- lib
            |
            +-- mysql-connector-j-9.4.0.jar
            +-- jackson-databind-2.22.3.jar
            +-- jackson-core-2.22.3.jar
            +-- jackson-annotations-2.22.jar
```

---

# 2. Requirements

Install/have the following:

- Eclipse IDE
- JDK 21
- Apache Tomcat 10.1
- MySQL Server
- MySQL Workbench
- Internet connection

The Java importers use Java's `HttpClient`, so Java 11 or newer is
required. This guide uses Java 21.

---

# 3. Mock JSON Data

The restaurant JSON is hosted in the `food-wala-api` GitHub repository:

```text
https://raw.githubusercontent.com/asifd1253/food-wala-api/main/restaurants/restaurants.json
```

The menu JSON files are hosted under:

```text
https://raw.githubusercontent.com/asifd1253/food-wala-api/main/menu/
```

The Java importers download these files directly from GitHub.

---

# 4. Create the Eclipse Dynamic Web Project

Open Eclipse.

Go to:

```text
File
    -> New
        -> Dynamic Web Project
```

Create the project with:

```text
Project Name: FoodWala
```

Use:

```text
Target Runtime: Apache Tomcat v10.1
```

Use:

```text
Java: JavaSE-21
```

Finish the project creation.

You do not need to create servlets, JSP pages, controllers, services,
repositories, or a frontend for this setup.

---

# 5. Create the MySQL Database

Open MySQL Workbench.

Run:

```sql
CREATE DATABASE foodwala;

USE foodwala;
```

Verify:

```sql
SHOW DATABASES;
```

You should see:

```text
foodwala
```

---

# 6. Create the Restaurant Table

Run:

```sql
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
```

Check the table:

```sql
DESCRIBE restaurant;
```

## Important: restaurant_id

`restaurant_id` is:

```sql
AUTO_INCREMENT
```

Therefore, Java does not need to manually assign restaurant IDs.

When 20 restaurants are inserted into an empty table, MySQL generates:

```text
1
2
3
...
20
```

Do NOT manually hardcode `restaurant_id` values in the importer.

---

# 7. Create the Menu Table

Run:

```sql
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
```

Check the table:

```sql
DESCRIBE menu;
```

The important relationship is:

```text
restaurant.restaurant_id
          |
          |
          v
menu.restaurant_id
```

One restaurant can have many menu items.

---

# 8. Add MySQL Connector/J

Download MySQL Connector/J.

For this setup, the JAR used is:

```text
mysql-connector-j-9.4.0.jar
```

The JAR will be in Download JARs folder and download the raw file from there like
<img width="764" height="152" alt="image" src="https://github.com/user-attachments/assets/9031cbd3-9119-44af-aeb4-c570b9b63ef0" />

In Eclipse, put the JAR here:

```text
FoodWala
    -> src
        -> main
            -> webapp
                -> WEB-INF
                    -> lib
                        -> mysql-connector-j-9.4.0.jar
```

After adding it, Eclipse should include it under the project's Web App
Libraries / build path.

You do not need to manually use `Add JARs` if the JAR is correctly
placed inside:

```text
WEB-INF/lib
```

---

# 9. Add Jackson Libraries

The restaurant and menu JSON files are parsed using Jackson.

Add these three JAR files to:

```text
FoodWala/src/main/webapp/WEB-INF/lib
```

Required files:

```text
jackson-databind-2.22.3.jar
jackson-core-2.22.3.jar
jackson-annotations-2.22.jar
```

so, similarly how you downloaded the mysql jar file download the Above 3 JARs
<img width="737" height="151" alt="image" src="https://github.com/user-attachments/assets/09b78ebb-7a8a-4ffd-9bc8-752efd63991e" />

The final `WEB-INF/lib` should contain:

```text
mysql-connector-j-9.4.0.jar
jackson-databind-2.22.3.jar
jackson-core-2.22.3.jar
jackson-annotations-2.22.jar
```

## Important Jackson Version Note

Do not try to download:

```text
jackson-annotations-2.22.3.jar
```

That version is not available.

The version used here is:

```text
jackson-annotations-2.22.jar
```

The three Jackson components are:

```text
jackson-databind
jackson-core
jackson-annotations
```

---

# 10. Create the Java Package

In Eclipse:

```text
Java Resources
    -> src/main/java
```

Create the package:

```text
com.foodwala.util
```

The final package will contain:

```text
com.foodwala.util
    |
    +-- DBConnection.java
    +-- RestaurantDataImporter.java
    +-- MenuDataImporter.java
```

---

# 11. Create DBConnection.java

Create:

```text
DBConnection.java
```

Use:

```java
package com.foodwala.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL =
            "jdbc:mysql://localhost:3306/foodwala";

    private static final String USER =
            "root";

    private static final String PASSWORD =
            "YOUR_MYSQL_PASSWORD";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
```

Change:

```java
"YOUR_MYSQL_PASSWORD"
```

to your local MySQL root password.

If your MySQL root account does not have a password:

```java
private static final String PASSWORD = "";
```

## Security Note

Do not commit your real MySQL password to GitHub.

For a real project, use environment variables or another secure
configuration mechanism.

---

# 12. Create RestaurantDataImporter.java

Create:

```text
RestaurantDataImporter.java
```

Use the existing `RestaurantDataImporter` code supplied with this
project.

The class must be in:

```text
package com.foodwala.util;
```

The importer uses:

```text
https://raw.githubusercontent.com/asifd1253/food-wala-api/main/restaurants/restaurants.json
```

It downloads the JSON using Java `HttpClient`, parses it using Jackson,
finds the restaurant list, extracts the required fields, and inserts the
restaurants into MySQL.

The importer connects using:

```java
DBConnection.getConnection()
```

---

# 13. Important Restaurant ID Concept

The restaurant table has:

```sql
restaurant_id INT PRIMARY KEY AUTO_INCREMENT
```

The importer does NOT insert a restaurant ID.

The database creates it automatically.

For example, after importing 20 restaurants:

```text
restaurant_id | restaurant_name
---------------+----------------
1              | Restaurant A
2              | Restaurant B
3              | Restaurant C
...
20             | Restaurant T
```

This is the correct approach.

Do not write code such as:

```java
restaurant_id = 1;
restaurant_id = 2;
...
restaurant_id = 20;
```

MySQL is responsible for generating those IDs.

---

# 14. Run RestaurantDataImporter

In Eclipse:

1.  Open `RestaurantDataImporter.java`.
2.  Right-click inside the editor.
3.  Select:

```text
Run As
    -> Java Application
```

The importer will:

```text
Download JSON
    |
    v
Parse JSON
    |
    v
Find restaurants
    |
    v
Connect to MySQL
    |
    v
Check existing restaurant names
    |
    v
Prepare INSERT statements
    |
    v
Batch insert
```

The importer also checks existing restaurant names so that an
already-existing restaurant can be skipped instead of being inserted
again.

After a successful run, verify:

```sql
USE foodwala;

SELECT COUNT(*) AS total_restaurants
FROM restaurant;
```

Expected result:

```text
20
```

---

# 15. Check the Restaurant Data

Run:

```sql
SELECT *
FROM restaurant;
```

Also check the generated IDs:

```sql
SELECT
    restaurant_id,
    restaurant_name
FROM restaurant
ORDER BY restaurant_id;
```

You should see restaurant IDs generated by MySQL.

---

# 16. Create MenuDataImporter.java

Create:

```text
MenuDataImporter.java
```

Place it in:

```text
com.foodwala.util
```

The package declaration should be:

```java
package com.foodwala.util;
```

The importer uses the menu files from:

```text
https://raw.githubusercontent.com/asifd1253/food-wala-api/main/menu/
```

The current importer processes these 20 menu JSON files:

```text
118256.json
17036.json
17310.json
23682.json
248787.json
2675.json
279024.json
28981.json
29954.json
334867.json
350220.json
36001.json
376708.json
382641.json
50467.json
548400.json
57283.json
79462.json
801279.json
81642.json
```

---

# 17. How Menu Data Finds the Correct Restaurant

Do not hardcode:

```text
restaurant_id = 1
restaurant_id = 2
...
restaurant_id = 20
```

The menu importer uses the restaurant name to find the MySQL-generated
ID.

Conceptually:

```text
Menu JSON
   |
   v
Restaurant Name
   |
   v
Search restaurant table
   |
   v
Get restaurant_id
   |
   v
Insert menu item using that restaurant_id
```

For example:

```text
Restaurant Name:
Aligarh House

MySQL:
restaurant_id = 3
```

Then its menu items are inserted using:

```text
menu.restaurant_id = 3
```

This keeps the foreign-key relationship correct.

---

# 18. Run MenuDataImporter

In Eclipse:

1.  Open:

```text
MenuDataImporter.java
```

2.  Right-click inside the editor.

3.  Select:

```text
Run As
    -> Java Application
```

The importer downloads the menu JSON files and inserts the menu records.

The importer uses the `restaurant` table to find the correct MySQL
restaurant ID before inserting menu items.

---

# 19. Expected Menu Import Result

The successful run in this setup produced:

```text
foodwala MENU IMPORT COMPLETED

Restaurants Imported : 20
Total Menu Items     : 3354
```

Your exact number can change if the mock JSON data is updated in the
GitHub repository.

---

# 20. Verify the Menu Table

Run:

```sql
SELECT COUNT(*) AS total_menu_items
FROM menu;
```

The current successful run produced:

```text
3354
```

---

# 21. Verify Restaurant + Menu Relationship

Run:

```sql
SELECT
    r.restaurant_id,
    r.restaurant_name,
    COUNT(m.menu_id) AS menu_items
FROM restaurant r
LEFT JOIN menu m
    ON r.restaurant_id = m.restaurant_id
GROUP BY
    r.restaurant_id,
    r.restaurant_name
ORDER BY
    r.restaurant_id;
```

This shows:

```text
restaurant_id
restaurant_name
number of menu items
```

for every restaurant.

---

# 22. Verify Sample Menu Records

Run:

```sql
SELECT
    m.menu_id,
    m.restaurant_id,
    r.restaurant_name,
    m.item_name,
    m.price,
    m.food_type,
    m.category
FROM menu m
JOIN restaurant r
    ON m.restaurant_id = r.restaurant_id
LIMIT 20;
```

This confirms that menu records are correctly connected to restaurants.

---

# 23. Final Database Structure

After completing the setup, the database looks like:

```text
foodwala
|
+-- restaurant
|   |
|   +-- restaurant_id
|   +-- restaurant_name
|   +-- cuisine_type
|   +-- estimated_delivery_time
|   +-- restaurant_address
|   +-- restaurant_admin_id
|   +-- cost_for_two
|   +-- rating
|   +-- is_active
|   +-- restaurant_image_url
|
+-- menu
    |
    +-- menu_id
    +-- restaurant_id  ---> restaurant.restaurant_id
    +-- item_name
    +-- description
    +-- price
    +-- is_available
    +-- food_type
    +-- category
    +-- rating
    +-- item_image_url
```

---

# 24. Complete Process in Short

A new student can remember the process as:

```text
1. Install JDK
        ↓
2. Install Eclipse
        ↓
3. Configure Tomcat
        ↓
4. Create Dynamic Web Project
        ↓
5. Create foodwala database
        ↓
6. Create restaurant table
        ↓
7. Create menu table
        ↓
8. Add MySQL Connector/J
        ↓
9. Add Jackson JARs
        ↓
10. Create com.foodwala.util package
        ↓
11. Create DBConnection.java
        ↓
12. Create RestaurantDataImporter.java
        ↓
13. Run RestaurantDataImporter
        ↓
14. Verify 20 restaurants
        ↓
15. Create MenuDataImporter.java
        ↓
16. Run MenuDataImporter
        ↓
17. Verify menu records
        ↓
18. Verify restaurant-menu relationship
```

---

# 25. Troubleshooting

## MySQL Access Denied

If you see:

```text
Access denied for user 'root'
```

Check:

```java
private static final String USER = "root";
private static final String PASSWORD = "YOUR_MYSQL_PASSWORD";
```

Make sure the password is correct.

---

## Unknown Database

If you see:

```text
Unknown database 'foodwala'
```

Run:

```sql
CREATE DATABASE foodwala;
```

Then:

```sql
USE foodwala;
```

---

## Table Does Not Exist

If you see:

```text
Table 'foodwala.restaurant' doesn't exist
```

Create the tables using the SQL in this guide.

---

## Jackson Class Not Found

If you see something such as:

```text
ClassNotFoundException
```

or:

```text
NoClassDefFoundError
```

check:

```text
WEB-INF/lib
```

It should contain:

```text
mysql-connector-j-9.4.0.jar
jackson-databind-2.22.3.jar
jackson-core-2.22.3.jar
jackson-annotations-2.22.jar
```

---

## Restaurant Not Found During Menu Import

If the menu importer says:

```text
Restaurant not found in MySQL
```

make sure the restaurant importer was successfully executed first.

The correct order is:

```text
RestaurantDataImporter
        ↓
restaurant table populated
        ↓
MenuDataImporter
        ↓
menu table populated
```

Do not run the menu importer first.

---

## Duplicate Restaurants

The restaurant importer checks existing restaurant names before
inserting.

If the restaurant already exists, it can skip that restaurant.

If you want to completely start over, you can clear the tables.

Because `menu` has a foreign key to `restaurant`, clear the menu table
first:

```sql
DELETE FROM menu;
```

Then:

```sql
DELETE FROM restaurant;
```

If you want the next restaurant import to start again from ID 1 in a
development/demo database:

```sql
ALTER TABLE restaurant AUTO_INCREMENT = 1;
```

Then run the restaurant importer again.

---

# 26. Important Notes for Students

### Do not confuse these IDs

`restaurant_id`:

```text
Generated automatically by MySQL
```

`restaurant_admin_id`:

```text
A separate column representing an admin/user reference
```

They are not the same thing.

The restaurant primary key is:

```text
restaurant_id
```

The menu foreign key is:

```text
menu.restaurant_id
```

The relationship is:

```text
restaurant.restaurant_id
            |
            v
menu.restaurant_id
```

---

# 27. Final Verification Checklist

Before considering the setup complete, verify:

- [ ] Eclipse Dynamic Web Project created
- [ ] JDK 21 configured
- [ ] Tomcat 10.1 configured
- [ ] `foodwala` database created
- [ ] `restaurant` table created
- [ ] `menu` table created
- [ ] MySQL Connector/J added
- [ ] Jackson Databind added
- [ ] Jackson Core added
- [ ] Jackson Annotations added
- [ ] `DBConnection.java` created
- [ ] `RestaurantDataImporter.java` created
- [ ] Restaurant importer executed successfully
- [ ] 20 restaurants inserted
- [ ] `MenuDataImporter.java` created
- [ ] Menu importer executed successfully
- [ ] Menu records inserted
- [ ] Restaurant-menu relationship verified

---

# 28. Result

At the end of this setup, you have a simple MySQL database populated
from GitHub mock data:

```text
foodwala
|
+-- restaurant
|      |
|      +-- 20 restaurants
|
+-- menu
       |
       +-- thousands of menu items
```

The important concept is that the Java importers automate the process
of:

```text
JSON
  ↓
Java
  ↓
Jackson parsing
  ↓
JDBC
  ↓
MySQL
```

No manual restaurant or menu data entry is required.
