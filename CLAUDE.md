# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

### Backend (Scala/Play)
- `sbt test` - Run all backend tests
- `sbt run` - Run in development mode (port 9000)
- `sbt runProd` - Run in production mode (requires `DB_PASSWORD` and `CRYPTO_SECRET` env vars)

#### Scala Console (via Docker)
```bash
docker compose exec app sbt -Dconfig.file=conf/application.dev.conf console
```

Then in `:paste` mode:
```scala
import play.api._
import play.api.inject.guice._

val app = new GuiceApplicationBuilder().in(Mode.Dev).build()
Play.start(app)
val injector = app.injector

// Access services
val userService = injector.instanceOf[services.UserService]

// Create a user
import scala.concurrent.Await
import scala.concurrent.duration._
val uuid = Await.result(userService.add("alice", "password"), 10.seconds)
```

**Note:** Ensure evolutions have run first by hitting any endpoint (e.g., `curl http://localhost:9000/api/login`).

### Frontend (React/Redux)
- `cd frontend && npm test` - Run frontend tests
- `cd frontend && npm run build:dev` - Development build
- `cd frontend && npm run build:prod` - Production build
- `cd frontend && npm start` - Start webpack dev server

### Docker
- `docker compose up --build` - Run full stack locally (uses `Dockerfile.dev`)
- `./scripts/seed-dev-user.sh` - Create alice/password user with default list (run after stack is up)
- `docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d` - Production deployment
- `docker compose -f docker-compose.yml -f docker-compose.prod.yml run --rm -i prod-console console` - Production console

## Architecture

### Overview
A real-time list application with Scala Play backend and React/Redux frontend. Communication happens via WebSocket with JWT authentication.

### Backend Structure (Scala Play + Akka)
- **Routes**: Three endpoints only: `POST /api/login`, `GET /api/ws` (WebSocket), `POST /api/lists/batch`
- **Actor System**:
  - `ListActor` (singleton) - Central hub that manages all connected clients and broadcasts actions
  - `WebSocketActor` - FSM-based actor per WebSocket connection, handles authentication state and message relay
- **Services**: `UserService`, `ItemService`, `ItemListService` - business logic layer
- **Repositories**: Slick-based data access (`SlickUserRepository`, `SlickItemRepository`, `SlickItemListRepository`)
- **DI**: Guice bindings in `app/Module.scala`

### WebSocket Protocol
Custom JSON protocol defined in `app/services/Message.scala`:
- Client sends `Action` messages (AddItem, EditItem, DeleteItem, CompleteItem, GetState, AddList, etc.)
- Server responds with `Response` messages and relays actions to other clients of the same user
- All messages include an `ack` field for acknowledgment tracking
- Messages use snake_case JSON format via `julienrf.json.derived`

### Frontend Structure (React/Redux)
- Redux store with thunks for async WebSocket communication
- Located in `frontend/src/` with standard Redux structure (actions, reducers, components, containers)
- Served via nginx reverse proxy that forwards `/api/*` to backend

### Database
- PostgreSQL with Slick ORM
- Evolutions in `conf/evolutions/default/`
- Test config uses H2 in-memory (`conf/application.test.conf`)
