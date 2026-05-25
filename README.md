# Cubeia Wallet

REST bookkeeping service. Work in progress.

## Requirements

JDK 17, 21, or 23. (JDK 24+ untested — Mockito / ByteBuddy versions are pinned
in `pom.xml` for forward compatibility, but please verify.)

## Build & run

```bash
./mvnw test
./mvnw spring-boot:run
```

Health check:

```bash
curl http://localhost:8080/health
# {"status":"ok"}
```
