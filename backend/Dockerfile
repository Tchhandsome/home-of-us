# syntax=docker/dockerfile:1.7

FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY deploy/maven/settings.xml /root/.m2/settings.xml
COPY backend/pom.xml backend/pom.xml
RUN mvn -s /root/.m2/settings.xml -f backend/pom.xml -ntp -DskipTests dependency:go-offline

COPY backend/src backend/src
RUN mvn -s /root/.m2/settings.xml -f backend/pom.xml -ntp -DskipTests package

FROM eclipse-temurin:17-jre-jammy

ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-Xms128m -Xmx512m"

WORKDIR /app
RUN mkdir -p /app/data/uploads

COPY --from=build /workspace/backend/target/home-of-us-backend-*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
