import akka.actor.{Actor, ActorLogging, ActorRef}
import scala.collection.mutable.ArrayBuffer
import scalafx.application.Platform

object Display {
  case class ShowJoin(user: User)
  case class RemoveJoin(user: User)
  case class ShowUserList(names: Map[String,ActorRef])
  case class ShowChatRoom(user: User, roomId: Option[String], messages: ArrayBuffer[ChatRoom.Message])
  case class ShowTyping(roomId: String, username: String)
  case class AddMessage(roomType: ChatRoomType, key: String, message: ChatRoom.Message)
}

class Display extends Actor with ActorLogging {
  import Display._
  import Client._

  def receive: Receive = {
    case ShowJoin(user) =>
      Platform.runLater {
        MyApp.mainController.showJoin(user)
      }
    case RemoveJoin(user) =>
      Platform.runLater {
        MyApp.mainController.removeJoin(user)
        if (Some(user) == MyApp.chatController.user) {
          MyApp.chatController.showStatus("is offline")
        }
      }      
    case ShowUserList(users) =>
      Platform.runLater {
        MyApp.mainController.showUserList(users)
        MyApp.mainController.clearJoin()
      }
    case ShowChatRoom(user, roomId, messages) =>
      Platform.runLater {
        MyApp.chatController.initialize(roomId, messages, user)
        MyApp.mainController.showChatRoom
      }
    case ShowTyping(roomId, username) =>
      if (roomId == MyApp.chatController.roomId) {
        Platform.runLater {
          MyApp.chatController.showStatus(username)
        }
      }      
    case AddMessage(roomType, key, message) =>
      Platform.runLater {
        roomType match {
          case Group =>
            log.info("AddMessage Group")
            if (key == MyApp.chatController.roomId) {
              MyApp.chatController.addMessage(message)
            }
          case Personal =>
            log.info("AddMesage Personal")
            if (Some(key) == MyApp.chatController.username) {
              MyApp.chatController.addMessage(message)
            }
        }
      }     
      
    case _ => log.info("Receive unknown message")
  }
}
