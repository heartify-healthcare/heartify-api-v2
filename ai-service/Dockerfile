# ==========================================
# STAGE 1: Build (Sử dụng Maven trên nền Alpine - Nhẹ hơn)
# ==========================================
# Khác với service Proto, ở đây ta dùng bản '-alpine' vì không cần thư viện glibc cho protoc
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# 1. Copy file cấu hình dependency
COPY pom.xml .

# 2. Tải dependency (Cache layer này để build nhanh hơn ở các lần sau)
RUN mvn dependency:go-offline -B

# 3. Copy source code
COPY src ./src

# 4. Build application
RUN mvn clean package -DskipTests

# ==========================================
# STAGE 2: Runtime (Sử dụng JRE Alpine)
# ==========================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Tạo user non-root (Bảo mật)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy file jar từ builder
# Dùng wildcard *.jar để linh hoạt với version trong pom.xml
COPY --from=builder /app/target/*.jar app.jar

# Cấu hình tối ưu JVM cho Container
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]