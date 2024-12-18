# Listan

It is built on top of Scala Play framework, however it relies heavily on akka for the actor system.

The whole stack is more or less fully asynchronous and mostly functional. Slick is used as the database layer. Guice is
used for dependency injection. This makes it easy to write good tests and mock out specific modules. Akka provides
rather good testing tools that is invaluable when testing the actor system.

## Setup

`docker compose up --build` then add a user via the console (see below).

## Running

`docker compose up --build` will use `Dockerfile.dev` for local dev using docker.

- `sbt test` for running the tests
- `sbt run` for running in dev mode (remember to add a user first, see below)
- `sbt runProd` for running the app locally in production mode, however do not forget to export the required ENV
  variables `DB_PASSWORD` and `CRYPTO_SECRET`.

### Adding a User Via the Console

`sbt console` then enter `:paste` to get to paste-mode and:

``` scala
// injector from Guice to gain access to services/repos
import play.api.inject.guice.GuiceApplicationBuilder
val injector = (new GuiceApplicationBuilder()).injector
```

To e.g. create a user and insert into the database:

``` scala
import services.UserService
val userService = injector.instanceOf[UserService]
val uuidFuture = userService.add("alice", "password")
val uuid = uuidFuture.value.get

val userFuture = userService.findByName("alice")
```

To create a list for the user:

``` scala
import services.ItemListService
val listService = injector.instanceOf[ItemListService]
val listUuidFuture = listService.add("my list", None, uuid.get)
```

### Docker deploy

- Substitute appropriate values into `.env` - see [sample.env](./sample.env) for an example
- Run `docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d`

For updating only frontend:

- `docker-compose -f docker-compose.yml -f docker-compose.prod.yml up --no-deps --build -d frontend`

#### Production Console

    docker compose -f docker-compose.yml -f docker-compose.prod.yml run --rm -i prod-console console

Then run `console`.

## Architecture

The backend consists of three routes: /api/login, /api/ws and /api/lists/batch. The /api/login route accepts a `POST`
with keys `username` and `password`. If the correct credentials are given a `JSON` response with a payload containing
the JWT token is returned.

The route /api/ws accepts WebSocket connections and requires that the client provides a valid JWT token (as required
from /api/login). Each WebSocket connection is maintained by an akka actor.

### WebSocket Protocol

A custom protocol is used. It is based on the idea of the client sending `Action`s and the server responding with
`Response`s and relaying the `Action`s to other clients. More details can be found in
[`Message.scala`](./app/services/Message.scala).

## Integration with recipe db

Endpoint `/api/lists/batch` is used to batch add items to the user's primary list.

``` shell
curl -X POST http://localhost:9000/api/lists/batch \
  -H "Authorization: Bearer $(uv run generate_jwt.py)" \
  -H "content-type: application/json" \
  -d '[ "mjölk", "mjöl", "socker"]' -i
```

- use [generate_jwt.py](./generate_jwt.py) to generate a JWT - make sure the `sub` claim points to a valid username in
  the database
- curl `POST /api/lists/batch` to batch add ingredients like the recipe db does
- the listan service will then batch add the ingredients to the first list owned by the user defined in the `sub` claim
  of the JWT
