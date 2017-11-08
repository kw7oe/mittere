import akka.actor.{Actor, Props, ActorLogging, ActorSelection}

object Client {
  case class JoinRequest(serverAddress: String, portNumber: String, username: String)
  case object Joined
}

class Client extends Actor with ActorLogging {
  import Client._
  import Display._

  var serverActor: Option[ActorSelection] = None
  var username: Option[String] = None

  def receive = {
    case JoinRequest(serverAddress, portNumber, name) =>
      log.info(serverAddress)
      log.info(portNumber)
      
      serverActor = Some(MyApp.system.actorSelection(s"akka.tcp://chat@$serverAddress:$portNumber/user/server"))
      username = Some(name)
      serverActor.get ! Server.Join(username.get)
    case Joined =>
      MyApp.displayActor ! DisplayJoined(username.get)
      context.become(joined)
    case _ => log.info("Received unknown message")
  }

  def joined: Receive = {
    case _ => log.info("Received unknown message")
  }

}
