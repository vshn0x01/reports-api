# Reports API (Spring Boot Backend)

Spring Boot equivalent of the Python `reports.api` backend with:
- JWT login
- role-based report access
- report run creation/list/download flow

## Endpoints

- `GET /health`
- `POST /auth/login`
- `GET /reports`
- `POST /reports/{reportId}/runs`
- `GET /reports/runs`
- `GET /reports/runs/{runId}/download`

## Configuration

Set these environment variables (or use defaults):

- `DATABASE_URL` (default: `jdbc:postgresql://localhost:5432/reports`)
- `DB_USERNAME` (default: `postgres`)
- `DB_PASSWORD` (default: `postgres`)
- `JWT_SECRET` (default is for local development only)
- `JWT_EXPIRATION_MINUTES` (default: `60`)

`application.properties` uses `spring.jpa.hibernate.ddl-auto=none`, so schema should already exist.

## Run

If Maven is installed:

```bash
mvn spring-boot:run
```

Then test:

```bash
curl http://localhost:8000/health
```
