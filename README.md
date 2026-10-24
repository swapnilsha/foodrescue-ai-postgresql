# FoodRescue AI 🍎🤖

An AI-powered surplus food redistribution platform built using Java, Spring Boot, MySQL, and Thymeleaf with Bootstrap. It is designed to bridge the gap between food donors (restaurants, events) and local NGOs, ensuring surplus food is rescued safely and efficiently.

---

## 📖 Table of Contents
1. [Project Description](#project-description)
2. [Problem Statement](#problem-statement)
3. [Project Objectives](#project-objectives)
4. [Key Features](#key-features)
5. [User Roles](#user-roles)
6. [System Workflow](#system-workflow)
7. [Technology Stack](#technology-stack)
8. [System Architecture](#system-architecture)
9. [Project Structure](#project-structure)
10. [Database Design & Migrations](#database-design--migrations)
11. [API Documentation](#api-documentation)
12. [Environment Configuration](#environment-configuration)
13. [Local Development Setup](#local-development-setup)
14. [Docker Deployment Setup](#docker-deployment-setup)
15. [Testing](#testing)
16. [Future Enhancements](#future-enhancements)
17. [License](#license)
18. [Author](#author)

---

## 💡 Project Description
**FoodRescue AI** is a real-time, full-stack web application designed to solve food waste. Using AI freshness prediction, donors can quickly evaluate the quality of surplus food by uploading an image. The application facilitates discovery of local verified NGOs, manages the delivery/pickup workflow, tracks progress on maps, and logs platform activity via audit trails.

---

## ⚠️ Problem Statement
According to the UN, nearly one-third of all food produced globally goes to waste, while millions suffer from hunger. Restaurants, hotels, and event organizers often have large quantities of edible surplus food but lack a direct, trusted, and efficient channel to deliver it to those in need before it spoils.

---

## 🎯 Project Objectives
- **Reduce Food Waste**: Accelerate food donation cycles before expiry.
- **Ensure Food Safety**: Simulate AI analysis of food quality and predict freshness labels.
- **Streamline Delivery**: Implement a structured workflow (Request -> Accepted -> Assigned -> Pickup -> Collected -> Received -> Distributed).
- **Auditability**: Log all actions of donors, NGOs, and admins for accountability.

---

## ✨ Key Features
- **AI-Powered Freshness Analysis**: Mock computer-vision freshness and shelf-life prediction based on food categories and image uploads.
- **Real-Time Notification Dispatch**: WebSockets send immediate updates to donors and NGOs when request statuses change.
- **Comprehensive Workflow Tracking**: Interactive pipeline steps from donor creation to NGO distribution.
- **Auditing & Logging**: Admin panel displays audit logs for all security, registration, and status updates.
- **Interactive Chat**: Built-in chat system allowing messaging between active donors and rescue partner NGOs.

---

## 👥 User Roles
1. **Donor (Restaurant/Host)**: Can upload surplus food, analyze freshness using AI, discover NGOs, and send requests.
2. **NGO (Rescue Partner)**: Receives requests, accepts/declines them, assigns delivery drivers, and marks food milestones.
3. **Admin (Platform Operator)**: Manages user verification, views analytical reports, controls settings, and monitors audit logs.

---

## 🔄 System Workflow
```
[Donor Uploads Food] ─> [AI Quality Check] ─> [Select NGO] ─> [Create Request]
                                                                     │
[Delivered/Distributed] <─ [Received] <─ [Collected] <─ [Pickup] <─ [Accept Request]
```

---

## 🛠️ Technology Stack
- **Backend**: Java 21, Spring Boot 3.2.3, Spring Security, JPA Hibernate, Spring Actuator, MapStruct, Lombok.
- **Database & Migrations**: MySQL 8.0, Flyway DB.
- **Frontend**: HTML5, CSS3, JavaScript (Vanilla ES6), Bootstrap 5, WebSockets (STOMP).
- **Testing & CI**: Spring Boot Test (MockMvc), JUnit 5, GitHub Actions.
- **Deployment**: Docker, Docker Compose.

---

## 🏛️ System Architecture
```
                ┌─────────────────────────┐
                │         Browser         │
                │ HTML5 / CSS3 / JS / BS5 │
                └────────────┬────────────┘
                             │ (REST APIs & WebSockets)
                             ▼
                ┌─────────────────────────┐
                │       Spring Boot       │
                │   (Spring MVC, Security)│
                └────────────┬────────────┘
                             │
            ┌────────────────┴────────────────┐
            ▼                                 ▼
     ┌─────────────┐                   ┌─────────────┐
     │    MySQL    │                   │ AI Freshness│
     │  Database   │                   │ Simulator   │
     └─────────────┘                   └─────────────┘
```

---

## 📂 Project Structure
```text
foodrescue/
├── .github/workflows/      # GitHub Actions CI Configurations
├── src/
│   ├── main/
│   │   ├── java/com/foodrescue/
│   │   │   ├── config/      # App configurations (Security, WebSocket, Swagger)
│   │   │   ├── controller/  # REST Controllers
│   │   │   ├── dto/         # Data Transfer Objects
│   │   │   ├── entity/      # JPA Hibernate Entities
│   │   │   ├── mapper/      # MapStruct DTO Mappers
│   │   │   ├── repository/  # Spring Data JPA Repositories
│   │   │   └── service/     # Core Business Logic Layer
│   │   └── resources/
│   │       ├── db/migration/# Flyway SQL Schema Migrations (V1 to V7)
│   │       ├── static/      # Web Static Assets (CSS, JS, Images)
│   │       ├── templates/   # Thymeleaf HTML Views
│   │       └── application.properties
│   └── test/                # JUnit Integration Test Suite
├── Dockerfile               # Multi-Stage App Dockerfile
├── docker-compose.yml       # Container Orchestration
├── .dockerignore            # Docker Build Exclusions
├── .gitignore               # Git Version Control Exclusions
├── LICENSE                  # MIT License
├── application-example.properties  # Configuration Properties Template
└── .env.example             # Environment Variables Template
```

---

## 🗄️ Database Design & Migrations
Database tables are managed automatically using **Flyway DB** migrations located in `src/main/resources/db/migration`:
* **V1**: Creates core tables (`users`, `locations`, `food_donations`).
* **V2**: Adds profile-specific structural columns.
* **V3**: Seeds default coordinates for cities.
* **V4**: Defines `donation_requests` for the rescue pipeline.
* **V5**: Introduces constraints and statuses.
* **V6**: Creates `audit_logs` and `chat_messages` tables.
* **V7**: Introduces the `system_settings` table.

---

## 🔌 API Documentation

### Authentication APIs
| HTTP Method | Endpoint | Purpose | Access | Request Body (JSON) | Response |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/register` | Register new Donor, NGO, Admin | Public | `RegisterRequest` | JWT token & user summary |
| `POST` | `/api/auth/login` | Login and obtain JWT | Public | `AuthRequest` | JWT token & user summary |
| `GET` | `/api/auth/me` | Get current logged-in profile | Authenticated | None | Current User JSON |
| `POST` | `/api/auth/logout` | Client side sign-out | Authenticated | None | Success message |

### Food Donation APIs
| HTTP Method | Endpoint | Purpose | Access | Request / Parameters | Response |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/food/upload` | Add donation with image | `DONOR` | `MultipartForm` (image, title, details) | Created `FoodDonation` |
| `POST` | `/api/food` | Add donation (JSON only) | `DONOR` | `FoodDonationRequest` (JSON) | Created `FoodDonation` |
| `GET` | `/api/food` | Retrieve all available food | `DONOR`, `NGO`, `ADMIN` | None | List of `FoodDonation` |
| `GET` | `/api/food/{id}` | Retrieve donation details | `DONOR`, `NGO`, `ADMIN` | Path variable `id` | `FoodDonation` |
| `GET` | `/api/food/donor` | Get current Donor's donations | `DONOR`, `ADMIN` | None | List of `DonationResponse` |
| `GET` | `/api/food/ngo` | Get accepted NGO donations | `NGO`, `ADMIN` | None | List of `DonationResponse` |
| `PATCH` | `/api/food/{id}/status` | Update general status | `DONOR`, `NGO`, `ADMIN` | `{"status": "..."}` | `DonationResponse` |
| `DELETE` | `/api/food/{id}` | Delete a donation record | `DONOR`, `ADMIN` | Path variable `id` | HTTP 204 No Content |

### Donation Request Workflow APIs
| HTTP Method | Endpoint | Purpose | Access | Request Body |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/requests` | Send donation request to NGO | `DONOR` | `{"donationId": 1, "ngoId": 2}` |
| `GET` | `/api/requests/donor` | Get all outgoing donor requests | `DONOR`, `ADMIN` | None |
| `GET` | `/api/requests/ngo` | Get incoming NGO requests | `NGO`, `ADMIN` | None |
| `PATCH` | `/api/requests/{id}/accept` | Accept a pending request | `NGO`, `ADMIN` | None |
| `PATCH` | `/api/requests/{id}/reject` | Reject a pending request | `NGO`, `ADMIN` | `{"reason": "..."}` |
| `PATCH` | `/api/requests/{id}/assign` | Assign driver to route | `NGO`, `ADMIN` | `{"assignedToName": "...", "assignedToPhone": "..."}` |
| `PATCH` | `/api/requests/{id}/pickup-start` | Mark route as started | `NGO`, `ADMIN` | None |
| `PATCH` | `/api/requests/{id}/collect` | Confirm food collected | `NGO`, `ADMIN` | None |
| `PATCH` | `/api/requests/{id}/receive` | Confirm food received | `NGO`, `ADMIN` | `{"receivedQuantityKg": 15.5, "notes": "..."}` |
| `PATCH` | `/api/requests/{id}/deliver` | Confirm food distributed | `NGO`, `ADMIN` | `{"notes": "..."}` |
| `PATCH` | `/api/requests/{id}/cancel` | Cancel active request | Authenticated | None |

### AI Analysis APIs
| HTTP Method | Endpoint | Purpose | Access | Request / Parameters |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/ai/analyze` | Scan uploaded image for freshness | `DONOR`, `NGO`, `ADMIN` | `MultipartForm` (image file, category) |

---

## 🔒 Environment Configuration

Sensitive secrets are abstracted to environment variables. Set the following keys in your system environment or a `.env` file at the project root:

| Key | Description | Default Value |
| :--- | :--- | :--- |
| `PORT` | Local runtime application port | `8080` |
| `DB_URL` | JDBC database connection URL | `jdbc:mysql://mysql:3306/foodrescue?useSSL=false` |
| `DB_USERNAME` | Production MySQL Username | `foodrescue_user` |
| `DB_PASSWORD` | Production MySQL Password | `[Required]` |
| `JWT_SECRET` | 256-bit Secure Signing Key | `[Required]` |
| `UPLOAD_DIR` | Directory path where uploads reside | `./uploads` |

---

## 💻 Local Development Setup

### Prerequisites
- JDK 21 installed.
- Maven 3.9+ installed.
- MySQL 8.0 server active locally.

### Step 1: Prepare Database
Log into MySQL and execute:
```sql
CREATE DATABASE foodrescue;
CREATE DATABASE foodrescue_test;
```

### Step 2: Configure Properties
Copy `application-example.properties` to `src/main/resources/application.properties` and replace credentials:
```properties
spring.datasource.username=root
spring.datasource.password=your_local_password
jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
```

### Step 3: Compile and Package
```bash
mvn clean install
```

### Step 4: Run Application
```bash
mvn spring-boot:run
```
The application will launch on [http://localhost:8080](http://localhost:8080).

---

## 🐳 Docker Deployment Setup

Deploy the entire stack (Spring Boot Application + MySQL Database) using Docker Compose.

### Step 1: Copy Environment Template
```bash
cp .env.example .env
```
Open `.env` and fill in custom database passwords and JWT secrets.

### Step 2: Build Containers
```bash
docker compose build
```

### Step 3: Start Services
Launch the application and database containers in detached mode:
```bash
docker compose up -d
```

### Step 4: Verify Services Status
```bash
docker compose ps
```

To inspect application startup log streams:
```bash
docker compose logs -f app
```

To shut down services:
```bash
docker compose down
```

---

## 🧪 Testing

### Running Unit & Integration Tests
Integration tests run against an isolated test database. Make sure you set your local MySQL passwords or define environment variables before running:
```bash
mvn clean test
```

### CI/CD Pipeline
Every pull request and push to the `main` or `master` branches triggers the **GitHub Actions** CI workflow to:
1. Initialize a MySQL service container.
2. Verify all JUnit tests pass.
3. Validate compilation and build package.
4. Verify the Dockerfile builds successfully.

---

## 🚀 Future Enhancements
- **True AI Computer Vision Model Integration**: Connect actual TensorFlow/PyTorch object detection endpoints for food analysis.
- **Dynamic Google Maps Routing**: Fetch traffic details and calculate optimal driver paths.
- **Multilingual Support**: Internationalization (i18n) for local community usability.

---

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## ✍️ Author
Designed & Developed by a Java Full Stack Developer.
