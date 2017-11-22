import akka.actor.{Actor, ActorLogging, ActorSelection, ActorRef, DeadLetter}
import scala.collection.mutable.ArrayBuffer
import scala.collection.immutable.{SortedMap, HashSet}

trait ChatManagement extends ActorLogging { this: Actor =>
  import Node._

  var superNodeActor: Option[ActorSelection]
  var username: Option[String]
  var usernameToClient: SortedMap[String,ActorRef]
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
          superNodeActor.get ! SuperNode.UpdateRoom(chatRoom.identifier)
          usernameToClient.foreach { case (_, userActor) =>
            userActor ! JoinChatRoom(chatRoom.identifier)
          }
        }
        val users = usernameToClient.filter { case (name, ref) =>
          r.users.contains(ref)
        }.keySet
        MyApp.displayActor ! Display.ShowChatRoom(chatRoom, r.messages, users)
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
      val room = chatRoom.chatRoomType match {
        case Group => roomNameToRoom.get(chatRoom.identifier).get
        case Personal => usernameToRoom.get(chatRoom.identifier).get
      }
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
      room.foreach { r => r.messages += msg }
      MyApp.displayActor ! Display.AddMessage(chatRoom,msg)
    case _ => log.info("Receive unknown message.")
  }
}