# ── Stage 1: Build ───────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies separately (cached layer)
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -DskipTests -q

# ── Stage 2: Runtime ─────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Create a non-root user for security
RUN addgroup --system crm && adduser --system --ingroup crm crm
USER crm

COPY --from=build /app/target/barometer-crm-*.jar app.jar

EXPOSE 8080

# JVM flags for containers: respect cgroup memory limits
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djdk.tls.client.protocols=TLSv1.2 -Djsse.enableSNIExtension=true"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
