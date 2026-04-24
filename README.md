# Git Commit Convention

## Format

```
type(scope) : subject
```

- **No period** at the end
- **English only**

---

## Types

| type | description |
|---|---|
| `feat` | new feature |
| `fix` | bug fix |
| `docs` | documentation |
| `style` | formatting |
| `refactor` | refactoring (no feature change, performance improvement) |
| `test` | test code |
| `chore` | maintenance & misc |

---

## Subject Rules

- imperative mood (present tense)
- all lowercase
- no period at the end

---

## Examples

```
feat(api) : add order delay prediction api
fix(auth) : fix login button error
docs(db) : update warehouse schema docs
style(ui) : format dashboard layout
refactor(db) : improve warehouse query
test(api) : add robot summary endpoint test
chore : update dependencies
```

---

# Docker Compose

## Local Run

```bash
docker compose up --build
```

- Frontend: http://localhost:5173
- Backend: http://localhost:8080
- MySQL from host: localhost:3307
- MySQL from containers: db:3306

The frontend container serves the Vue build through Nginx. Browser requests to `/api/**` are proxied to the Spring Boot `backend` service, so `VITE_API_BASE_URL` should stay empty for the default Compose deployment.

## Environment

`docker compose` reads the root `.env` file automatically. For deployment, copy `.env.example` to `.env` and change at least:

```bash
MYSQL_PASSWORD=...
MYSQL_ROOT_PASSWORD=...
FRONTEND_PORT=80
```

The database schema currently uses `warehouse_db`, so keep `MYSQL_DATABASE=warehouse_db` unless you also update the SQL files.

## Reset DB

MySQL init scripts run only when the volume is first created. To rebuild the database from `DB/schema.sql` and `DB/view.sql`:

```bash
docker compose down -v
docker compose up --build
```
