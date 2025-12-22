
FROM eclipse-temurin:21-jdk-alpine as build
WORKDIR /workspace/app

COPY gradle gradle
COPY build.gradle settings.gradle gradlew ./
COPY src src

RUN ./gradlew build -x test

FROM eclipse-temurin:21-jre-alpine
VOLUME /tmp

COPY --from=build /workspace/app/build/libs/*.jar app.jar

ENTRYPOINT ["java","-jar","/app.jar"]
