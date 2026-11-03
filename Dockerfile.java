FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY backend/pom.xml backend/pom.xml
COPY demo/pom.xml demo/pom.xml
COPY workload/pom.xml workload/pom.xml
RUN mvn -B dependency:go-offline -DskipTests
COPY backend/src backend/src
COPY demo/src demo/src
COPY workload/src workload/src
RUN mvn -B package -DskipTests
ADD https://repo.maven.apache.org/maven2/io/opentelemetry/javaagent/opentelemetry-javaagent/2.22.0/opentelemetry-javaagent-2.22.0.jar /otel-agent.jar
RUN chmod 644 /otel-agent.jar

FROM eclipse-temurin:21-jre-alpine AS backend
RUN addgroup -S nexus && adduser -S nexus -G nexus
WORKDIR /app
COPY --from=build /workspace/backend/target/backend-1.0.0.jar app.jar
COPY --from=build /otel-agent.jar otel-agent.jar
USER nexus
ENTRYPOINT ["java","-javaagent:/app/otel-agent.jar","-XX:MaxRAMPercentage=65","-jar","/app/app.jar"]

FROM eclipse-temurin:21-jre-alpine AS workload
RUN addgroup -S nexus && adduser -S nexus -G nexus
WORKDIR /app
COPY --from=build /workspace/workload/target/workload-1.0.0.jar app.jar
COPY --from=build /otel-agent.jar otel-agent.jar
USER nexus
ENTRYPOINT ["java","-javaagent:/app/otel-agent.jar","-XX:MaxRAMPercentage=65","-jar","/app/app.jar"]

FROM eclipse-temurin:21-jre-alpine AS demo
RUN addgroup -S nexus && adduser -S nexus -G nexus
WORKDIR /app
COPY --from=build /workspace/demo/target/demo-1.0.0.jar app.jar
COPY --from=build /otel-agent.jar otel-agent.jar
USER nexus
ENTRYPOINT ["java","-javaagent:/app/otel-agent.jar","-XX:MaxRAMPercentage=65","-jar","/app/app.jar"]
