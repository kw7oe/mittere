import akka.actor.{Actor, ActorLogging, ActorSelection, ActorRef, DeadLetter}
import scala.collection.mutable.ArrayBuffer
import scala.collection.immutable.{HashMap, HashSet}

object Node {
  // SessionManagement
  case object InvalidUsername
  case class RequestToJoin(serverAddress: String,
                           portNumber: String,
                           username: String)
  case class Joined(clients: Map[String, ActorRef],
                    rooms:  Map[String, Room])
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
  SessionManagement with
  ChatManagement {  
}