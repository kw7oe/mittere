import akka.actor.{Actor, ActorLogging, ActorRef}
import scala.collection.mutable.ArrayBuffer
import scalafx.application.Platform

object Display {
  // Initialization
  case class Initialize(names: Map[String,ActorRef],
                        rooms: Map[String,Room])

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
    case AddMessage(chattable, message) =>
      Platform.runLater {
        if (Some(chattable) == MyApp.chatController.chattable) {
          MyApp.chatController.addMessage(message)
        }
      }     
      
    case _ => log.info("Receive unknown message")
  }
}
