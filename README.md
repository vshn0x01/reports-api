# Reports API (Spring Boot Backend)

Spring Boot equivalent of the Python `reports.api` backend with:
- JWT login
- role-based report access
- report run creation/list/download flow

## Endpoints

- `GET /health`
- `POST /auth/login`
- `GET /reports`
- `GET /reports/filters`
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

Filter options endpoint:

```bash
curl -H "Authorization: Bearer <jwt-token>" \
  "http://localhost:8000/reports/filters?sbuId=1&zoneId=2&clusterId=3&regionId=4&unitId=5"
```

All query params are optional. API returns scope-aware options for:
`sbu`, `zone`, `cluster`, `region`, `unit`, `branch`.

## Schema requirements

For the region-level hierarchy and scoped filtering, apply:
- `SCHEMA_REQUIREMENTS.sql`
