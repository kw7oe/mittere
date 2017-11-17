import akka.actor.{Actor, ActorLogging, ActorSelection, ActorRef, DeadLetter}
import scala.collection.mutable.ArrayBuffer
import scala.collection.immutable.{HashMap, HashSet}

trait ChatManagement extends ActorLogging { this: Actor => 
  import Node._

  var serverActor: Option[ActorSelection]
  var username: Option[String]
  var usernameToClient: Map[String,ActorRef]
  var usernameToMessages: Map[String, ArrayBuffer[Room.Message]]
  var roomNameToRoom: Map[String, Room]

  protected def chatManagement: Receive = {
    case RequestToChatWith(chattable) =>
      log.info(s"RequestToChatWith: $chattable")

      chattable.chattableType match {
        case Personal =>
          if (!usernameToMessages.contains(chattable.key)) {
            usernameToMessages += (chattable.key -> new ArrayBuffer[Room.Message]())
          }
          val messages = usernameToMessages.get(chattable.key)
          MyApp.displayActor ! Display.ShowChatRoom(chattable, messages.get)
        case Group =>
          val room = roomNameToRoom.get(chattable.key)
          room match {
            case Some(r) => 
              if (!r.users.contains(self)) {
                // Broadcast to other users
                serverActor.get ! Server.UpdateRoom(chattable.key)
                usernameToClient.foreach { case (_, userActor) =>
                userActor ! JoinChatRoom(chattable.key)
              }
            }
            MyApp.displayActor ! Display.ShowChatRoom(chattable, r.messages)

          case None => // Do Nothing
          } 
        }
      case RequestToCreateChatRoom(tempRoom) =>
        log.info(s"RequestToCreateChatRoom: $tempRoom")

        if (roomNameToRoom.contains(tempRoom.name)) {
          MyApp.displayActor ! Display.ShowAlert(
            "Invalid Room Name", 
            "Room name has already been taken.", 
            "Please enter a different room name.")
        } else {
          // Initialize Room and add into roomNameToRoom
          if (!roomNameToRoom.contains(tempRoom.name)) {
            tempRoom.users = HashSet(self)
            roomNameToRoom += (tempRoom.name -> tempRoom)
          }

          val room = roomNameToRoom.get(tempRoom.name)

          // Inform the host
          serverActor.get ! Server.ChatRoomCreated(room.get)

          // Broadcast the created room to other users
          usernameToClient.foreach { case (_, userActor) =>
          userActor ! NewChatRoom(room.get)
        }
      }     
    case Typing(chattable) =>
      log.info(s"Typing: $chattable")
      chattable.chattableType match {
        case Group => 
          val room = roomNameToRoom.get(chattable.key)
          room foreach { r => 
            r.users.foreach { u =>
              u ! ReceiveShowTyping(chattable, chattable.key, username.get)
            }
          }
        case Personal => 
          val actor = usernameToClient.get(chattable.key)
          actor foreach { a => a ! ReceiveShowTyping(chattable, username.get, username.get)}
        }
    case ReceiveShowTyping(chattable, key, username) =>
      log.info(s"ReceiveShowTyping: $chattable, $username")
      if (username != this.username.get) {
        MyApp.displayActor ! Display.ShowTyping(chattable, key, username)
      }
    case RequestToSendMessage(chattable, msg) =>
      log.info(s"RequestToSendMessage: $chattable, $msg")
      chattable.chattableType match {
        case Group =>
          val room = roomNameToRoom.get(chattable.key).get
          log.info(s"${room.users}")
          room.users.foreach { actor =>
            val message = new Room.Message(username.get, msg)
            val key = chattable.key
            actor ! ReceiveMessage(chattable, key, message)
          }
        case Personal =>
          val actor = usernameToClient.get(chattable.key)
          val message = new Room.Message(username.get, msg)
          val key = username.get
          actor.get ! ReceiveMessage(chattable, key, message)
          if (key != chattable.key) {
            self ! ReceiveMessage(chattable, chattable.key, message)
          }
        }
    case ReceiveMessage(chattable, key, msg) =>
      log.info(s"ReceiveMessage: $chattable, $msg")

      chattable.chattableType match {
        case Group =>
          // Extremely DANGER
          roomNameToRoom.get(key).get.messages += msg
          MyApp.mainController.showUnread(key)
        case Personal =>
          if (!usernameToMessages.contains(key)) {
            usernameToMessages += (key -> new ArrayBuffer[Room.Message]())
          }
          MyApp.mainController.showUnread(key)
          usernameToMessages.get(key).get += msg
        }
        MyApp.displayActor ! Display.AddMessage(chattable, key, msg)    
  }
}