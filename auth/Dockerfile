FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY . .

# Fase 1: descargar dependencias. En un runner ARM nativo la red es estable, pero
# se reintenta igual porque un fallo transitorio tira abajo la compilacion entera.
RUN --mount=type=cache,target=/root/.m2 set -e; \
    find /root/.m2 -name "*.lastUpdated" -delete || true; \
    for i in $(seq 1 5); do \
      echo "=== Downloading dependencies: attempt $i/5 ==="; \
      if mvn -B -ntp -s /build/settings.xml dependency:go-offline -DskipTests \
           -Dmaven.wagon.http.retryHandler.count=10 \
           -Daether.syncContext.named.factory=noop; then \
        echo "=== Dependencies resolved on attempt $i ==="; break; \
      fi; \
      echo "--- attempt $i failed, cache is kept for the next one ---"; sleep 5; \
    done

# Fase 2: compilar sin red. dependency:go-offline no siempre cubre las extensiones
# de build de Quarkus, asi que si la compilación offline falla se reintenta online.
RUN --mount=type=cache,target=/root/.m2 set -e; \
    start=$(date +%s); \
    if ! mvn -o -B -ntp -s /build/settings.xml clean package -T 1C -DskipTests \
         -Dquarkus.package.jar.type=uber-jar -Daether.syncContext.named.factory=noop; then \
      echo "=== OFFLINE BUILD FAILED: falling back to online build ==="; \
      mvn -B -ntp -s /build/settings.xml clean package -T 1C -DskipTests \
          -Dquarkus.package.jar.type=uber-jar -Daether.syncContext.named.factory=noop; \
    fi; \
    end=$(date +%s); \
    echo "Build took $((end - start)) seconds."; \
    echo "Generated JARs:"; \
    ls -lh /build/target/*-runner.jar || (echo "ERROR: no JAR was produced" && exit 1)

FROM registry.access.redhat.com/ubi8/openjdk-21-runtime:1.20
WORKDIR /app
COPY --from=builder /build/target/*-runner.jar app.jar
EXPOSE 8082
ENV TZ=UTC
ENV JAVA_OPTS="-Duser.timezone=UTC -XX:+UseContainerSupport"
ENTRYPOINT ["java", "-jar", "app.jar"]
