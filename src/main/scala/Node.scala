import akka.actor.ActorRef
import scala.collection.immutable.SortedMap

object Node {
  // JoinManagement
  case object InvalidUsername
  case class RequestToJoin(serverAddress: String,
                           portNumber: String,
                           username: String)
  case class Joined(clients: SortedMap[String, ActorRef],
                    rooms:  Map[String, Room])

  // SessionManagement
  case object NewSuperNode
  case class NewUser(name: String, actorRef: ActorRef)
  case class RequestToCreateChatRoom(roomName: String)
  case class JoinChatRoom(key: String)
  case class NewChatRoom(room: Room)

  // Chat Management
  case class RequestToChatWith(room: Room)
  case class Typing(room: Room)
  case class ReceiveShowTyping(room: Room, username: String)
  case class RequestToSendMessage(room: Room, msg: String)
  case class ReceiveMessage(room: Room, message: Room.Message)
}

class Node extends
  NodeActor with
  JoinManagement with
  SessionManagement with
  ChatManagement {
}