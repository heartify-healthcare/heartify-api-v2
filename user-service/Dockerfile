# ==========================================
# STAGE 1: Build (Dùng Maven với JDK 21 - Temurin)
# ==========================================
# Thay đổi từ temurin-17 sang temurin-21
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# 1. Copy file cấu hình dependency
COPY pom.xml .

# 2. Tải toàn bộ dependency (Go Offline)
RUN mvn dependency:go-offline -B

# 3. Copy source code
COPY src ./src

# 4. Build application
RUN mvn clean package -DskipTests

# ==========================================
# STAGE 2: Runtime (Dùng JRE 21 Alpine)
# ==========================================
# Thay đổi từ temurin:17 sang temurin:21
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Tạo user non-root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy file jar
COPY --from=builder /app/target/*.jar app.jar

# Cấu hình JVM
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]