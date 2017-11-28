import akka.actor.{Actor, ActorLogging, ActorRef}
import scala.collection.immutable.{HashMap, TreeMap, SortedMap}

object SuperNode {
  case object Ping
  case class Join(username: String)
  case class ChatRoomCreated(room: Room)
  case class UpdateRoom(key: String)
  case class BecomeSuperNode(
    clients: SortedMap[String, ActorRef],
    rooms: Map[String, Room])
}

class SuperNode extends Actor with ActorLogging {
  import SuperNode._

  // A collection of client username and actor ref.
  var usernameToClient: SortedMap[String, ActorRef] = new TreeMap()
  var roomNameToRoom: Map[String, Room] = new HashMap()

  override def preStart() = {
    context.system.eventStream.subscribe(self, classOf[akka.remote.DisassociatedEvent])
    log.info("SuperNode started")
  }
  override def postStop() = log.info("SuperNode stopped")

  override def receive = {
    case akka.remote.DisassociatedEvent(local, remote, _) =>
      log.info("Supernode receive DisassociatedEvent")
      // When Remote User disconnected
      // Check which user disconnected
      val userInfo = usernameToClient.find { case(_, x) =>
       x.path.address == remote
      }

      // If the user exists in our record
      userInfo.foreach { value =>
        val disconnectedUsername = value._1
        val userRef = value._2

        // Remove tracking
        usernameToClient -= disconnectedUsername

        usernameToClient.foreach { case(_, ref) =>
          ref ! Node.RemoveUser(disconnectedUsername, userRef)
        }
      }
    case BecomeSuperNode(clients, rooms) =>
      log.info("I have become super node")
      usernameToClient = clients
      roomNameToRoom = rooms

      usernameToClient.foreach { case (name, ref) =>
        ref ! Node.NewSuperNode
      }
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
    case Ping => log.info("Receive Ping")
    case _ => log.info("Unknown message received.")
  }

}
