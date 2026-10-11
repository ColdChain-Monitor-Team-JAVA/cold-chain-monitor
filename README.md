# ColdChain Monitor (coldchain-web)

Java 21+ / Spring Boot 3.5 MVC + Thymeleaf + Spring JDBC (JdbcTemplate) + PostgreSQL + Spring Security (BCrypt). No JavaScript is used: charts are server-rendered SVG.

## Run (Windows CMD)
```
cd coldchain-web
set DB_PASSWORD=your_local_postgres_password
set MVN=E:\Downloads\apache-maven-3.10.0-bin\apache-maven-3.10.0\bin\mvn.cmd
%MVN% test
%MVN% spring-boot:run
```
Optional: `set DB_USERNAME=postgres`, `set DB_URL=jdbc:postgresql://localhost:5432/coldchain_db?currentSchema=coldchain_web`. Open http://localhost:8080.
If the older backend already uses port 8080, stop it or add `-Dspring-boot.run.arguments=--server.port=8081`.

Tables are created in a separate schema `coldchain_web` of `coldchain_db` (`CREATE ... IF NOT EXISTS`, nothing is dropped). Demo users, products and shipments are seeded idempotently.

## Local-development demo accounts (not production credentials)
`admin@`, `warehouse@`, `transport@`, `driver@`, `hospital@`, `supplier@`, `inspector@` + `coldchain.local`; password `Admin@123` (`DEMO_PASSWORD`). Demonstration MFA code `123456` (`MFA_CODE`), 5-minute expiry, 5 attempts.

## Login and dashboards
Pick your role on the login page. If it does not match the role stored for that account you get "Selected role does not match this account." (the database role is always authoritative). After MFA each role lands on its own dashboard: `/dashboard/admin`, `/warehouse`, `/transport`, `/driver`, `/hospital`, `/supplier`, `/inspector`. Opening another role's dashboard URL gives "Access denied".

## Implemented
- Auth: BCrypt, demo MFA gate (expiry, attempt limit), CSRF, logout, role-based URL rules plus server-side data scoping (drivers: own shipments; suppliers: own products and their shipments; hospital staff: shipments addressed to them).
- Users (Admin): create, edit, role, activate/deactivate, generated temporary password (shown once), audit log.
- Inventory: products and batches (create, edit, delete with reference checks), search, low-stock/expiring/expired status, pagination.
- Shipments: create with unique tracking numbers and stock reservation (row locks), assignment, validated status transitions with history, cancellation releases stock, delivery confirmation with received quantity, notes and discrepancy alert (duplicate delivery blocked), search, filter, pagination.
- Simulation: Spring `@Scheduled` simulated temperature/humidity/GPS, product-specific thresholds, SVG charts. Also detects delayed shipments, stale sensors, expired and low-stock batches.
- Alerts: one unresolved alert per type per shipment/batch (a resolved alert never blocks a new one), acknowledge, investigation notes, resolve with note.
- Reports (role-restricted) with date/text filters, pagination and CSV export with formula-injection protection.

## Known limitations
- A shipment carries one product/batch line (no multi-line shipments).
- Alert assignment to a named person and per-alert status history tables are not implemented (acknowledge/resolve actors and timestamps are stored on the alert).
- A user deactivated or reset while logged in keeps their current session until it ends.
- Tests: unit tests only (rules, MFA, CSV). There are no database-backed integration tests yet.
- Nothing in this project has been compiled or run by its author in the build environment; see the delivery notes.
