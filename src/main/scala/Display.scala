import akka.actor.{Actor, ActorLogging, ActorRef}
import scala.collection.mutable.ArrayBuffer
import scalafx.application.Platform

object Display {
  // Initialization
  case class Initialize(names: Map[String,Room],
                        rooms: Map[String,Room])
  case class ShowAlert(tuple: Tuple3[String, String, String])

  // User related
  case class ShowJoin(user: Room)
  case class RemoveJoin(user: Room)

  // Chat Room related
  case class ShowNewChatRoom(room: Room)
  case class ShowChatRoom(room: Room, 
                          messages: ArrayBuffer[Room.Message])

  // Chatting related
  case class ShowTyping(room: Room, username: String)
  case class AddMessage(room: Room, message: Room.Message)
}

class Display extends Actor with ActorLogging {
  import Display._
  import Node._

  def receive: Receive = {
    case Initialize(users, rooms) =>
      Platform.runLater {
        MyApp.mainController.initialize(users, rooms)
        MyApp.mainController.clearJoin()
      }

    case ShowAlert(tuple) => {
      Platform.runLater {
        MyApp.showAlert(tuple)
      }
    }
    case ShowJoin(user) =>
      Platform.runLater {
        MyApp.mainController.showJoin(user)
      }
    case RemoveJoin(user) =>
      Platform.runLater {
        MyApp.mainController.removeJoin(user)
      }      
    case ShowNewChatRoom(room) =>
      Platform.runLater {
        MyApp.mainController.showNewChatRoom(room)
      }
    case ShowChatRoom(room, messages) =>
      Platform.runLater {
        MyApp.chatController.initialize(room, messages)
        MyApp.mainController.showChatRoom
        MyApp.mainController.hideUnread(room.identifier)
      }
    case ShowTyping(room, username) =>
      if (shouldDisplay(room)) {
        Platform.runLater {
          MyApp.chatController.showStatus(username + " is typing...")
        }
      }      
    case AddMessage(room, message) =>
      Platform.runLater {
        if (shouldDisplay(room)) {
          MyApp.chatController.addMessage(message)
        } else {
          log.info("Cannot add message")
        }
      }     
    case _ => log.info("Receive unknown message")
  }

  def shouldDisplay(room: Room): Boolean = {
    MyApp.chatController.room match {
      case Some(c) =>
        room.identifier == c.identifier &&
        room.chatRoomType == c.chatRoomType
      case None => false
    }
  }
}
