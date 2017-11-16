import akka.actor.{Actor, Props, ActorLogging, ActorRef, Terminated}
import scala.collection.immutable.{HashMap, HashSet}
import scalafx.collections.{ObservableSet, ObservableHashSet}
import java.util.UUID.randomUUID


object Server {
  case class Join(username: String)
  case class CreateChatRoom(user: User)
  case class ChatRoomCreated(room: Room)
  case class UpdateRoom(key: String)
}

class Server extends Actor with ActorLogging {
  import Server._

  // A collection of client username and actor ref.
  var usernameToClient: Map[String, ActorRef] = new HashMap()
  var roomNameToRoom: Map[String, Room] = new HashMap()

  override def preStart() = log.info("Server started")
  override def postStop() = log.info("Server stopped")

  override def receive = {
    case Join(name) =>
      log.info(s"Join from $name")

      usernameToClient.foreach { case (_, userActor) =>
        userActor ! Client.NewUser(name, sender())
      }

      usernameToClient += (name -> sender())
      sender() ! Client.Joined(usernameToClient, roomNameToRoom)
    case ChatRoomCreated(room) =>
      log.info(s"ChatRoomCreated: $room")
      roomNameToRoom += (room.name -> room) 
    case UpdateRoom(key) =>
      log.info(s"UpdateRoom: $key")
      val room = roomNameToRoom.get(key)
      room match {
        case Some(r) =>
          r.users = sender() :: r.users 
        case None => // Do Nothing
      }
    case _ => log.info("Unknown message received.")
  }

}
