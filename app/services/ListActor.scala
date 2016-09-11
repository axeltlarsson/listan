package services

import akka.actor._
import javax.inject._
import play.Logger
import scala.collection.mutable

object ListActor {
  case object Subscribe // asking ListActor to add sender to list of clients
}

@Singleton
class ListActor @Inject() () extends Actor {
  import ListActor._

  val clients: mutable.Set[ActorRef] = mutable.Set[ActorRef]()

  println("Starting ListActor")

  def receive = {
    case Subscribe => {
      Logger.debug("Adding client to set")
      clients += sender
      clients.foreach(client => {
        println("Sending msg to client")
        client ! "HI CLIENT YOU HAVE BEEN SUCCESSFULLY ADDED"
      })
    }
    case _ => println("ListActor got a message")
  }
}
