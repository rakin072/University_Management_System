# University Management System (UMS)
**CSE 3230 Home Task (4th Lab)**

A role-based University Management System RESTful web application built with **Java 21**, **Spring Boot 3.3.4**, **Spring Data JPA**, **Spring Security 6**, and **PostgreSQL 16**, fully containerized using **Docker** and **Docker Compose**.

---

## 🏛️ System Overview & Architecture

### User Roles & Permissions (RBAC)
The application enforces strict Role-Based Access Control (RBAC):
- **Admin (`ROLE_ADMIN`)**: Superuser with complete management access to **Admins**, **Teachers**, and **Students**.
- **Teacher (`ROLE_TEACHER`)**: Faculty member with access strictly restricted to managing **Teachers** (`/api/teachers/**`). Cannot access Admin or Student endpoints.
- **Student (`ROLE_STUDENT`)**: Enrolled student with access strictly restricted to managing **Students** (`/api/students/**`). Cannot access Admin or Teacher endpoints.

### Role Access Matrix

| Endpoint Pattern | Method | Permitted Roles | Description |
| :--- | :--- | :--- | :--- |
| `/api/auth/login` | `POST` | Public | Form login (`username`, `password`) returning JSON + session cookie |
| `/api/auth/logout` | `POST` | Authenticated | Session invalidation and cookie cleanup |
| `/api/admins/**` | `GET`, `POST`, `PUT`, `DELETE` | `ROLE_ADMIN` | Admins only |
| `/api/teachers/**` | `GET`, `POST`, `PUT`, `DELETE` | `ROLE_ADMIN`, `ROLE_TEACHER` | Admins & Teachers |
| `/api/students/**` | `GET`, `POST`, `PUT`, `DELETE` | `ROLE_ADMIN`, `ROLE_STUDENT` | Admins & Students |

---

## 📁 Project Directory Structure

```plaintext
university-management-system/
├── compose.yaml
├── Dockerfile
├── .dockerignore
├── .gitignore
├── pom.xml
├── README.md
└── src/main/
    ├── java/com/example/ums/
    │   ├── UmsApplication.java
    │   ├── config/
    │   │   ├── DataInitializer.java
    │   │   └── SecurityConfig.java
    │   ├── controller/
    │   │   ├── AdminController.java
    │   │   ├── StudentController.java
    │   │   └── TeacherController.java
    │   ├── model/
    │   │   ├── Admin.java
    │   │   ├── BaseUser.java
    │   │   ├── Student.java
    │   │   └── Teacher.java
    │   ├── repository/
    │   │   ├── AdminRepository.java
    │   │   ├── StudentRepository.java
    │   │   └── TeacherRepository.java
    │   └── security/
    │       └── CustomUserDetailsService.java
    └── resources/
        └── application.yml
```

---

## 🚀 Getting Started

### Prerequisites
- [Docker](https://www.docker.com/) and [Docker Compose](https://docs.docker.com/compose/) installed and running.
- *(Optional for local runs)*: Java 21 JDK and Maven 3.9+.

### Running with Docker Compose (Recommended)
From the project root directory, run:

```bash
docker compose up --build -d
```

This will:
1. Spin up a **PostgreSQL 16** container (`ums-postgres`) on port `5432` with a health check.
2. Build the Spring Boot application using a multi-stage `Dockerfile` (`maven:3.9.9-eclipse-temurin-21` -> `eclipse-temurin:21-jre-alpine`).
3. Launch the Spring Boot container (`ums-backend`) on port `8080` once PostgreSQL is healthy.
4. Auto-seed initial accounts via `DataInitializer`.

To stop the containers:
```bash
docker compose down
```

---

## 🔑 Default Seeded Accounts

On startup, `DataInitializer` seeds one default user per role with BCrypt password hashing:

| Role | Username | Password | Email | Additional Details |
| :--- | :--- | :--- | :--- | :--- |
| **Admin** | `admin` | `admin123` | `admin@univ.edu` | Office: Room 101 |
| **Teacher** | `teacher` | `teacher123` | `turing@univ.edu` | Dept: CSE, Designation: Professor |
| **Student** | `student` | `student123` | `ada@univ.edu` | Program: B.Sc. in CSE, Batch: 2026 |

---

## 🧪 Testing with `curl`

### 1. Form-based Login as Teacher (Session Cookie)
```bash
curl -i -c cookies.txt -X POST http://localhost:8080/api/auth/login \
  -d "username=teacher&password=teacher123"
```
**Expected Response:** `200 OK` with JSON:
```json
{"message":"Login successful","user":"teacher","roles":"[ROLE_TEACHER]"}
```

### 2. Teacher Accesses Teacher Endpoint (Permitted)
```bash
curl -i -b cookies.txt http://localhost:8080/api/teachers
```
**Expected Response:** `200 OK` with a list of teachers.

### 3. Teacher Attempts to Access Student Endpoint (Forbidden)
```bash
curl -i -b cookies.txt http://localhost:8080/api/students
```
**Expected Response:** `403 Forbidden`.

### 4. Logout Session
```bash
curl -i -b cookies.txt -X POST http://localhost:8080/api/auth/logout
```
**Expected Response:** `200 OK` with JSON:
```json
{"message":"Logout successful"}
```

### 5. Admin Creates a New Student via HTTP Basic Auth
```bash
curl -i -u admin:admin123 -X POST http://localhost:8080/api/students \
  -H "Content-Type: application/json" \
  -d '{
    "username": "rahim26",
    "password": "secretPassword",
    "fullName": "Rahim Uddin",
    "email": "rahim@univ.edu",
    "program": "B.Sc. in CSE",
    "batch": 2026
  }'
```
**Expected Response:** `201 Created` with the created student object (password excluded via `@JsonProperty.Access.WRITE_ONLY`).

### 6. Admin Reads the Newly Created Student
```bash
curl -i -u admin:admin123 http://localhost:8080/api/students/2
```

### 7. Student Logs in and Accesses Students
```bash
curl -i -c student_cookie.txt -X POST http://localhost:8080/api/auth/login \
  -d "username=student&password=student123"

curl -i -b student_cookie.txt http://localhost:8080/api/students
```

### 8. Student Attempts to Access Teachers (Forbidden)
```bash
curl -i -b student_cookie.txt http://localhost:8080/api/teachers
```
**Expected Response:** `403 Forbidden`.
