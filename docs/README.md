# Money Transfer System 💰

A full-stack money transfer application with secure authentication, account management, and real-time transaction processing.

## Tech Stack

**Backend:** Java 17, Spring Boot 3.2, Spring Security, JWT, MySQL, Maven  
**Frontend:** Angular 19, Angular Material, TypeScript, Chart.js  

## Features 📍

- JWT-based authentication with role-based access (USER/ADMIN)
- Money transfers between accounts with validation
- Transaction history and account dashboard
- Admin panel for user and transaction management
- Real-time analytics with charts
- Responsive Material Design UI

## Getting Started

### Prerequisites

- Java JDK 17+
- Maven 3.6+
- Node.js 18+
- MySQL 8.0+

### Backend Setup

1. Clone and navigate to backend:
   ```bash
   git clone https://github.com/yourusername/money-transfer-system.git
   cd money-transfer-system/backend
   ```

2. Create MySQL database:
   ```sql
   CREATE DATABASE money_transfer_db;
   ```

3. Configure `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/money_transfer_db
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   jwt.secret=your-secret-key
   ```

4. Run the application:
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```
   Backend runs at `http://localhost:8080`

### Frontend Setup

1. Navigate to frontend and install dependencies:
   ```bash
   cd frontend
   npm install
   ```

2. Start development server:
   ```bash
   npm start
   ```
   Frontend runs at `http://localhost:4200`

## API Documentation 📄

Swagger UI: `http://localhost:8080/swagger-ui.html`

**Key Endpoints:** 📌
- `POST /api/auth/register` - Register user
- `POST /api/auth/login` - Login
- `POST /api/transfers` - Create transfer
- `GET /api/accounts` - Get accounts

## Project Structure

```
money-transfer-system/
├── backend/
│   └── src/main/java/com/moneytransfersystem/
│       ├── config/          # Security, CORS configuration
│       ├── controller/      # REST controllers
│       ├── domain/          # Entities, DTOs, enums
│       ├── repository/      # Data access
│       └── service/         # Business logic
└── frontend/
    └── src/app/
        ├── components/      # UI components
        ├── services/        # API services
        └── guards/          # Route guards
```

## Testing

**Backend:**
```bash
mvn test
```

**Frontend:**
```bash
npm test
```

## License

MIT License
