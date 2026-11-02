FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY src/main/java/edu/northstar/App.java ./App.java
RUN apt-get update && apt-get install -y --no-install-recommends curl && \
    mkdir -p build lib && \
    curl -fsSL https://jdbc.postgresql.org/download/postgresql-42.7.14.jar -o lib/postgresql.jar && \
    javac --add-modules jdk.httpserver -cp lib/postgresql.jar -d build App.java && \
    rm -rf /var/lib/apt/lists/*

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build ./build
COPY --from=build /app/lib ./lib
COPY src/main/resources/public/index.html ./index.html
COPY src/main/resources/public/styles.css ./styles.css
COPY src/main/resources/public/app.js ./app.js
ENV PORT=10000
EXPOSE 10000
CMD ["java", "--add-modules", "jdk.httpserver", "-cp", "build:lib/postgresql.jar", "edu.northstar.App"]
