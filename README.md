Listan-server
=============
This is the backend to [listan](https://github.com/AxelTLarsson/listan).
It is build on top of Scala Play framework, however it relies heavily on akka
for the actor system.

The whole stack is more or less fully asynchronous and mostly functional. Slick
is used as the database layer. Guice is used for dependency injection. This makes
it easy to write good tests and mock out specific modules. Akka provides rather good
testing tools that is invaluable when testing the actor system.

Running
-------
* `sbt test` for running the tests
* `sbt run` for running in dev mode
* `sbt testProd` for running the app locally in production mode
(application.prod.conf will be used as configuration file)


Architecture
------------
The backend consists of two routes: /api/login and /api/ws. The /api/login route
accepts a `POST` with keys `username` and `password`. If the correct credentials
are given a `JSON` response with a header `Authorization: Bearer <token>`
and a payload also containing the JWT token is returned.

The route /api/ws accepts WebSocket connections and requires that the client
provides a valid JWT token (as required from /api/login). Each WebSocket
connection is maintained by an akka actor.

WebSocket Protocol
------------------
A custom protocol is used. It is based on the idea of the client sending `Action`s
and the server responding with `Response`s and relaying the `Action`s to other
clients. More details can be found in [`Message.scala`](./app/services/Message.scala).

Deploying
---------
`DB_PASSWORD` and `CRYPTO_SECRET` environment variables are required.
`sbt dist` and a zip file will be produced under `target/universal` whose `bin` dir
will contain a runnable executable (really just a shell script). That can be run
from the command line, but do not forget to pass in the path to the production
configuration like so:

`./bin/listan-server-1.0.0 -Dconfig.file=../conf/application.prod.conf`

