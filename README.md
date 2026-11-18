# IdentityService

Authentication and authorization microservice of the TenderFlow platform (RBAC + JWT access token + rotating refresh token).

## Run locally (Eclipse)
Create the database `identityservice_db`, then set these environment variables in the Eclipse run configuration:
`SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET` (>= 32 chars), and optionally `ADMIN_EMAIL`, `ADMIN_PASSWORD`.
The service listens on http://localhost:8082.

## Run with Docker
```
docker network create platform-net      # once
copy .env.example .env                  # then edit the values
docker compose up --build
```

## Default roles / permissions (seeded at startup, only the missing ones)
| Role | Purpose | Permissions |
|---|---|---|
| ADMIN | Full access | all |
| SECURITY_ADMIN | Accounts, roles, audit (no tender content) | ACCOUNT_MANAGE, ROLE_MANAGE, AUDIT_READ |
| MANAGER | Manages tenders, statuses, files | TENDER_READ_ALL, TENDER_WRITE, TENDER_DELETE, TENDER_STATUS_MANAGE, NOTIFICATION_SEND, NOTIFICATION_READ, DOCUMENT_READ, DOCUMENT_WRITE, DOCUMENT_DELETE, REPORT_READ |
| EDITOR | Creates / edits tenders and files, no delete | TENDER_READ_ALL, TENDER_WRITE, DOCUMENT_READ, DOCUMENT_WRITE |
| AUDITOR | Read-only supervisor | TENDER_READ_ALL, AUDIT_READ, REPORT_READ |
| USER | Reads open tenders only | TENDER_READ |
| SERVICE | Technical account for the microservices | TENDER_READ_ALL, NOTIFICATION_SEND, AUDIT_WRITE, SEARCH_INDEX |

Technical account: set `SERVICE_ACCOUNT_EMAIL` and `SERVICE_ACCOUNT_PASSWORD` (created at startup with role SERVICE).
Existing database: new permissions and the SERVICE role are added, ADMIN gets every permission, but the other existing roles are never modified:
give them the new permissions with `PUT /api/roles/{id}`, or recreate the database.

New roles can be created at runtime with `/api/roles` (permission ROLE_MANAGE), no code change needed.

## Endpoints
| Method | URL | Access |
|---|---|---|
| POST | /api/auth/register | public (role USER) |
| POST | /api/auth/login | public, returns accessToken + refreshToken |
| POST | /api/auth/refresh | public, rotates the refresh token |
| POST | /api/auth/logout | public, revokes the refresh token |
| GET | /api/accounts/me | any authenticated user (used by TenderFlow through Feign) |
| GET/POST/PUT/DELETE | /api/accounts, /api/accounts/{id} | ACCOUNT_MANAGE |
| PATCH | /api/accounts/{id}/status?value=ACTIVE\|SUSPENDED\|DISABLED | ACCOUNT_MANAGE |
| CRUD | /api/roles, /api/permissions | ROLE_MANAGE |
| GET | /api/audit-logs[?accountId=] | AUDIT_READ |

## Test with Postman
1. POST /api/auth/login with `{"email":"...","password":"..."}`; copy `accessToken`.
2. On the next requests: Authorization tab, type Bearer Token, paste the token.
3. When it expires (15 min by default): POST /api/auth/refresh with `{"refreshToken":"..."}`.

## Front end (browser)
`IdentityService_Front` (Angular) calls this API directly: its origin must be listed in `CORS_ALLOWED_ORIGINS`
(default `http://localhost:4200,http://localhost:4201,http://localhost:4202,http://localhost:4203,http://localhost:4204,http://localhost:4205,http://localhost:58020`). Through the ApiGateway, CORS is handled by the gateway.
