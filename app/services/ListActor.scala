package services

import akka.actor._
import javax.inject._

object ListActor {
  // Messages should be defined here maybe?
}

@Singleton
class ListActor @Inject() () extends Actor {
  import ListActor._

  println("Starting ListActor")

  def receive = {
    case _ => println("ListActor got a message")
  }
}
