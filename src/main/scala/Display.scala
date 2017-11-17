import akka.actor.{Actor, ActorLogging, ActorRef}
import scala.collection.mutable.ArrayBuffer
import scalafx.application.Platform

object Display {
  // Initialization
  case class Initialize(names: Map[String,ActorRef],
                        rooms: Map[String,Room])
  case class ShowAlert(tuple: Tuple3[String, String, String])

  // User related
  case class ShowJoin(user: User)
  case class RemoveJoin(user: User)

  // Chat Room related
  case class ShowNewChatRoom(room: Room)
  case class ShowChatRoom(chattable: Chattable, 
                          messages: ArrayBuffer[Room.Message])

  // Chatting related
  case class ShowTyping(chattable: Chattable, key: String, username: String)
  case class AddMessage(chattable: Chattable,
                        key: String, 
                        message: Room.Message)
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
    case ShowChatRoom(chattable, messages) =>
      Platform.runLater {
        MyApp.chatController.initialize(chattable, messages)
        MyApp.mainController.showChatRoom
        MyApp.mainController.hideUnread(chattable.key)
      }
    case ShowTyping(chattable, key, username) =>
      if (shouldDisplay(chattable, key)) {
        Platform.runLater {
          MyApp.chatController.showStatus(username + " is typing...")
        }
      }      
    case AddMessage(chattable, key, message) =>
      Platform.runLater {
        if (shouldDisplay(chattable, key)) {
          MyApp.chatController.addMessage(message)
        } else {
          log.info("Cannot add message")
        }
      }     
    case _ => log.info("Receive unknown message")
  }

  def shouldDisplay(chattable: Chattable, key: String): Boolean = {
    return !MyApp.chatController.chattable.isEmpty && 
           key == MyApp.chatController.chattable.get.key &&
           chattable.chattableType == MyApp.chatController.chattable.get.chattableType
  }
}
