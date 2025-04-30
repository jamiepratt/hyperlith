FROM clojure:tools-deps-bullseye-slim AS builder

WORKDIR /opt

COPY . .

RUN clojure -Srepro -T:build uber

FROM eclipse-temurin:22 AS runtime
COPY --from=builder /opt/target/app.jar /app.jar

EXPOSE 8080
EXPOSE 5555

ENTRYPOINT ["java", "-cp", "app.jar", "clojure.main", "-m", "app.main"]

