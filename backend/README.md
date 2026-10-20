# StudyHive backend

Spring Boot 4 (Java 21, Hibernate 7, Spring Security 7) REST API. See
[`../docs/architecture.md`](../docs/architecture.md) and
[`../docs/api-specification.md`](../docs/api-specification.md) for design and
the endpoint list.

## Run locally (Windows)

1. From the repo root: `copy .env.example .env` and fill in `DB_PASSWORD`
   and `JWT_SECRET` (32+ random bytes; `openssl rand -base64 48` works).
2. Make sure MySQL's `studyhive` database has the schema applied (see
   [`../database/README.md`](../database/README.md)).
3. From the repo root: `scripts\dev-backend.bat` (loads `.env`, runs
   `mvn spring-boot:run`). API listens on `http://localhost:8080/api/v1`.

## Tests

```bash
mvn test
```

Runs against an in-memory H2 database (`src/test/resources/application-test.yml`,
profile `test`) -- no MySQL needed. Covers:

- `MaterialServiceTest` -- the PRIVATE/COMMUNITY access-control checks that
  gate every material read, and the Personal Vault / subject-community
  validation rules.
- `CommunityServiceTest` -- the owner-gets-an-OWNER-membership-row
  invariant, the private-community password gate, and the owner-can't-leave
  rule.
- `MaterialAccessControlIntegrationTest` -- a real `MockMvc` run through
  register → create community → upload → access-control, confirming the
  whole chain (security filter, controllers, services, repositories) is
  wired correctly.

## Verified against the real database

Beyond the automated tests above, the full API was manually exercised
against the actual local MySQL `studyhive` database (not just H2) on
2026-10-19/20: register, login, create/join public and
password-protected private communities, create subjects, upload a
material (multipart), search/filter, download, add a new version
(confirmed the old version is retained), favorite/unfavorite, record a
view, upload and filter a question paper, submit and resolve a report,
and confirm a non-member gets 403 on both a community material and a
Personal Vault material. See `../docs/development-guide.md` for the dated
status log.

## Notable MySQL/JDBC quirks hit during development

- **Spring Boot 4 uses Jackson 3** (`tools.jackson.*`), not classic
  `com.fasterxml.jackson`, by default. Code that needs `ObjectMapper`
  directly (the JSON auth entry point / access-denied handler) depends on
  `tools.jackson.core:jackson-databind` explicitly.
- **`-parameters` compiler flag is required** (set via
  `maven.compiler.parameters` in the root `pom.xml`) because Spring Boot 4
  no longer sets it for you outside `spring-boot-starter-parent`, and
  without it Spring can't disambiguate same-type beans (e.g. our
  `CorsConfigurationSource` vs. the autoconfigured
  `HandlerMappingIntrospector`, which also implements that interface).
- **MySQL `YEAR` and `TINYINT UNSIGNED` columns** (`question_papers.exam_year`,
  `.semester`) don't map to `Integer` the way you'd expect. `TINYINT` maps
  to `Byte`. `YEAR` needs the JDBC URL flag `yearIsDateType=false` *and*
  a `Short` field -- otherwise Hibernate's schema validation fails (if
  mapped as `Integer`/`Date`) or the insert fails under strict SQL mode
  with "Data truncated for column" (if mapped as `java.sql.Date`, which
  is what Connector/J reports by default).
- **MockMvc moved to its own starter** in Boot 4:
  `spring-boot-starter-webmvc-test`, with `@AutoConfigureMockMvc` now in
  `org.springframework.boot.webmvc.test.autoconfigure`.
