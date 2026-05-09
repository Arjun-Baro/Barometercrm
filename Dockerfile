# ── Stage 1: Build ───────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY barometer-crm-backend/pom.xml .
RUN mvn dependency:go-offline -q
COPY barometer-crm-backend/src ./src
RUN mvn clean package -DskipTests -q

# ── Stage 2: Runtime ─────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN addgroup --system crm && adduser --system --ingroup crm crm
USER crm

COPY --from=build /app/target/barometer-crm-*.jar app.jar

EXPOSE 10000

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
