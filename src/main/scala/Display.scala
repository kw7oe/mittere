import akka.actor.{Actor, ActorLogging}
import scalafx.application.Platform

object Display {
  case object ClearJoin
  case class ShowJoin(username: String)
  case class ShowUserList(names: Seq[String])
}

class Display extends Actor with ActorLogging {
  import Display._
  import Client._

  def receive: Receive = {
    case ClearJoin =>
      Platform.runLater {
        MyApp.controller.clearJoin()
      }
    case ShowJoin(name) =>
      Platform.runLater {
        MyApp.controller.showJoin(name)
      }
    case ShowUserList(names) =>
      Platform.runLater {
        MyApp.controller.showUserList(names)
      }
    case _ => log.info("Receive unknown message")
  }
}
