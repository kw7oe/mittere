import akka.actor.{Actor, ActorLogging}
import scalafx.application.Platform

object Display {
  case class ShowJoin(username: User)
  case class ShowUserList(names: Map[String,String])
  case object ShowChatRoom
}

class Display extends Actor with ActorLogging {
  import Display._
  import Client._

  def receive: Receive = {
    case ShowJoin(user) =>
      Platform.runLater {
        MyApp.controller.showJoin(user)
      }
    case ShowUserList(users) =>
      Platform.runLater {
        MyApp.controller.showUserList(users)
        MyApp.controller.clearJoin()
      }
    case ShowChatRoom =>
      Platform.runLater {
        MyApp.controller.showChatRoom
      }
    case _ => log.info("Receive unknown message")
  }
}
