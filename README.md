heartify-microservices/
├── eureka-server/              # Service Discovery Server
│   ├── src/
│   │   └── main/
│   │       ├── java/com/heartify/eureka/
│   │       │   └── EurekaServerApplication.java
│   │       └── resources/
│   │           └── application.yml
│   ├── pom.xml
│   └── Dockerfile
│
├── api-gateway/                # API Gateway with JWT Filter
│   ├── src/
│   │   └── main/
│   │       ├── java/com/heartify/gateway/
│   │       │   ├── ApiGatewayApplication.java
│   │       │   ├── config/
│   │       │   │   └── SecurityConfig.java
│   │       │   ├── filter/
│   │       │   │   └── AuthenticationFilter.java
│   │       │   └── util/
│   │       │       └── JwtUtil.java
│   │       └── resources/
│   │           └── application.yml
│   ├── pom.xml
│   └── Dockerfile
│
├── user-service/               # Main Business Logic Service
│   ├── src/
│   │   └── main/
│   │       ├── java/com/heartify/userservice/
│   │       │   ├── UserServiceApplication.java
│   │       │   ├── entity/
│   │       │   │   ├── User.java
│   │       │   │   ├── Device.java
│   │       │   │   ├── HealthRecord.java
│   │       │   │   ├── OtpVerification.java
│   │       │   │   └── RefreshToken.java
│   │       │   ├── repository/
│   │       │   │   ├── UserRepository.java
│   │       │   │   ├── DeviceRepository.java
│   │       │   │   ├── HealthRecordRepository.java
│   │       │   │   ├── OtpVerificationRepository.java
│   │       │   │   └── RefreshTokenRepository.java
│   │       │   ├── service/
│   │       │   │   ├── AuthService.java
│   │       │   │   ├── UserService.java
│   │       │   │   ├── DeviceService.java
│   │       │   │   └── EmailService.java
│   │       │   ├── controller/
│   │       │   │   ├── AuthController.java
│   │       │   │   ├── UserController.java
│   │       │   │   └── DeviceController.java
│   │       │   ├── dto/
│   │       │   │   ├── AuthDto.java
│   │       │   │   ├── UserDto.java
│   │       │   │   └── DeviceDto.java
│   │       │   ├── config/
│   │       │   │   └── SecurityConfig.java
│   │       │   ├── exception/
│   │       │   │   └── GlobalExceptionHandler.java
│   │       │   ├── grpc/
│   │       │   │   └── UserGrpcServiceImpl.java
│   │       │   └── util/
│   │       │       └── JwtUtil.java
│   │       ├── proto/
│   │       │   └── user_service.proto
│   │       └── resources/
│   │           └── application.yml
│   ├── pom.xml
│   └── Dockerfile
│
├── ai-service/                 # Placeholder for future AI features
│   └── README.md
│
├── docker-compose.yml          # Container orchestration
└── README.md                   # This file