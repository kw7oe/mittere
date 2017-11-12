import akka.actor.{Actor, ActorLogging}
import scalafx.application.Platform

object Display {
  case class ShowJoin(username: User)
  case class ShowUserList(names: Map[String,String])
  case object ShowChatRoom
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
    case ShowChatRoom =>
      Platform.runLater {
        MyApp.mainController.showChatRoom
      }
    case AddMessage(from, message) =>
      Platform.runLater {
        MyApp.chatController.addMessage(from, message)
      }
    case _ => log.info("Receive unknown message")
  }
}
