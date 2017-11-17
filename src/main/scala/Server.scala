import akka.actor.{Actor, ActorLogging, ActorRef}
import scala.collection.immutable.{HashMap, HashSet}

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
      // Join request from new node
      log.info(s"Join from $name")

      // Check username is unique
      if (usernameToClient.contains(name)) {
        // If it is not, inform that the username is invalid
        sender() ! Node.InvalidUsername
      } else {

        // Broadcast to other node about the new node
        usernameToClient.foreach { case (_, userActor) =>
          userActor ! Node.NewUser(name, sender())
        }

        // Keep track of the detail
        usernameToClient += (name -> sender())

        // Inform node about other nodes details 
        // and rooms details
        sender() ! Node.Joined(usernameToClient, roomNameToRoom)
      }      
    case ChatRoomCreated(room) =>
      log.info(s"ChatRoomCreated: $room")
      // Keep track of room created
      roomNameToRoom += (room.name -> room) 
    case UpdateRoom(key) =>
      log.info(s"UpdateRoom: $key")

      // To update the room users
      val room = roomNameToRoom.get(key)
      room.foreach { r => r.users += sender() }
    case _ => log.info("Unknown message received.")
  }

}
