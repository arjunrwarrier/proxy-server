FROM amazoncorretto:17-alpine
WORKDIR /app
COPY target/proxy-server-*.jar app.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "app.jar"]