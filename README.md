# ColdChain Monitor (full stack)

React + TypeScript + Vite + Tailwind frontend (`coldchain-monitor/`) and Java 21 + Spring Boot 3 backend (`coldchain-backend/`, package `com.coldchain.monitor`) with PostgreSQL.

## Setup
1. PostgreSQL: `createdb coldchain_monitor`
2. Backend env vars: `DB_URL` (default `jdbc:postgresql://localhost:5432/coldchain_monitor`), `DB_USERNAME` (default `postgres`), `DB_PASSWORD`
3. Backend: `cd coldchain-backend && mvn clean install && mvn spring-boot:run` (http://localhost:8080). Tables are created by Hibernate (`ddl-auto=update`) and the seven demo users are seeded on first start.
4. Frontend: `cd coldchain-monitor && npm install && npm run dev` (http://localhost:5173). Optional `VITE_API_URL` overrides `http://localhost:8080/api`.

## Demo credentials (development only)
`admin@`, `warehouse@`, `transport@`, `driver@`, `hospital@`, `supplier@`, `inspector@` + `coldchain.local`; password `Admin@123`; pick the matching role. MFA code `123456` (demo mechanism, not real email/SMS).

## API
- `POST /api/auth/login`, `POST /api/auth/mfa/verify`, `POST /api/auth/mfa/resend`, `GET /api/auth/me`, `POST /api/auth/logout`
- Administrator only: `GET/POST /api/users`, `GET/PUT/DELETE /api/users/{id}`, `PATCH /api/users/{id}/activate|deactivate`

## Security notes
BCrypt password hashes; role always read from the database (the login dropdown is only checked against it); opaque bearer tokens kept in backend memory (restart = sign out); `/api/users/**` enforced server-side.

## Not built yet
Monitoring/readings, shipments, alerts, GPS simulation, reports and quality-inspection APIs. Role dashboards still use the frontend demo data in `src/data/`.
