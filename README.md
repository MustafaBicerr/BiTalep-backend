# BiTalep API

Java 21 / Spring Boot 3 SaaS backend for the BiTalep request panel.

## Local

```bash
cp .env.example .env
colima start   # if Docker daemon is down
docker compose up --build
curl -sf http://localhost:8080/actuator/health
```

Postgres is **not** published on host 5432. API is on `8080`. Mailpit UI: `http://localhost:8025`.

## Demo login

See [docs/DEMO_CREDENTIALS.md](docs/DEMO_CREDENTIALS.md). Seed only via HTTP:

```bash
./scripts/seed-demo.sh http://localhost:8080
```

## Tests

```bash
./scripts/test-auth.sh
./scripts/test-isolation.sh
```
