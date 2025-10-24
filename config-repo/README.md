# Spring Cloud Config Repository

This folder contains centralized configuration files for all microservices in the Heartify system.

## Files

- `eureka-server.yml` - Configuration for Eureka Service Registry
- `api-gateway.yml` - Configuration for API Gateway
- `user-service.yml` - Configuration for User Service

## Notes

- The Config Server reads from this directory using the native profile
- Each service fetches its configuration based on its `spring.application.name`
- Environment variables can still be used for sensitive values (e.g., `${DB_PASSWORD}`)
