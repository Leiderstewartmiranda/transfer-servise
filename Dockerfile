# =====================================================
# Stage 1: Build
# =====================================================
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copiar solo el pom primero para aprovechar cache de Maven
COPY pom.xml .
RUN apk add --no-cache maven && \
    mvn dependency:go-offline -q

# Copiar fuentes y compilar (sin tests para build rápido)
COPY src ./src
RUN mvn package -DskipTests -q

# =====================================================
# Stage 2: Runtime (imagen mínima)
# =====================================================
FROM eclipse-temurin:17-jre-alpine AS runtime

# Usuario no-root por seguridad
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

WORKDIR /app

# Copiar JAR desde el builder
COPY --from=builder /app/target/*.jar app.jar

# Puerto de la aplicación
EXPOSE 8080

# Health check interno de Docker
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# Opciones JVM para contenedor
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
