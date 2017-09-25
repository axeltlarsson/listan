[![CircleCI](https://circleci.com/gh/AxelTLarsson/listan-server/tree/multiple-lists.svg?style=svg)](https://circleci.com/gh/AxelTLarsson/listan-server/tree/multiple-lists)
# Listan-server
This is the backend to [listan](https://github.com/AxelTLarsson/listan).
It is built on top of Scala Play framework, however it relies heavily on akka
for the actor system.

The whole stack is more or less fully asynchronous and mostly functional. Slick
is used as the database layer. Guice is used for dependency injection. This makes
it easy to write good tests and mock out specific modules. Akka provides rather good
testing tools that is invaluable when testing the actor system.

## Running
* `sbt test` for running the tests
* `sbt run` for running in dev mode (default dev user auth is `axel:whatever`)
* `sbt runProd` for running the app locally in production mode
(application.prod.conf will be used as configuration file), however do not forget
to export the required ENV variables `DB_PASSWORD` and `CRYPTO_SECRET`.
### Docker
`sbt dist`

`docker-compose up` which will also spin up a MySQL database.


## Architecture
The backend consists of two routes: /api/login and /api/ws. The /api/login route
accepts a `POST` with keys `username` and `password`. If the correct credentials
are given a `JSON` response with a payload containing the JWT token is returned.

The route /api/ws accepts WebSocket connections and requires that the client
provides a valid JWT token (as required from /api/login). Each WebSocket
connection is maintained by an akka actor.

### WebSocket Protocol
A custom protocol is used. It is based on the idea of the client sending `Action`s
and the server responding with `Response`s and relaying the `Action`s to other
clients. More details can be found in [`Message.scala`](./app/services/Message.scala).

## Installing
Releases for debian are created with `sbt debian:packageBin`. The resulting .deb file
is found in `target/listan-server_x.x.x`. These .deb files are also published under
"Releases" on GitHub.

The only dependency required is Java 8 or later, but unfortunately it is not trivial to
encode this requirement in the .deb in a satisfactory manner; it it was you would not have
needed to install Java manually.

Use `dpkg -i listan-server_x.x.x.deb` to install. Currently an upstart script is used and
you need to somehow provide the required environment variables to the process. In theory,
this could be done in `/etc/default/listan-server`, unfortunately though that does not work
for some reason. So the easiest solution is to edit the generated upstart configuration file
in `/etc/init/listan-server`.

### Docker
`sbt dist`

Make `DB_PASSWORD` and `CRYPTO_SECRET` available to the shell before continuing:

Then: `docker-compose -f docker-compose.yml -f docker-compose.prod.yml up`

