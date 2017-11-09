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

  var clientNamePairs: Map[String,String] = new HashMap()
  var clients: ObservableSet[ActorRef] = new ObservableHashSet() 

  override def receive = {
    case Join(name) =>
      val senderPath = sender().path.toString
      // Return user the online clients
      sender() ! Joined(clientNamePairs)
      log.info(s"${clients.size} clients currently.")

      // Notify other users new user has joined
      clients.foreach { userActor =>
        log.info(s"Send all users to $userActor")
        userActor ! NewUser(senderPath, name)
      }
    case ReceivedJoined(name) =>
      val senderPath = sender().path.toString
      // Add client to the online clients HashMap
      clientNamePairs += (senderPath -> name)
      clients += sender()
      log.info(s"${clients.size} clients currently.")
    case _ => log.info("Unknown message received.")
  }

  override def preStart(): Unit = {
    log.info("Server started")
  }

}
