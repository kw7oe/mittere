import akka.actor.{Actor, ActorLogging}
import scala.collection.mutable.ArrayBuffer
import scalafx.application.Platform

object Display {
  case class ShowJoin(username: User)
  case class ShowUserList(names: Map[String,String])
  case class ShowChatRoom(user: User, roomId: String, messages: ArrayBuffer[ChatRoom.Message])
  case class AddMessage(from: String, message: String)
}

class Display extends Actor with ActorLogging {
  import Display._
  import Client._

  def receive: Receive = {
    case ShowJoin(user) =>
      Platform.runLater {
        MyApp.mainController.showJoin(user)
      }
    case ShowUserList(users) =>
      Platform.runLater {
        MyApp.mainController.showUserList(users)
        MyApp.mainController.clearJoin()
      }
    case ShowChatRoom(user, roomId, messages) =>
      Platform.runLater {
        MyApp.chatController.messages = messages
        MyApp.chatController.user = user
        MyApp.chatController.roomId = roomId
        MyApp.mainController.showChatRoom
      }
    case AddMessage(from, message) =>
      Platform.runLater {
        MyApp.chatController.addMessage(from, message)
      }
    case _ => log.info("Receive unknown message")
  }
}
