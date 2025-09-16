# AI Auto Poster - Environment Setup Guide

## Prerequisites

### Required Software
- **Java 8+** ✅ (Already installed: 1.8.0_202)
- **Maven 3.6+** (Need to install)
- **MySQL 8.0+** (Need to install and configure)
- **Git** (For version control)

### External Service Accounts
- **OpenAI Account** (For AI content generation)
- **Azure OpenAI Account** (Backup AI service)
- **LinkedIn Developer Account** (For post publishing)
- **SMTP Email Service** (For notifications)

## Step 1: Install Maven

### Windows Installation
```powershell
# Download Maven from https://maven.apache.org/download.cgi
# Extract to C:\Program Files\Apache\maven
# Add to PATH environment variable
$env:PATH += ";C:\Program Files\Apache\maven\bin"

# Verify installation
mvn --version
```

### Alternative: Use Maven Wrapper (Recommended)
```powershell
# Generate Maven wrapper in project root
mvn -N io.takari:maven:wrapper

# Use wrapper instead of global Maven
./mvnw clean install
```

## Step 2: Database Setup

### Install MySQL
```sql
-- Download MySQL 8.0+ from https://dev.mysql.com/downloads/mysql/
-- Install with default settings

-- Create database and user
CREATE DATABASE aiautoposter;
CREATE USER 'aiautoposter'@'localhost' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON aiautoposter.* TO 'aiautoposter'@'localhost';
FLUSH PRIVILEGES;
```

### Update Database Configuration
Edit `src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/aiautoposter
    username: aiautoposter
    password: your_secure_password
```

## Step 3: Environment Variables Configuration

### Create Environment File
Create `.env` file in project root:
```env
# Database Configuration
DB_URL=jdbc:mysql://localhost:3306/aiautoposter
DB_USERNAME=aiautoposter
DB_PASSWORD=your_secure_password

# JWT Configuration
JWT_SECRET=your-super-secret-jwt-key-at-least-256-bits-long
JWT_EXPIRATION=86400

# OpenAI Configuration
OPENAI_API_KEY=sk-your-openai-api-key-here
OPENAI_API_URL=https://api.openai.com/v1

# Azure OpenAI Configuration (Fallback)
AZURE_OPENAI_API_KEY=your-azure-openai-key
AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com/
AZURE_OPENAI_DEPLOYMENT_NAME=your-deployment-name

# LinkedIn OAuth Configuration
LINKEDIN_CLIENT_ID=your-linkedin-client-id
LINKEDIN_CLIENT_SECRET=your-linkedin-client-secret
LINKEDIN_REDIRECT_URI=http://localhost:8080/api/linkedin/callback

# Email Configuration
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
MAIL_FROM=noreply@aiautoposter.com
```

### Windows Environment Variables
```powershell
# Set environment variables in Windows
[Environment]::SetEnvironmentVariable("OPENAI_API_KEY", "sk-your-key", "User")
[Environment]::SetEnvironmentVariable("JWT_SECRET", "your-secret", "User")
# ... repeat for all variables
```

## Step 4: External Service Setup

### OpenAI API Setup
1. Visit https://platform.openai.com/
2. Create account and navigate to API Keys
3. Generate new API key
4. Add to environment variables as `OPENAI_API_KEY`

### Azure OpenAI Setup (Optional Fallback)
1. Visit https://azure.microsoft.com/en-us/products/cognitive-services/openai-service
2. Create Azure OpenAI resource
3. Deploy a model (e.g., gpt-3.5-turbo)
4. Get endpoint and API key
5. Add to environment variables

### LinkedIn Developer Setup
1. Visit https://developer.linkedin.com/
2. Create new application
3. Configure OAuth redirect URLs
4. Get Client ID and Client Secret
5. Add to environment variables

### Email Service Setup
For Gmail:
1. Enable 2-factor authentication
2. Generate App Password
3. Use App Password in MAIL_PASSWORD

## Step 5: Application Configuration

### Update application.yml
```yaml
server:
  port: 8080

spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/aiautoposter}
    username: ${DB_USERNAME:aiautoposter}
    password: ${DB_PASSWORD:password}
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
  
  mail:
    host: ${MAIL_HOST:smtp.gmail.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

jwt:
  secret: ${JWT_SECRET:defaultSecretKey}
  expiration: ${JWT_EXPIRATION:86400}

ai:
  openai:
    api-key: ${OPENAI_API_KEY:}
    api-url: ${OPENAI_API_URL:https://api.openai.com/v1}
  azure:
    api-key: ${AZURE_OPENAI_API_KEY:}
    endpoint: ${AZURE_OPENAI_ENDPOINT:}
    deployment-name: ${AZURE_OPENAI_DEPLOYMENT_NAME:}

linkedin:
  client-id: ${LINKEDIN_CLIENT_ID:}
  client-secret: ${LINKEDIN_CLIENT_SECRET:}
  redirect-uri: ${LINKEDIN_REDIRECT_URI:http://localhost:8080/api/linkedin/callback}

logging:
  level:
    com.aiautoposter: INFO
    org.springframework.security: DEBUG
```

## Step 6: Build and Run

### Build Application
```powershell
# Clean and compile
mvn clean compile

# Run tests (optional)
mvn test

# Package application
mvn package

# Run application
java -jar target/ai-auto-poster-1.0.0.jar
```

### Alternative: Run with Maven
```powershell
mvn spring-boot:run
```

### Development Mode
```powershell
# Run with development profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Step 7: Verify Installation

### Health Check Endpoints
```bash
# Application health
curl http://localhost:8080/actuator/health

# Database connection
curl http://localhost:8080/actuator/health/db
```

### Test API Endpoints
```bash
# Register new user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "firstName": "Test",
    "lastName": "User",
    "role": "USER",
    "department": "Engineering"
  }'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

### Access Frontend
Open browser and navigate to: http://localhost:8080

## Troubleshooting

### Common Issues

#### Maven Not Found
```powershell
# Add Maven to PATH
$env:PATH += ";C:\Program Files\Apache\maven\bin"
```

#### Database Connection Failed
- Verify MySQL is running
- Check database credentials
- Ensure database exists
- Check firewall settings

#### API Keys Not Working
- Verify environment variables are set
- Check API key validity
- Ensure sufficient API credits/quota

#### Port Already in Use
```yaml
# Change port in application.yml
server:
  port: 8081
```

#### CORS Issues
- Check CORS configuration in SecurityConfig
- Verify frontend URL in allowed origins

### Logs Location
- Application logs: `logs/application.log`
- Spring Boot logs: Console output
- Database logs: MySQL error log

### Performance Tuning
```yaml
# Add to application.yml for production
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true
  
server:
  tomcat:
    max-threads: 200
    min-spare-threads: 10
```

## Production Deployment

### Environment-Specific Configurations
- **Development**: `application-dev.yml`
- **Testing**: `application-test.yml`
- **Production**: `application-prod.yml`

### Security Considerations
- Use strong JWT secrets (256+ bits)
- Enable HTTPS in production
- Secure database credentials
- Use environment-specific API keys
- Enable security headers
- Configure rate limiting

### Monitoring
- Enable Actuator endpoints
- Set up application monitoring (e.g., Micrometer)
- Configure log aggregation
- Set up health checks

## Support

### Documentation
- Spring Boot: https://spring.io/projects/spring-boot
- Spring Security: https://spring.io/projects/spring-security
- MySQL: https://dev.mysql.com/doc/

### Getting Help
- Check application logs first
- Verify environment configuration
- Test external API connectivity
- Review database connectivity

This guide provides complete setup instructions for the AI Auto Poster backend. Follow each step carefully and verify functionality at each stage.
