import akka.actor.ActorRef

object Node {
  // JoinManagement
  case object InvalidUsername
  case class RequestToJoin(serverAddress: String,
                           portNumber: String,
                           username: String)
  case class Joined(clients: Map[String, ActorRef],
                    rooms:  Map[String, Room])

  // SessionManagement
  case class NewUser(name: String, actorRef: ActorRef)
  case class RequestToCreateChatRoom(room: Room)
  case class JoinChatRoom(key: String)
  case class NewChatRoom(room: Room)

  // Chat Management
  case class RequestToChatWith(chattable: Chattable)
  case class Typing(chattable: Chattable)
  case class ReceiveShowTyping(chattable: Chattable, key: String, username: String)
  case class RequestToSendMessage(chattable: Chattable, msg: String)
  case class ReceiveMessage(chattable: Chattable, key: String, message: Room.Message)
}

class Node extends 
  NodeActor with 
  JoinManagement with
  SessionManagement with
  ChatManagement {  
}