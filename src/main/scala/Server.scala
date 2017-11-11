import akka.actor.{Actor, Props, ActorLogging, ActorRef}
import scalafx.collections.{ObservableHashMap, ObservableMap, ObservableSet, ObservableHashSet}
import scala.collection.mutable.{Map, HashMap}


object Server {
  case class Join(username: String)
  case class ReceivedJoined(username: String)
}

class Server extends Actor with ActorLogging {
  import Client._
  import Server._
  import Display._

  var clientNamePairs: Map[String,String] = new HashMap()
  var clients: ObservableSet[ActorRef] = new ObservableHashSet() 

  override def receive = {
    case Join(name) =>
      sender() ! Joined(clientNamePairs)
    case ReceivedJoined(name) =>
      val senderPath = sender().path.toString

      // Notify other users new user has joined
      clients.foreach { userActor =>
        userActor ! NewUser(senderPath, name)
      }

      // Add client to the online clients HashMap
      clientNamePairs += (senderPath -> name)
      clients += sender()
    case _ => log.info("Unknown message received.")
  }

  override def preStart(): Unit = {
    log.info("Server started")
  }

}
