import akka.actor.{Actor, Props, ActorLogging, ActorRef}
import scalafx.collections.{ObservableHashSet, ObservableSet}

object Server {
  case class Join(username: String)
}

class Server extends Actor with ActorLogging {
  import Client._
  import Server._

  var clients: ObservableSet[ActorRef] = new ObservableHashSet()

  def receive = {
    case Join(name) =>
      log.info(s"$name joined the chat room.")
      sender() ! Joined
    case _ => log.info("Unknown message received.")
  }
}
