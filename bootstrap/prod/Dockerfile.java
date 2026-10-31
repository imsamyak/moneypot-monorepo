# One image recipe for the three services: a small Java runtime and the jar deploy.sh built (JAR build argument).
FROM eclipse-temurin:21-jre-alpine
ARG JAR
RUN addgroup -S app && adduser -S app -G app && mkdir /data && chown app /data
WORKDIR /app
COPY ${JAR} app.jar
USER app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
