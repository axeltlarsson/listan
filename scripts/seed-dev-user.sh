#!/bin/bash
# Seeds a default dev user for local development
# Usage: ./scripts/seed-dev-user.sh

set -e

echo "Waiting for app to be ready..."
until curl -s http://localhost:9000/api/login > /dev/null 2>&1; do
  sleep 2
done

echo "Creating dev user..."
docker compose exec -T app sbt -Dconfig.file=conf/application.dev.conf console <<'EOF'
import play.api._
import play.api.inject.guice._
import scala.concurrent.Await
import scala.concurrent.duration._

val app = new GuiceApplicationBuilder().in(Mode.Dev).build()
Play.start(app)
val userService = app.injector.instanceOf[services.UserService]

// Check if user exists first
val existing = Await.result(userService.findByName("alice"), 10.seconds)
if (existing.isEmpty) {
  val uuid = Await.result(userService.add("alice", "password"), 10.seconds)
  println(s"Created alice user with UUID: $uuid")

  // Create a default list for the user
  val listService = app.injector.instanceOf[services.ItemListService]
  val listUuid = Await.result(listService.add("My List", None, uuid), 10.seconds)
  println(s"Created default list with UUID: $listUuid")
} else {
  println("alice user already exists")
}

System.exit(0)
EOF

echo "Done! Login with alice/password"
