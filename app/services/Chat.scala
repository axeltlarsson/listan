package services

import akka.actor._

// our domain message protocol
case object Join
case object Leave
final case class ClientSentMessage(text: String)

// Chat actor
class Chat extends Actor {
  // Initial message-handling behaviour
  def receive = process(Set.empty)

  def process(subscribers: Set[ActorRef]): Receive = {
    case Join =>
      // replaces message-handling behaviour by the new one
      context become process(subscribers + sender)
    case Leave =>
      context become process(subscribers - sender)
    case msg: ClientSentMessage =>
      // send messages to all subscribers except sender
      (subscribers - sender).foreach { _ ! msg }
  }
}