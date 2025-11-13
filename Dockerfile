FROM amazoncorretto:17-alpine
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java","-jar","/app.jar"]

#FROM gradle:8.14.3-jdk17 AS builder
#WORKDIR /app
#COPY build.gradle settings.gradle ./
#COPY src ./src
#RUN gradle clean build -x test
#
#FROM amazoncorretto:17-alpine
#WORKDIR /app
#COPY --from=builder /app/build/libs/*.jar app.jar
##EXPOSE 8080
#ENTRYPOINT ["java", "-jar", "app.jar"]