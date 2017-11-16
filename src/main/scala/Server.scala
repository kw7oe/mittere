import akka.actor.{Actor, Props, ActorLogging, ActorRef, Terminated}
import scala.collection.immutable.{HashMap, HashSet}
import scalafx.collections.{ObservableSet, ObservableHashSet}
import java.util.UUID.randomUUID


object Server {
  case class Join(username: String)
  case class CreateChatRoom(user: User)
  case class ChatRoomCreated(room: Room)
}

class Server extends Actor with ActorLogging {
  import Server._

  // A collection of client username and actor ref.
  var usernameToClient: Map[String, ActorRef] = new HashMap()
  var roomNameToRoom: Map[String, Room] = new HashMap()

  // A collection of created ChatRoom ActorRef and their 
  // roomId
  //
  // E.g 
  //   { "actorRef" -> 'random_uuid' }
  // var chatRoomToUUID: Map[ActorRef, String] = new HashMap()

  override def preStart() = log.info("Server started")
  override def postStop() = log.info("Server stopped")

  override def receive = {
    case Join(name) =>
      log.info(s"Join from $name")

      // Multicast new user to all online users
      usernameToClient.foreach { case (_, userActor) =>
        userActor ! Client.NewUser(name, sender())
      }

      // Update the collection
      usernameToClient += (name -> sender())

      // Send the online users info to the client
      sender() ! Client.Joined(usernameToClient, roomNameToRoom)
    case ChatRoomCreated(room) =>
      roomNameToRoom += (room.name -> room) 
    case CreateChatRoom(user) =>
      log.info(s"CreateChatRoom: $user")
      // val senderPath = sender().path.toString
      // val roomId = generateUUID

      // val userPaths = Array(senderPath, user.actorPath)
      // val chatRoomActor = context.actorOf(
      //   ChatRoom.props(roomId, userPaths), s"chatroom-$roomId")
      
      // context.watch(chatRoomActor)
      // chatRoomToUUID += (chatRoomActor -> roomId)
      // chatRoomActor ! ChatRoom.Invite(user, sender())
    case Terminated(actor) =>
      log.info(s"$actor has been terminated")
      // chatRoomToUUID -= actor
    case _ => log.info("Unknown message received.")
  }

  // Generate unique id for each ChatRoom Actor
  // private def generateUUID: String = {   
  //   return randomUUID.toString
  // }



}
