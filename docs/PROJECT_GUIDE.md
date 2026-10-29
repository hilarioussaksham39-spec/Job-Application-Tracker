# Job Tracker: Code Walkthrough and Interview Guide

## 1. What you built

A full-stack job application tracker with a Java/Spring Boot backend, a relational database, and an HTML/CSS/JavaScript frontend. Users register and sign in, then create, list, change the status of, or delete their own applications. It is deployed using Docker on Render with Aiven MySQL and can be installed as a PWA on compatible browsers.

Do not describe it as a native Android app, an offline-first tracker, a JWT application, or a microservices system. Those are not its implementation.

## 2. File-by-file reading order

| File | Responsibility |
|---|---|
| JobtrackerApplication.java | Starts Spring Boot and component scanning |
| JobApplication.java | Immutable response/data record: id, company, role, status |
| JobApplicationController.java | HTTP mappings, request validation, authenticated user identity |
| JobApplicationRepository.java | Parameterized SQL and database-to-record mapping |
| AuthController.java | Receives registration JSON |
| RegistrationService.java | Username/password rules, hashing, duplicate-account handling |
| UserRepository.java | Inserts users |
| PasswordConfig.java | Supplies the shared PasswordEncoder bean |
| SecurityConfig.java | User lookup, public/protected paths, login/logout, API authentication errors |
| CsrfController.java | Supplies a CSRF token |
| security.js | Adds CSRF headers for writes and handles HTTP 401 |
| index.html | Dashboard markup, styling, browser state, filtering, Fetch requests |
| login.html | Standard form submission to Spring Security |
| register.html | JSON registration request |
| schema.sql | Initial table definitions; not a migration system |
| manifest.json / pwa.js / sw.js | Installation metadata, worker registration, connection-failure page |
| Dockerfile | Builds the JAR and runs it with a Java runtime |

## 3. Follow one create request

1. The user submits company and role in index.html.
2. The handler prevents normal form navigation, validates nonblank fields, and creates JSON.
3. apiFetch obtains a CSRF token, adds its named header, and sends POST /api/applications.
4. The browser includes its same-origin session cookie automatically.
5. Spring Security checks the session and CSRF token before the controller runs.
6. Spring converts JSON into CreateApplicationRequest.
7. The controller validates field lengths and obtains Principal.getName().
8. The repository looks up that username's database ID.
9. save inserts company, role, APPLIED, and the owner ID using parameters.
10. GeneratedKeyHolder retrieves the AUTO_INCREMENT ID.
11. The returned JobApplication becomes JSON with HTTP 201.
12. JavaScript adds the returned object to its local list and redraws the cards and counts.

Explain why each step exists, not just its method name.

## 4. Registration

POST /api/auth/register calls RegistrationService.register.

The username must match 3–50 ASCII letters, digits, or underscores. Password validation checks at least 12 Java String code units and at most 72 UTF-8 bytes. The latter protects against bcrypt's byte limit; characters and bytes are not equivalent for Unicode.

PasswordConfig creates a DelegatingPasswordEncoder. The current encoder generates bcrypt output with a {bcrypt} prefix. Salt information is included in the encoded representation. Re-encoding the same password normally produces a different string.

UserRepository inserts username and password_hash. A unique database constraint prevents duplicate usernames, including concurrent registration attempts. DuplicateKeyException becomes HTTP 409.

The raw password is used transiently for registration; it is not stored in the table. Password hashing does not replace HTTPS.

## 5. Login and sessions

The form posts username/password fields and a CSRF hidden input to /login. This is form data, unlike the registration endpoint's JSON.

Spring Security processes login through its filters. UserDetailsService loads the username and stored hash from SQL. The authentication provider uses the configured encoder to verify the supplied password. On success, the authentication context is associated with the server-side session and the browser is redirected to /.

The cookie identifies the session; it does not contain the user's raw password. Server-side sessions may be lost when the single deployed server restarts, requiring login again. User and application database records remain.

Logout is a CSRF-protected POST. Spring Security invalidates the session and redirects to the login page.

Authentication answers who the user is. Authorization determines which actions and records that user can access.

## 6. Ownership: the most important SQL condition

UPDATE applications SET status = ? WHERE id = ? AND user_id = ?

Both conditions must match. An application ID alone is insufficient.

The controller derives the owner from Principal, then looks up its ID. It does not accept user_id from the browser. Lists filter by owner; inserts attach the owner; updates/deletes require owner and record ID.

The repository returns whether rows were affected. For a missing or inaccessible record the controller returns 404. This also avoids explicitly confirming whether another user's record exists.

Manual cross-account tests reported by the developer returned 404 for PATCH and DELETE. A 404 alone is not conclusive: the targeted record must be known to exist and remain unchanged.

## 7. Spring and Java concepts

- Bean: an object Spring creates/manages.
- Constructor injection: Spring supplies required collaborators through a constructor.
- @RestController: request handling whose return values are written to the response body.
- @Service: application/business logic component.
- @Repository: data access component.
- @Configuration and @Bean: configuration that creates managed objects.
- Record: a compact Java data carrier with generated accessors and value semantics; not a database entity.
- Serialization: converting returned Java data into JSON.
- JdbcTemplate: assists SQL execution, connections/resource handling, and result mapping; it does not remove the need to write SQL.
- Row mapper lambda: constructs one JobApplication for each ResultSet row.
- Prepared statement: binds values separately from SQL syntax.

Current CRUD controllers call the repository directly. Registration uses an intervening service. Do not claim every feature has a service layer.

## 8. CSRF, SQL injection, and XSS

These are different problems.

CSRF: a malicious origin causes a browser to submit unwanted authenticated requests. The app keeps Spring Security CSRF protection and sends a token with write requests. Session cookies alone are not enough.

SQL injection: input changes SQL meaning. The repository uses bound ? parameters rather than concatenating usernames, company names, or IDs into SQL.

XSS: untrusted content becomes executable markup/script. The dashboard creates elements and assigns company/role through textContent rather than inserting user data with innerHTML. This is a useful defense in those rendering paths, not a complete security audit.

HttpOnly prevents JavaScript from reading the session cookie. Secure restricts cookie transmission to HTTPS. Neither property replaces CSRF protection.

## 9. Deployment and PWA

Docker's first stage compiles/package-builds with a JDK. The second runs the JAR with a JRE as a non-root user. -DskipTests skips execution; it does not prove tests pass.

Render hosts Spring Boot and serves frontend/API from one origin, so this design needs no separate cross-origin frontend configuration. Aiven stores relational data independently of the Render instance.

The verified TLS JDBC connection checks certificate trust and hostname. Cloud credentials come from environment variables. Localhost in the hosted server refers to that server, not the developer's XAMPP.

The PWA manifest describes name/icons/start URL and standalone display. The worker catches navigation network failures and returns an offline message. It does not cache private API data, synchronize edits, or provide notifications. Installation does not move Java or MySQL onto the phone.

## 10. HTTP responses to explain

| Code | Example |
|---|---|
| 200 | Read application list |
| 201 | Account or application created |
| 204 | Successful status update or deletion |
| 400 | Invalid submitted values |
| 401 | API request without authenticated session |
| 403 | Access denied, including invalid/missing CSRF in relevant requests |
| 404 | Application missing or not owned by current user |
| 409 | Duplicate registration username |

## 11. Interview introduction

“I built a job application tracker using Java, Spring Boot, Spring Security, JDBC, and a MySQL-compatible database. Users register and log in, then manage their own applications through a responsive JavaScript dashboard. I implemented status updates, search, filters, and summary counts.

The backend derives record ownership from the authenticated session and checks both the application ID and owner ID in update and delete queries. Passwords are encoded with bcrypt, and write requests use CSRF protection.

I deployed the app with Docker on Render and used Aiven MySQL for persistent storage. I also added a PWA manifest and service worker so it can be installed on Android, while keeping data operations online.”

Adapt this to your own words. Be transparent about using AI guidance if asked; explain what you implemented, tested, understood, and would improve.

## 12. Questions to practice aloud

1. Why does refreshing keep data but restarting the old ArrayList version lose it?
2. What happens from clicking Add to the record appearing in SQL?
3. Where does the owner ID come from, and why not accept it in JSON?
4. How does bcrypt verification work when salts produce different hashes?
5. What differs between hashing, encryption, and HTTPS?
6. Why keep a unique username constraint after validating input?
7. Why is CSRF relevant to cookie-based authentication?
8. Which parts use JSON and which use regular form data?
9. Why use JdbcTemplate rather than JPA here?
10. What does dependency injection change about ordinary Java constructors?
11. What does the service worker actually do offline?
12. Why are environment variables needed on Render as well as your PC?
13. Which checks were manual, and which tests are automated?
14. What happens to login sessions when Render restarts?
15. What would you improve before handling more users?

A strong answer includes a concrete method, SQL condition, or observed behavior.

## 13. Honest limitations and next improvements

- Search/filtering currently load all owned applications into browser memory; add server-side pagination for larger datasets.
- Browser refresh and concurrent mutations can race; add request sequencing or reconciliation if expanding the UI.
- Add automated two-account authorization tests and registration validation tests.
- Add rate limiting and password recovery before broader public use.
- Use schema migrations rather than CREATE TABLE IF NOT EXISTS for future table changes.
- A deleted user with an existing session currently needs better handling in findUserId.
- The username column uses database collation rules; do not assume case-sensitive usernames.
- Context test package capitalization was found to differ from the main package. Correct it to com.saksham.jobtracker before relying on test discovery.
- The generated context test is not evidence of full functionality; the Docker build skips its execution.

## 14. Resume and portfolio wording

Project title: Job Application Tracker | Java, Spring Boot, Spring Security, JDBC, MySQL, Docker

Resume bullets:
- Built and deployed a full-stack job application tracker with session-based authentication, bcrypt password encoding, and SQL ownership checks for user-specific CRUD operations.
- Created a responsive, installable PWA with company/role search, status filters, and summary counts; deployed the Java backend on Render with Aiven MySQL over verified TLS.

Portfolio description:
“A responsive job application tracker with personal accounts, status tracking, search, and dashboard counts. Built with Spring Boot, Spring Security, JDBC, and vanilla JavaScript, deployed with Docker, and installable as a PWA.”

Live demo: https://job-application-tracker-kwrn.onrender.com
Source: https://github.com/hilarioussaksham39-spec/Job-Application-Tracker

Do not claim performance improvements, user counts, automated test coverage, or native Android development that were not measured or implemented.

## 15. Seven focused practice sessions

1. Trace GET and POST through the controller and repository. Recreate a small GET endpoint.
2. Explain records, JSON, lambdas, and constructor injection. Write one example of each.
3. Rebuild registration in a separate practice branch with the encoder.
4. Trace login, sessions, CSRF, and logout without copying explanations.
5. Repeat two-account ownership tests for list/update/delete and explain the SQL.
6. Explain the Dockerfile, cloud settings, and PWA lifecycle.
7. Give the project introduction and answer the fifteen questions aloud.

## 16. Add real screenshots

Use disposable account data. Capture the dashboard at desktop and phone widths and optionally the login page. Do not include passwords, database connection screens, personal applications, or browser account information.

Suggested repository paths: docs/screenshots/dashboard.png and docs/screenshots/mobile.png. Add Markdown image links to README.md only after those actual files exist.

