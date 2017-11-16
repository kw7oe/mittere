import akka.actor.{Actor, ActorLogging, ActorRef}
import scala.collection.mutable.ArrayBuffer
import scalafx.application.Platform

object Display {
  // Initialization
  case class Initialize(names: Map[String,ActorRef],
                        rooms: Map[String,Room])
  case class ShowAlert(title: String,
                       headerText: String,
                       contentText: String)

  // User related
  case class ShowJoin(user: User)
  case class RemoveJoin(user: User)

  // Chat Room related
  case class ShowNewChatRoom(room: Room)
  case class ShowChatRoom(chattable: Chattable, 
                          messages: ArrayBuffer[Room.Message])

  // Chatting related
  case class ShowTyping(roomId: String, username: String)
  case class AddMessage(chattable: Chattable,
                        key: String, 
                        message: Room.Message)
}

class Display extends Actor with ActorLogging {
  import Display._
  import Client._

  def receive: Receive = {
    case Initialize(users, rooms) =>
      Platform.runLater {
        MyApp.mainController.initialize(users, rooms)
        MyApp.mainController.clearJoin()
      }

    case ShowAlert(title, headerText, contentText) => {
      Platform.runLater {
        MyApp.showAlert(
          title,
          headerText,
          contentText
        )
      }
    }
    case ShowJoin(user) =>
      Platform.runLater {
        MyApp.mainController.showJoin(user)
      }
    case RemoveJoin(user) =>
      Platform.runLater {
        MyApp.mainController.removeJoin(user)
        // if (Some(user) == MyApp.chatController.user) {
        //   MyApp.chatController.showStatus("is offline")
        // }
      }      
    case ShowNewChatRoom(room) =>
      Platform.runLater {
        MyApp.mainController.showNewChatRoom(room)
      }
    case ShowChatRoom(chattable, messages) =>
      Platform.runLater {
        MyApp.chatController.initialize(chattable, messages)
        MyApp.mainController.showChatRoom
      }
    case ShowTyping(roomId, username) =>
      // if (roomId == MyApp.chatController.roomId) {
      //   Platform.runLater {
      //     MyApp.chatController.showStatus(username)
      //   }
      // }      
    case AddMessage(chattable, key, message) =>
      Platform.runLater {
        if (shouldDisplayMessage(chattable, key)) {
          MyApp.chatController.addMessage(message)
          MyApp.mainController.showUnread(key)
        } else {
          log.info("Cannot add message")
        }
      }     
    case _ => log.info("Receive unknown message")
  }

  def shouldDisplayMessage(chattable: Chattable, key: String): Boolean = {
    return !MyApp.chatController.chattable.isEmpty && 
           key == MyApp.chatController.chattable.get.key &&
           chattable.chattableType == MyApp.chatController.chattable.get.chattableType
  }
}
