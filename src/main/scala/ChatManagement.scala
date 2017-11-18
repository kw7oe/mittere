import akka.actor.{Actor, ActorLogging, ActorSelection, ActorRef, DeadLetter}
import scala.collection.mutable.ArrayBuffer
import scala.collection.immutable.{HashMap, HashSet}

trait ChatManagement extends ActorLogging { this: Actor => 
  import Node._

  var serverActor: Option[ActorSelection]
  var username: Option[String]
  var usernameToClient: Map[String,ActorRef]
  var usernameToRoom: Map[String, Room]
  var roomNameToRoom: Map[String, Room]

  protected def chatManagement: Receive = {
    case RequestToChatWith(chatRoom) =>
      log.info(s"RequestToChatWith: $chatRoom")

      val room = chatRoom.chatRoomType match {
        case Group => roomNameToRoom.get(chatRoom.identifier)
        case Personal => usernameToRoom.get(chatRoom.identifier)
      }

      room.foreach { r => 
          if (!r.users.contains(self)) {
            // Broadcast to other users
            serverActor.get ! Server.UpdateRoom(chatRoom.identifier)
            usernameToClient.foreach { case (_, userActor) =>
              userActor ! JoinChatRoom(chatRoom.identifier)
            }
          }
        MyApp.displayActor ! Display.ShowChatRoom(chatRoom, r.messages)
      }
    case Typing(chatRoom) =>
      log.info(s"Typing: $chatRoom")
      val room = chatRoom.chatRoomType match {
        case Group => roomNameToRoom.get(chatRoom.identifier)
        case Personal => usernameToRoom.get(chatRoom.identifier)
      }

      room foreach { r => 
        r.users.foreach { u =>
          u ! ReceiveShowTyping(chatRoom, username.get)
        }
      }
    case ReceiveShowTyping(chatRoom, username) =>
      log.info(s"ReceiveShowTyping: $chatRoom, $username")
      if (username != this.username.get) {
        MyApp.displayActor ! Display.ShowTyping(chatRoom, username)
      }
    case RequestToSendMessage(chatRoom, msg) =>
      log.info(s"RequestToSendMessage: $chatRoom, $msg")
      // WARNING
      val room = chatRoom.chatRoomType match {
        case Group => roomNameToRoom.get(chatRoom.identifier).get
        case Personal => usernameToRoom.get(chatRoom.identifier).get
      }
      log.info(s"${room.users}")
      room.users.foreach { actor =>
        val message = new Room.Message(username.get, msg)
        val key = chatRoom.identifier
        actor ! ReceiveMessage(chatRoom, message)
      }
    case ReceiveMessage(chatRoom, msg) =>
      log.info(s"ReceiveMessage: $chatRoom, $msg")
      val room = chatRoom.chatRoomType match {
        case Group => roomNameToRoom.get(chatRoom.identifier)
        case Personal => usernameToRoom.get(chatRoom.identifier)
      }
      // WARNING
      room.get.messages += msg
      MyApp.displayActor ! Display.AddMessage(chatRoom,msg)    
  }
}