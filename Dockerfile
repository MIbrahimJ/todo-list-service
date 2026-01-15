# =========================
# Build Stage
# =========================
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN ./mvnw dependency:go-offline -B

COPY src src
RUN ./mvnw clean package -DskipTests

# =========================
# Runtime Stage
# =========================
FROM eclipse-temurin:17-jre
WORKDIR /app

ENV TZ=Europe/Berlin
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

ENV JAVA_TOOL_OPTIONS="-Duser.timezone=Europe/Berlin"

ENV SPRING_PROFILES_ACTIVE=docker,default

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
