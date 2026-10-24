# FoodRescue AI 🍎🤖

An AI-assisted surplus food redistribution platform built with **Java, Spring Boot, PostgreSQL, Spring Security, JPA/Hibernate, Thymeleaf, JavaScript, and WebSockets**.

FoodRescue AI is designed to connect food donors with NGOs so that surplus food can be identified, donated, tracked, and distributed efficiently before it expires.

---

## 📖 Table of Contents

1. [Project Overview](#-project-overview)
2. [Problem Statement](#-problem-statement)
3. [Project Objectives](#-project-objectives)
4. [Key Features](#-key-features)
5. [User Roles](#-user-roles)
6. [System Workflow](#-system-workflow)
7. [Technology Stack](#-technology-stack)
8. [System Architecture](#-system-architecture)
9. [Project Structure](#-project-structure)
10. [Database Design & Migrations](#-database-design--migrations)
11. [API Documentation](#-api-documentation)
12. [Environment Configuration](#-environment-configuration)
13. [Local Development Setup](#-local-development-setup)
14. [Docker Deployment](#-docker-deployment)
15. [Testing](#-testing)
16. [MySQL to PostgreSQL Migration](#-mysql-to-postgresql-migration)
17. [My Contribution](#-my-contribution)
18. [Future Enhancements](#-future-enhancements)
19. [License](#-license)
20. [Author](#-author)

---

## 🚀 Project Overview

**FoodRescue AI** is a full-stack web application designed to reduce food waste by connecting food donors with NGOs and rescue partners.

The platform provides functionality for:

- Donor registration and authentication
- Surplus food donation management
- Food image upload and analysis
- NGO discovery
- Donation request management
- Pickup and delivery workflow tracking
- Notifications
- Real-time communication
- Audit logging
- Role-based access control

The application follows a layered Spring Boot architecture with controllers, services, repositories, entities, DTOs, security components, and database migrations.

---

## ⚠️ Problem Statement

Large quantities of edible food are often discarded by restaurants, events, hotels, and other organizations while nearby communities and NGOs may require food resources.

FoodRescue AI aims to provide a structured digital platform through which surplus food can be registered, evaluated, requested, collected, and distributed before it expires.

---

## 🎯 Project Objectives

- **Reduce Food Waste**  
  Enable donors to quickly register and redistribute surplus food.

- **Improve Food Safety**  
  Provide image-based food analysis and freshness estimation.

- **Streamline Donation Management**  
  Manage the complete donation lifecycle through structured workflows.

- **Improve Transparency**  
  Maintain audit logs and status tracking throughout the process.

- **Enable Real-Time Communication**  
  Provide notifications and chat functionality between platform users.

---

## ✨ Key Features

### 🔐 Authentication & Security

- User registration and login
- JWT-based authentication
- Spring Security integration
- Role-based authorization
- Protected REST APIs
- Global exception handling

### 🍱 Food Donation Management

- Create food donations
- Upload food images
- Track donation status
- Store food quantity and expiry information
- View donation history
- Manage donor-specific donations

### 🤖 AI-Assisted Food Analysis

- Upload food images
- Analyze food freshness
- Estimate food quantity
- Generate freshness/confidence information
- Provide food rescue recommendations

> The current implementation uses an application-level analysis/simulation approach rather than a production computer-vision model.

### 🤝 Donation Request Workflow

The application supports a structured workflow:

```text
Request
   ↓
Accepted
   ↓
Assigned
   ↓
Pickup Started
   ↓
Collected
   ↓
Received
   ↓
Delivered
```

### 🔔 Notifications

- Donation-related notifications
- Request status updates
- Real-time notification dispatch
- WebSocket/STOMP communication

### 💬 Real-Time Chat

- Donor-to-NGO communication
- WebSocket-based messaging
- Room-based chat handling
- Persistent chat messages

### 📊 Administration

- User management
- Audit logs
- Monitoring
- Analytics
- System settings
- Platform activity tracking

---

## 👥 User Roles

### Donor

A donor can:

- Register and authenticate
- Add surplus food
- Upload food images
- Analyze food
- View donations
- Send donation requests
- Track donation progress

### NGO / Rescue Partner

An NGO can:

- View incoming donation requests
- Accept or reject requests
- Assign pickup personnel
- Track collection
- Confirm received food
- Confirm distribution
- Communicate with donors

### Admin

An administrator can:

- Manage users
- Monitor platform activity
- Review audit logs
- View analytics
- Manage system settings
- Monitor donation workflows

---

## 🔄 System Workflow

```text
┌──────────────────┐
│  Donor Registers │
└────────┬─────────┘
         ↓
┌──────────────────┐
│ Upload Food Info │
│   + Food Image   │
└────────┬─────────┘
         ↓
┌──────────────────┐
│  AI Food Analysis│
└────────┬─────────┘
         ↓
┌──────────────────┐
│ Select NGO /     │
│ Create Request   │
└────────┬─────────┘
         ↓
┌──────────────────┐
│ NGO Accepts      │
│ Request          │
└────────┬─────────┘
         ↓
┌──────────────────┐
│ Assign Pickup    │
└────────┬─────────┘
         ↓
┌──────────────────┐
│ Pickup / Collect │
└────────┬─────────┘
         ↓
┌──────────────────┐
│ Food Received    │
└────────┬─────────┘
         ↓
┌──────────────────┐
│ Food Distributed │
└──────────────────┘
```

---

## 🛠️ Technology Stack

### Backend

- Java 21
- Spring Boot 3.2.3
- Spring MVC
- Spring Security
- JWT Authentication
- Spring Data JPA
- Hibernate
- REST APIs
- Spring Actuator
- MapStruct
- Lombok

### Database

- PostgreSQL
- Flyway Database Migrations

### Frontend

- HTML5
- CSS3
- JavaScript ES6
- Bootstrap
- Thymeleaf

### Real-Time Communication

- WebSockets
- STOMP

### Testing

- JUnit 5
- Spring Boot Test
- MockMvc
- Integration Testing

### Development & Deployment

- Maven
- Git
- GitHub
- GitHub Actions
- Docker
- Docker Compose
- IntelliJ IDEA
- Postman

---

## 🏛️ System Architecture

```text
                    ┌──────────────────────┐
                    │       Browser        │
                    │ HTML / CSS / JS      │
                    │ Thymeleaf / Bootstrap│
                    └──────────┬───────────┘
                               │
                    REST APIs / WebSockets
                               │
                               ▼
                    ┌──────────────────────┐
                    │     Spring Boot      │
                    │                      │
                    │ Controllers          │
                    │ Services             │
                    │ Security             │
                    │ DTOs                 │
                    │ Exception Handling   │
                    └──────────┬───────────┘
                               │
                    ┌──────────┴───────────┐
                    │                      │
                    ▼                      ▼
          ┌─────────────────┐    ┌──────────────────┐
          │   PostgreSQL    │    │ Food Analysis    │
          │    Database     │    │    Service       │
          └─────────────────┘    └──────────────────┘
                    │
                    ▼
          ┌─────────────────┐
          │ Flyway Database │
          │   Migrations    │
          └─────────────────┘
```

---

## 📂 Project Structure

```text
foodrescue-ai/
│
├── .github/
│   └── workflows/
│       └── build.yml
│
├── src/
│   ├── main/
│   │   ├── java/com/foodrescue/
│   │   │
│   │   ├── config/
│   │   │   ├── ApplicationConfig.java
│   │   │   ├── SecurityConfig.java
│   │   │   └── WebSocketConfig.java
│   │   │
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── exception/
│   │   ├── mapper/
│   │   ├── repository/
│   │   ├── scheduler/
│   │   ├── security/
│   │   ├── service/
│   │   ├── util/
│   │   └── websocket/
│   │
│   └── resources/
│       ├── db/
│       │   └── migration/
│       ├── static/
│       ├── templates/
│       └── application.properties
│
├── src/test/
│
├── Dockerfile
├── docker-compose.yml
├── pom.xml
├── application-example.properties
├── .env.example
├── .gitignore
└── README.md
```

---

## 🗄️ Database Design & Migrations

Database schema changes are managed using **Flyway**.

Migration files are located at:

```text
src/main/resources/db/migration/
```

Current migrations include:

| Migration | Purpose |
|---|---|
| V1 | Initial database schema |
| V2 | Additional food donation and notification fields |
| V3 | Default location seed data |
| V4 | Donation request workflow |
| V5 | Food urgency and notes |
| V6 | Audit logs and chat messages |
| V7 | System settings |
| V8 | User profile fields |

Major database tables include:

```text
users
locations
food_donations
food_analysis
notifications
donation_requests
audit_logs
chat_messages
system_settings
```

---

## 🔌 API Documentation

### Authentication APIs

| Method | Endpoint | Purpose | Access |
|---|---|---|---|
| POST | `/api/auth/register` | Register user | Public |
| POST | `/api/auth/login` | Login | Public |
| GET | `/api/auth/me` | Current user profile | Authenticated |
| POST | `/api/auth/logout` | Logout | Authenticated |

### Food Donation APIs

| Method | Endpoint | Purpose | Access |
|---|---|---|---|
| POST | `/api/food/upload` | Create donation with image | DONOR |
| POST | `/api/food` | Create donation | DONOR |
| GET | `/api/food` | Get available food | DONOR / NGO / ADMIN |
| GET | `/api/food/{id}` | Get donation details | DONOR / NGO / ADMIN |
| GET | `/api/food/donor` | Get donor donations | DONOR / ADMIN |
| GET | `/api/food/ngo` | Get NGO donations | NGO / ADMIN |
| PATCH | `/api/food/{id}/status` | Update donation status | DONOR / NGO / ADMIN |
| DELETE | `/api/food/{id}` | Delete donation | DONOR / ADMIN |

### Donation Request APIs

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/requests` | Create donation request |
| GET | `/api/requests/donor` | View donor requests |
| GET | `/api/requests/ngo` | View NGO requests |
| PATCH | `/api/requests/{id}/accept` | Accept request |
| PATCH | `/api/requests/{id}/reject` | Reject request |
| PATCH | `/api/requests/{id}/assign` | Assign pickup personnel |
| PATCH | `/api/requests/{id}/pickup-start` | Start pickup |
| PATCH | `/api/requests/{id}/collect` | Mark food collected |
| PATCH | `/api/requests/{id}/receive` | Mark food received |
| PATCH | `/api/requests/{id}/deliver` | Mark food delivered |
| PATCH | `/api/requests/{id}/cancel` | Cancel request |

### AI Analysis API

```text
POST /api/ai/analyze
```

Accepts a food image and category for food analysis.

---

## 🔒 Environment Configuration

Sensitive configuration values are loaded through environment variables.

The application supports:

```properties
PORT
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
```

Example configuration:

```properties
server.port=${PORT:8080}

spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/foodrescue}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD}

jwt.secret=${JWT_SECRET}
```

### Example Environment Variables

```text
DB_URL=jdbc:postgresql://localhost:5432/foodrescue
DB_USERNAME=postgres
DB_PASSWORD=your_postgresql_password
JWT_SECRET=your_secure_jwt_secret
```

**Do not commit real passwords, API keys, or JWT secrets to GitHub.**

---

## 💻 Local Development Setup

### Prerequisites

Install:

- JDK 21
- PostgreSQL
- Maven 3.9+
- Git
- IntelliJ IDEA or another Java IDE

### Step 1: Create PostgreSQL Database

Create the application database:

```sql
CREATE DATABASE foodrescue;
```

For testing:

```sql
CREATE DATABASE foodrescue_test;
```

### Step 2: Configure Environment Variables

Set the required variables:

```text
DB_URL=jdbc:postgresql://localhost:5432/foodrescue
DB_USERNAME=postgres
DB_PASSWORD=your_password
JWT_SECRET=your_secure_secret
```

Alternatively, configure them through your IDE's Run Configuration.

### Step 3: Build the Project

Using Maven Wrapper:

```bash
./mvnw clean install
```

On Windows:

```powershell
.\mvnw.cmd clean install
```

### Step 4: Run the Application

```powershell
.\mvnw.cmd spring-boot:run
```

Or run `FoodrescueApplication` directly from IntelliJ IDEA.

### Step 5: Open the Application

```text
http://localhost:8080
```

Flyway automatically applies the database migrations when the application starts.

---

## 🐳 Docker Deployment

The project includes Docker-related configuration for containerized deployment.

### Build the Application Image

```bash
docker compose build
```

### Start Containers

```bash
docker compose up -d
```

### Check Running Containers

```bash
docker compose ps
```

### View Application Logs

```bash
docker compose logs -f app
```

### Stop Containers

```bash
docker compose down
```

> For PostgreSQL deployment, ensure the Docker Compose database configuration uses PostgreSQL and that the application's `DB_URL`, username, and password match the PostgreSQL service.

---

## 🧪 Testing

The project contains unit and integration tests under:

```text
src/test/
```

Run the test suite with:

```powershell
.\mvnw.cmd clean test
```

The project also includes a GitHub Actions workflow for automated build verification.

---

## 🔄 MySQL to PostgreSQL Migration

The original application used **MySQL**. This version of the project has been migrated to **PostgreSQL**.

### Migration Work

The migration involved:

- Replacing the MySQL JDBC driver with the PostgreSQL driver
- Updating Spring datasource configuration
- Updating Hibernate database configuration
- Converting MySQL-specific SQL syntax to PostgreSQL-compatible SQL
- Updating Flyway migration scripts
- Converting MySQL `AUTO_INCREMENT` behavior to PostgreSQL `BIGSERIAL`
- Converting MySQL boolean fields to PostgreSQL-compatible `BOOLEAN`
- Updating timestamp and constraint definitions
- Verifying foreign keys and indexes
- Creating PostgreSQL-compatible seed data
- Testing database initialization through Flyway
- Verifying application startup and JPA integration

### PostgreSQL Stack

```text
Java 21
   │
Spring Boot
   │
Spring Data JPA / Hibernate
   │
PostgreSQL
   │
Flyway
```

The PostgreSQL database is now the primary database configuration for this repository.

---

## 👨‍💻 My Contribution

This project was developed as a **team project**.

My primary contribution was the **MySQL-to-PostgreSQL migration and database integration**.

### Responsibilities

- Migrated the application from MySQL to PostgreSQL
- Updated Maven database dependencies
- Updated Spring Boot datasource configuration
- Converted Flyway SQL migrations
- Resolved PostgreSQL compatibility issues
- Configured Hibernate/JPA for PostgreSQL
- Verified database schema creation
- Tested application startup with PostgreSQL
- Tested authentication and registration
- Tested food donation creation and persistence
- Tested image upload and food analysis
- Verified the application locally after migration

I also worked with and understood the application's existing Spring Boot modules, including authentication, donation management, security, notifications, and the donation request workflow.

---

## 📈 Future Enhancements

- Integrate a production-grade computer vision model for food freshness analysis
- Improve food expiry prediction
- Integrate real-time Google Maps routing
- Add optimized pickup route calculation
- Add multilingual support
- Improve NGO verification workflows
- Add cloud-based image storage
- Add production monitoring and observability
- Improve automated test coverage

---

## 📄 License

This project is licensed under the MIT License.

See the [LICENSE](LICENSE) file for details.

---

## ✍️ Author

**Sha Sopnil Jain**

Java Full Stack Developer

### Technologies

```text
Java
Spring Boot
Spring Security
REST APIs
JPA / Hibernate
PostgreSQL
React / JavaScript
Docker
Git
```

---

⭐ If you find this project useful, consider giving the repository a star.
