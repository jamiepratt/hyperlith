FROM clojure:tools-deps-bullseye-slim AS builder

WORKDIR /opt

COPY . .

RUN clojure -Srepro -T:build uber

FROM eclipse-temurin:22 AS runtime
COPY --from=builder target/app.jar /app.jar

EXPOSE 8080
EXPOSE 5555

ENTRYPOINT ["java", "-cp", "app.jar", "clojure.main", "-m", "app.main", "-D", "clojure.server.repl=\"{:port 5555 :accept clojure.core.server/repl}\""]

