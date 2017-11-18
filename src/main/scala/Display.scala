import akka.actor.{Actor, ActorLogging, ActorRef}
import scala.collection.mutable.ArrayBuffer
import scalafx.application.Platform

object Display {
  // Initialization
  case class Initialize(names: Map[String,Room],
                        rooms: Map[String,Room],
                        username: String)
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
    case Initialize(users, rooms, username) =>
      Platform.runLater {
        //after user joined, setup
        MyApp.mainController.initialize(users, rooms, username)
        MyApp.showMain()
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
        MyApp.mainController.addChatroom(room)
      }
    case ShowChatRoom(room, messages) =>
      Platform.runLater {
        MyApp.mainController.showRoom(room, messages)
        MyApp.mainController.hideUnread(room)
      }
    case ShowTyping(room, username) =>
      log.info(s"Send typing with $username")
      if (shouldDisplay(room)) {
        Platform.runLater {
          MyApp.mainController.showStatus(username)
        }
      }      
    case AddMessage(room, message) =>
      Platform.runLater {
        if (shouldDisplay(room)) {
          MyApp.mainController.addMessage(message)
        } else {
          log.info("Cannot add message")
          room.chatRoomType match {
            case Group =>
              MyApp.mainController.showUnread(room)
            case Personal =>
              MyApp.mainController.showUnread(room)
          }
        }
      }     
    case _ => log.info("Receive unknown message")
  }

  def shouldDisplay(room: Room): Boolean = {
    MyApp.mainController.room match {
      case Some(c) =>
        room.identifier == c.identifier &&
        room.chatRoomType == c.chatRoomType
      case None => false
    }
  }
}
