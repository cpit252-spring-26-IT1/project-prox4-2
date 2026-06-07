# ProX4 â€“ Secure Access Proxy System

## Description

A Java CLI application that simulates a secure file-access system built around classic
design patterns to keep security logic cleanly separated from business logic. A **Proxy**
layer intercepts every file request to enforce role-based permissions, lock checks, view
limits and business-hours rules, while an **Observer** layer reacts to every security event
(console alerts + persistent audit log). User accounts are persisted in an **encrypted**
local database with hashed passwords and brute-force lockout.

**Team:** Nawaf Baryan (2338019) آ· Abdulrhman Nasiri (2337601)
**Course:** CPIT-252 آ· King Abdulaziz University, FCIT

### Design Patterns Used
- **Singleton** (Creational) â€” `AuthenticationManager`, `FileRegistry` and `PromotionManager`
  each expose a single shared instance.
- **Proxy** (Structural) â€” `SecureFileProxy` guards the real `RealFileAccess` resource;
  `DownloadProxy` guards file downloads.
- **Observer** (Behavioral) â€” `SecureFileProxy` notifies `SecurityLogger` and `AlertObserver`
  of every `AccessEvent`.

## Features
- **Role-Based Access Control** with four roles (`OWNER`, `MANAGER`, `USER`, `GUEST`) and
  three file sensitivity levels (`SENSITIVE`, `INTERNAL`, `NORMAL`).
- **Secure accounts** â€” passwords hashed with SHA-256, account auto-lockout after 3 failed
  attempts, and an AES-encrypted user database (`users.dat`).
- **First-run setup** â€” on first launch you create the `OWNER` account; there are no
  hard-coded credentials.
- **View-limit enforcement** â€” each non-owner is limited to 3 views per file.
- **File operations** â€” register, browse, open (with confidential watermark), move to a
  workspace, delete, and lock/unlock files.
- **User management** â€” owners add / remove / promote / demote users; managers may add users.
- **Promotion workflow** â€” users and guests can request a promotion; owners approve or reject.
- **Owner dashboard** â€” at-a-glance pending requests, locked files, registered users and
  logged security violations.
- **Auditing** â€” every event is written to `access_log.txt`; a built-in colored audit-log
  viewer lets owners review or clear it.
- **Real-time security alerts** for unauthorized access attempts and view-limit violations.

## Usage

### Build & run (Maven)
```shell
mvn clean package
java -jar target/course-project.jar
```

> On Windows, run with UTF-8 so the boxes/colors render correctly:
> `java -Dstdout.encoding=UTF-8 -jar target/course-project.jar`

### Run with Docker
```shell
docker build -t prox4:latest .
docker run -it --rm prox4:latest
```
To persist accounts and logs between runs, mount a volume:
```shell
docker run -it --rm -v "${PWD}/data:/app" prox4:latest
```

### First run & roles
On first launch the app detects an empty database and asks you to create the **OWNER**
account. After that, log in (or continue as a **Guest**). Capabilities by role:

| Role | Can see | Highlights |
|---|---|---|
| **OWNER** | all files | full control; bypasses lock/view-limit; user management; dashboard; audit log |
| **MANAGER** | SENSITIVE, INTERNAL, NORMAL | open/move/lock; delete NORMAL; add USER accounts |
| **USER** | INTERNAL, NORMAL | open/move; request promotion |
| **GUEST** | NORMAL only | browse; request promotion |

## Testing & Coverage

Unit + integration tests run with JUnit 5; coverage is measured by JaCoCo. The single
`AppTest` suite combines pure unit tests with full integration runs of `App.main` (driven
through a redirected `System.in`) so that even the interactive menu flows are exercised.

```shell
mvn test
```
- **49 tests**, all passing.
- **96.5% line coverage** (911 / 944 lines).
- **90% branch coverage** (403 / 447 branches).
- HTML report generated at `target/site/jacoco/index.html`.

The handful of uncovered lines are unreachable defensive `catch` blocks (e.g. SHA-256
unavailable, AES failure with a valid key, I/O write failures) and guarded dead branches.

## Project Highlights / What's Included
- âœ… Clean separation of concerns via **Singleton + Proxy + Observer** patterns.
- âœ… **Encrypted, persistent** account store with hashed passwords and lockout.
- âœ… **96.5%** test line coverage with JUnit 5 + JaCoCo (`AppTest`).
- âœ… **Dockerized** with a multi-stage build (`Dockerfile` + `.dockerignore`).
- âœ… Executable JAR with a configured `Main-Class` manifest (`maven-jar-plugin`).
- âœ… UTF-8 / ANSI-colored console UI (banner, menus, dashboard, audit viewer).

## Screenshots

**1. First-run setup â€” creating the OWNER account**
![First-run setup](docs/screenshots/01-first-run-setup.png)

**2. Owner login & dashboard**
![Owner dashboard](docs/screenshots/02-owner-dashboard.png)

**3. Registering and browsing files**
![Register & browse](docs/screenshots/03-register-browse.png)

**4. Secure file view (Proxy granted + watermark)**
![Open file](docs/screenshots/04-open-file.png)

**5. Access denied & security alert (USER on a SENSITIVE file)**
![Access denied](docs/screenshots/05-access-denied.png)

## Releases

- **v4.0** â€” Security Stage: encrypted persistent accounts, SHA-256 password hashing,
  account lockout, OWNER role + dashboard, file lock/unlock, promotion-request workflow,
  audit-log viewer, download proxy, Docker support, ~95% test coverage.
- **v3.0** â€” Behavioral Stage: Observer pattern, security logging, alert observer,
  time-limited access, full codebase audit.
- **v2.0** â€” Structural Stage: Proxy pattern, role-based access control, view-limit enforcement.
- **v1.0** â€” Creational Stage: Singleton `AuthenticationManager`, basic CLI menu.

## License

MIT License
