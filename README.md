# NexusHR

NexusHR is an enterprise HR/workforce platform built as a Java 21 and React 19 application. The repository is a Maven multi-module system of independently deployed Spring Boot services behind a Spring Cloud Gateway.

## Platform modules

| Module | Responsibility | Local port |
|---|---|---:|
| `api-gateway` | Routing, CORS, HttpOnly cookie-to-bearer bridge, WebSocket proxy | 8091 |
| `auth-service` | Invitation-based accounts, login, refresh rotation, logout, audit events | 8081 |
| `employee-service` | Employee profiles, organization hierarchy, scoped directory, lifecycle approvals, documents and workforce summary | 8082 |
| `payroll-service` | Configurable salary inputs, payroll runs, approval, payslips, XLSX/CSV/PDF exports | 8083 |
| `attendance-service` | Attendance, leave types/balances, approvals, holidays, biometric simulation | 8084 |
| `performance-service` | Goals, self/manager reviews, scoped 360-degree feedback invitations | 8085 |
| `ai-insights-service` | Explainable workforce indicators, Spring AI provider, deterministic fallback | 8086 |
| `notification-service` | In-app notifications, authenticated WebSocket delivery, optional SMTP | 8087 |

Each persistence service owns a PostgreSQL schema and Flyway migrations. The current deployment is a microservice-style modular platform, rather than a single-process modular monolith.

See [docs/architecture.md](./docs/architecture.md) for the architecture, data relationships, endpoint families, and implementation boundaries.

## Local setup (Windows PowerShell)

Requirements: Docker Desktop with Compose, Java 21, Maven, Node 20, and pnpm 9. The Compose stack does not require paid services or an LLM key.

Generate local-only secrets and a bootstrap administrator, then start the platform:

```powershell
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtBytes = New-Object byte[] 32
$dbBytes = New-Object byte[] 24
$adminBytes = New-Object byte[] 24
$grafanaBytes = New-Object byte[] 24
$notificationBytes = New-Object byte[] 32
$rng.GetBytes($jwtBytes)
$rng.GetBytes($dbBytes)
$rng.GetBytes($adminBytes)
$rng.GetBytes($grafanaBytes)
$rng.GetBytes($notificationBytes)
$jwtSecret = [Convert]::ToBase64String($jwtBytes)
$dbPassword = [Convert]::ToBase64String($dbBytes)
$adminPassword = [Convert]::ToBase64String($adminBytes)
$grafanaPassword = [Convert]::ToBase64String($grafanaBytes)
$notificationToken = [Convert]::ToBase64String($notificationBytes)
@"
DB_USERNAME=nexushr
POSTGRES_DB=nexushr_auth
DB_PASSWORD=$dbPassword
JWT_SECRET=$jwtSecret
INTERNAL_NOTIFICATION_TOKEN=$notificationToken
COOKIE_SECURE=false
JWT_ACCESS_TOKEN_TTL_MS=900000
PASSWORD_RESET_EMAIL_ENABLED=true
FRONTEND_BASE_URL=http://localhost:3000
PASSWORD_RESET_TOKEN_TTL_MINUTES=30
NEXUSHR_BOOTSTRAP_ADMIN_NAME=Development Administrator
NEXUSHR_BOOTSTRAP_ADMIN_EMAIL=admin@example.test
NEXUSHR_BOOTSTRAP_ADMIN_PASSWORD=$adminPassword
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=$grafanaPassword
EMAIL_ENABLED=false
"@ | Set-Content -Path .env -Encoding ascii
Write-Output "Bootstrap admin: admin@example.test"
Write-Output "Bootstrap password: $adminPassword"
Write-Output "Grafana user: admin"
Write-Output "Grafana password: $grafanaPassword"
docker compose up --build
```

The bootstrap user is created only when the auth schema has no `SUPER_ADMIN` and bootstrap variables are configured; existing employee or HR accounts do not prevent initial super-admin provisioning. Invitation-based registration is used for subsequent accounts. Keep `.env` local; generated values are for development only.

## Viewing role-specific dashboards

The dashboard is selected from the signed-in account's role; there is intentionally no client-side role switch. Use the bootstrap `SUPER_ADMIN` account for the full HR/admin view and sign in with an `EMPLOYEE` account for the employee view. To try the manager view, an administrator creates an invitation with role `MANAGER` or `DEPARTMENT_MANAGER` through `POST /api/auth/invitations`, then the invitee registers with the returned invitation token at `POST /api/auth/register`. Manager directory data is scoped to the manager's employee/department relationships, so create and associate the corresponding employee profile and department for meaningful results. Invitations can also be created from the Auth Swagger UI; no manager demo account is seeded by default.

HR administrators and super administrators can also create invitations from **Employees → Invite a new employee**. Enter the employee's work email and role, create the invitation, and securely share the generated registration link with that person. The employee opens the link, confirms their details, and creates a password; successful registration signs them in. The invited employee then completes **My Profile**, after which HR reviews the pending onboarding request. Invitation links are single-use and expire after seven days. Treat the link as a secret and send it only to the intended recipient. A `SUPER_ADMIN` can invite every role; an `HR_ADMIN` can invite employee, manager, department manager, or HR manager roles.

Open the UI at `http://localhost:3000`. API requests go through the gateway at `http://localhost:8091`. PostgreSQL is exposed on `5434`, Redis on `6381`, the local password-reset email inbox (Mailpit) on `8025`, Prometheus on `9090`, and Grafana on `3001`.

The password-reset workflow is enabled locally and sends messages to Mailpit; use `http://localhost:8025` to open the inbox. For deployment, configure `PASSWORD_RESET_EMAIL_ENABLED=true`, `FRONTEND_BASE_URL`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_FROM`, and provider credentials in `.env`; configure TLS/auth to match the provider. If reset email is not configured, its API returns `503` rather than claiming delivery. Delivery runs after the reset transaction commits; failures are recorded in auth audit events and application logs while the public request response remains generic to prevent account enumeration. The separate notification email integration remains disabled unless `EMAIL_ENABLED=true`. WebSocket origins must be listed in `APP_ALLOWED_ORIGINS`.

Automated leave-review, attendance check-in/out, payroll-run approval, onboarding, and offboarding notifications use a transactional outbox in each producing service. The event is committed atomically with the HR operation, and a scheduled relay retries HTTP delivery with bounded exponential backoff and a stable event ID. The notification service deduplicates by that ID, persists the in-app notification, and queues enabled email delivery in its own database. Email failures are retried and terminal failures are visible to `SUPER_ADMIN`/`HR_ADMIN` at `GET /api/notifications/admin/delivery-failures`; authorized administrators can requeue a failure at `PUT /api/notifications/admin/delivery-failures/{id}/retry`. WebSocket updates are published after commit; persisted in-app notifications remain available if a client is offline. Configure a shared `INTERNAL_NOTIFICATION_TOKEN` and keep it in a Kubernetes Secret as `APP_NOTIFICATIONS_INTERNAL_TOKEN`. Delivery polling, batch, lease and retry limits are configurable through `.env`; defaults are documented in `.env.example`.

## Build and verification

From the repository root:

```powershell
mvn verify
Set-Location frontend
pnpm install --frozen-lockfile
pnpm lint
pnpm build
Set-Location ..
$env:DB_PASSWORD = "compose-config-check"
$env:JWT_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
$env:INTERNAL_NOTIFICATION_TOKEN = "compose-config-check"
$env:GRAFANA_ADMIN_PASSWORD = "compose-config-check"
docker compose config --quiet
```

The Compose configuration check validates configuration only; it does not start containers. The Maven verification includes PostgreSQL 17 Testcontainers tests for auth, employee, attendance, payroll, performance, and notifications. They validate Flyway migrations and start each service on a random port; Testcontainers skips these tests when Docker is unavailable, so a successful build without Docker does not verify database migrations. On 2026-11-03, `mvn clean verify` ran 72 tests with zero skips, failures, or errors. All six persistence services migrated and started against PostgreSQL 17.11; container-backed tests also exercise producer outbox commit/rollback, retry and stable event IDs, notification idempotency, email retry/dead-letter tracking, and administrator requeue. The AI service has both a credential-free deterministic startup test and a Spring AI HTTP protocol test against a local provider stub. A call to an actual paid OpenAI account still requires deployment credentials and has not been performed.

The full Compose stack was also started against PostgreSQL 17.11 and Redis 7: frontend, gateway, all seven application services, PostgreSQL, and Redis reported healthy. Gateway and service actuator health endpoints returned HTTP 200. A live cookie-authenticated super-admin smoke test returned HTTP 200 for auth/session, employee directory and analytics, departments, attendance, leave types and holiday calendar, payroll runs, pending reviews, notifications, and all three deterministic AI insight endpoints. Password reset was exercised end to end through the frontend proxy: request, local Mailpit delivery, single-use token completion, and login with the replacement password. Frontend lint/build, Compose configuration, and Kubernetes rendering passed. The frontend health probe uses `127.0.0.1` so it works in containers where `localhost` resolves to IPv6 while Nginx listens on IPv4.

## API documentation

OpenAPI/Swagger UI is available directly on each service when running with Compose:

- Auth: `http://localhost:8081/swagger-ui/index.html` (includes reset request and completion endpoints)
- Employees: `http://localhost:8082/swagger-ui/index.html`
- Payroll: `http://localhost:8083/swagger-ui/index.html`
- Attendance/leave: `http://localhost:8084/swagger-ui/index.html`
- Performance: `http://localhost:8085/swagger-ui/index.html`
- AI insights: `http://localhost:8086/swagger-ui/index.html`
- Notifications: `http://localhost:8087/swagger-ui/index.html`

Protected operations require a valid access token in the Swagger **Authorize** dialog. In the browser application, access and rotating refresh tokens are held in `HttpOnly` cookies; they are not stored in `localStorage`.

## Demo and policy boundaries

- There are no hard-coded demo credentials. Use the bootstrap values generated above.
- Biometric attendance is explicitly a simulation; it does not connect to hardware.
- Leave allowances are configurable sample values, not jurisdictional policy.
- Payroll uses the configured percentage and deduction inputs. Tax values are estimates and are not represented as legally compliant tax calculations.
- AI insights keep risk/engagement scores deterministic and explainable. Recommendation generation defaults to deterministic rules and starts without provider credentials; unused model types are disabled unless configured. Optionally set `APP_AI_PROVIDER=openai`, `SPRING_AI_CHAT_MODEL=openai`, and `OPENAI_API_KEY` (server-side secret only) to use Spring AI with OpenAI. `SPRING_AI_OPENAI_BASE_URL`, `OPENAI_MODEL`, `AI_PROVIDER_CONNECT_TIMEOUT` (default `PT3S`), and `AI_PROVIDER_READ_TIMEOUT` (default `PT15S`) are external configuration. The provider makes one bounded attempt; failures are logged and use deterministic fallback. Requests exclude employee email but may send submitted workforce metrics, target roles, and skill gaps to the configured provider. Outputs are not validated probabilities or facts; human review is mandatory before employment decisions. A local HTTP provider test verifies Spring AI request/response wiring; an actual OpenAI API request still requires deployment credentials.
- Payroll runs support native `.xlsx` workbooks with typed numeric cells at `GET /api/payroll/runs/{id}/export.xlsx`, in addition to CSV and PDF exports.
- SMTP is optional and disabled by default. Redis is provisioned for local infrastructure but is not currently required by the service execution path.

## Operations and deployment

- CI: `.github/workflows/ci.yml` runs Maven tests, frontend lint/build, and Compose validation.
- Containers: root `Dockerfile` builds one backend module at a time; `frontend/Dockerfile` builds the UI.
- Employee documents: local Compose storage is private to the service and persisted in a named volume; Kubernetes is configured for private S3 storage with server-side encryption. Provision the bucket and grant the employee-service workload least-privilege S3 access before enabling it. Uploads accept signature-checked PDF/PNG/JPEG files up to 10 MB; access is owner-or-HR only and uploads/downloads are audited.
- Employee lifecycle: self-service or HR-created onboarding profiles remain in `ONBOARDING` until another HR administrator approves them. Employees can request offboarding; HR may also submit requests on an employee's behalf. Only `SUPER_ADMIN`, `HR_ADMIN`, or `HR_MANAGER` can review, requesters cannot review their own submissions, rejection requires a note, and approved offboarding changes the employee to `TERMINATED`. Direct status changes cannot bypass this workflow.
- Local monitoring: Prometheus scrapes Actuator metrics; Grafana is provisioned with the platform availability dashboard.
- Kubernetes: `k8s/` contains deployment, service, ingress, configuration, and persistent PostgreSQL resources. Create the `nexushr-secrets` Secret out of band before applying; do not commit production secrets. Replace the sample image names and ingress host with your registry/domain.

Production deployment still requires organization-specific review of network policy, secret management, TLS, backups, SMTP, retention, and applicable employment/tax rules.
