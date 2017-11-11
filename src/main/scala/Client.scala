import akka.actor.{Actor, Props, ActorLogging, ActorSelection}
import scala.collection.immutable.HashMap

object Client {
  case class RequestToJoin(serverAddress: String, portNumber: String, username: String)
  case class Joined(otherUsers: Map[String,String])

  case class NewUser(ref: String, name: String)
  case class RequestToMessage(actorRef: String)
  case object JoinChatRoom
}

class Client extends Actor with ActorLogging {
  import Client._

  // { "akka.tcp://..." -> "username" }
  var otherUsers: Map[String,String] = new HashMap()
  var serverActor: Option[ActorSelection] = None
  var username: Option[String] = None

  def receive = {
    case RequestToJoin(serverAddress, portNumber, name) =>
      serverActor = Some(MyApp.system.actorSelection(s"akka.tcp://chat@$serverAddress:$portNumber/user/server"))
      username = Some(name)
      serverActor.get ! Server.Join(username.get)
    case Joined(users)  =>
      otherUsers = users
      MyApp.displayActor ! Display.ShowUserList(otherUsers)
      MyApp.displayActor ! Display.ClearJoin
      sender() ! Server.ReceivedJoined(username.get)
      context.become(joined)
    case _ => log.info("Received unknown message")
  }

  def joined: Receive = {
    case NewUser(ref, name) =>
      otherUsers += (ref -> name)
      val user = User(ref, name)
      MyApp.displayActor ! Display.ShowJoin(user)
    case RequestToMessage(actorRef) =>
      serverActor.get ! Server.CreateChatRoom(actorRef)
    case JoinChatRoom =>
      MyApp.displayActor ! Display.ShowChatRoom
      context.become(chatting)
    case _ => log.info("Received unknown message")
  }

  def chatting: Receive = {
    case _ => log.info("Received unknown message")
  }

}
