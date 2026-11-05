# ReliefGrid — Security Notes (Step 6)

## Authentication
- **Access tokens:** short-lived JWTs (15 min default), HS256, signed with a secret of 32+ bytes from `JWT_SECRET`. Never logged, never stored server-side.
- **Refresh tokens:** opaque random values, never JWTs. Only their SHA-256 hash is stored in `refresh_tokens`, so a database leak alone can't be used to log in.
- **Rotation:** every refresh issues a new refresh token and immediately revokes the old one (one-time use).
- **Reuse detection:** if an already-used or expired refresh token is presented again, the entire token family is revoked and the user must log in again. This is the standard response to a stolen refresh token being replayed.
- **Logout:** revokes all of the user's refresh tokens, ending every active session, not just the current device.

## Authorization
- Role-based access control on every non-auth endpoint via `@PreAuthorize`.
- Ownership checks in service code where role alone isn't enough (e.g., an NGO can only edit its own supply point's inventory; a volunteer can only complete a task they accepted).

## Transport and headers
- `Strict-Transport-Security` once behind HTTPS (set by the load balancer/ingress in Step 11)
- `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`
- `Content-Security-Policy: default-src 'none'` — this is a JSON API with no HTML/JS of its own to allow
- CORS restricted to the exact origins in `CORS_ORIGINS`, explicit methods and headers only

## Rate limiting
- `/auth/login`, `/auth/register`, `/auth/refresh` limited to 10 requests/minute per client IP (Bucket4j, in-memory)
- In-memory is per-instance; once running multiple replicas in Kubernetes (Step 9), move the bucket store to Redis so the limit is shared across pods

## Input validation
- Bean Validation (`jakarta.validation`) on every request DTO: bounded string lengths, numeric ranges, required fields
- Errors returned as RFC 9457 `ProblemDetail`, with field-level messages for validation failures — never a raw stack trace

## Secrets
- Nothing sensitive is hardcoded. `JWT_SECRET`, database credentials, and the bootstrap admin password all come from environment variables (`.env.example` shows what's needed)
- In Kubernetes (Step 9) these move to `Secret` objects; in AWS (Step 11) to Secrets Manager or SSM Parameter Store, injected at deploy time — never committed to git

## Dependency scanning
- OWASP Dependency-Check Maven plugin: `mvn org.owasp:dependency-check-maven:check`, fails the build on a known CVE with CVSS ≥ 8. Wired into CI in Step 7.
- Dependabot enabled for Maven, npm, pip, Docker base images, and GitHub Actions — see `.github/dependabot.yml`
- GitHub secret scanning and push protection enabled at the repo level (see `docs/project-structure.md` §3.8)

## Still to add later
- Account lockout / CAPTCHA after repeated failed logins (beyond the IP rate limit)
- Centralized secret rotation policy once in AWS
- WAF rules at the ingress (Step 11)
