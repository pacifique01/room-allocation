# Smart Room Allocation System

Spring Boot web application for managing campus or office rooms and their bookings.

## Features

- Login page with Spring Security
- Admin and student roles
- Admin room CRUD
- Admin booking creation and deletion
- PostgreSQL persistence with Spring Data JPA
- Thymeleaf frontend
- Double-booking prevention for the same room and time slot

## Demo Accounts

| Role | Username | Password |
| --- | --- | --- |
| Admin | `admin` | `admin123` |
| Student | `student` | `student123` |

## PostgreSQL Setup

Create a database named `roomallocation` in PostgreSQL:

```sql
CREATE DATABASE roomallocation;
```

The project is configured for:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/roomallocation
spring.datasource.username=postgres
spring.datasource.password=paccy
```

## Run

```bash
mvn spring-boot:run
```

Open:

```text
http://localhost:8080/rooms
```

You will be redirected to the login page.

## Gmail Email Setup

Copy `application-local.example.properties` to `application-local.properties` and set:

```properties
MAIL_USERNAME=your.sender@gmail.com
MAIL_PASSWORD=abcdefghijklmnop
MAIL_FROM=your.sender@gmail.com
```

For Gmail, `MAIL_PASSWORD` must be a 16-character Gmail App Password, not your normal Google account password.
After changing the file, restart the app and use **Email Test** from the admin dashboard.
