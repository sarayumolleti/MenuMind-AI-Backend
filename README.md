# MenuMind AI — Backend REST API

High-performance Spring Boot REST API powering MenuMind AI. Features intelligent menu engineering (BCG Matrix for Stars, Puzzles, Plowhorses, Dogs), automated customer sentiment analytics, POS billing, and real-time sales reporting.

---

## 🛠️ Tech Stack
- **Framework:** Spring Boot 3.3.4 (Java 17)
- **Database:** MySQL 8.0 (Aiven Cloud) + Spring Data JPA / Hibernate
- **Security:** Spring Security + JSON Web Tokens (JJWT)
- **Analytics:** Apache Commons Math
- **Build Tool:** Maven

---

## ⚙️ Local Setup

1. Copy `.env.example` to `.env`:
   ```bash
   cp .env.example .env
   ```
2. Populate `.env` with your MySQL database credentials:
   ```env
   DB_HOST=your-mysql-instance.aivencloud.com
   DB_PORT=25107
   DB_NAME=defaultdb
   DB_USER=avnadmin
   DB_PASSWORD=your_password
   ```
3. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```
   Or on Windows:
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```
   API runs on `http://localhost:8080`.

---

## 🧪 Testing
Run the complete automated test suite (43 integration & unit tests):
```bash
./mvnw test
```

---

## 🔒 Security
- Live credentials are kept in `.env` and excluded via `.gitignore`.
