# ProX4 – Secure Access Proxy System

## Description

A Java CLI application that simulates a secure file access system using design patterns to separate security logic from business logic. The system uses a Proxy layer to intercept file access requests, enforce role-based permissions, limit view counts, and notify observers of every security event.

**Team:** Nawaf Baryan (2338019) · Abdulrhman Nasiri (2337601)  
**Course:** CPIT-252 · King Abdulaziz University, FCIT

### Design Patterns Used
- **Singleton** (Creational) — `AuthenticationManager` ensures a single shared user database.
- **Proxy** (Structural) — `SecureFileProxy` controls access to the real file resource.
- **Observer** (Behavioral) — `SecureFileProxy` notifies subscribed observers of every security event.


## Features
- Role-Based Access Control with three roles (MANAGER, USER, GUEST) and three file types (SENSITIVE, INTERNAL, NORMAL).
- View limit enforcement: each user is restricted to 3 views per file.
- Security event logging to `access_log.txt` with timestamp, user, role, and file name.
- Real-time console security alerts for unauthorized access attempts and view-limit violations.
- Business hours validation through `TimeAccessChecker` (07:00 – 23:00).
- Actual file content reading and display within the CLI (no external viewer required).
- Case-insensitive username lookup and full input validation against crashes.
- Guest fallback for unknown usernames.


## Usage

To build and run the app, use:

```shell
mvn clean package
java -jar target/project-prox4-3.jar
```

### Default Users

| Username | Role |
|---|---|
| Nawaf | MANAGER |
| Khaled | MANAGER |
| Abdulrahman | MANAGER |
| Faisal | USER |
| *(any other name)* | GUEST |

### Releases

- **v3.0** — Behavioral Stage: Observer pattern, security logging, alert observer, time-limited access, full codebase audit.
- **v2.0** — Structural Stage: Proxy pattern, role-based access control, view limit enforcement.
- **v1.0** — Creational Stage: Singleton AuthenticationManager, basic CLI menu.


## Screenshots

*(Screenshots will be added before the final demo.)*


## License

MIT License