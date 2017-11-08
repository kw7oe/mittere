import akka.actor.{Actor, ActorLogging}
import scalafx.application.Platform

object Display {
  case class DisplayJoined(username: String)
}

class Display extends Actor with ActorLogging {
  import Display._
  import Client._

  def receive: Receive = {
    case DisplayJoined(name) =>
      Platform.runLater {
        MyApp.controller.showJoined(name)
      }
    case _ => log.info("Receive unknown message")
  }
}