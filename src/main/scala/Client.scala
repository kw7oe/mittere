import akka.actor.{Actor, Props, ActorLogging, ActorSelection, ActorRef}
import scala.collection.immutable.HashMap

object Client {
  // Before Join
  case class RequestToJoin(serverAddress: String, portNumber: String, username: String)
  case class Joined(otherUsers: Map[String,String])

  // Joined
  case class NewUser(ref: String, name: String)
  case class RequestToCreateChat(actorRef: String)
  case object JoinChatRoom

  // Chatting
  case class RequestToSendMessage(msg: String)
  case class ReceiveMessage(from: String, msg: String)
}

class Client extends Actor with ActorLogging {
  import Client._

  // { "akka.tcp://..." -> "username" }
  var otherUsers: Map[String,String] = new HashMap()
  var serverActor: Option[ActorSelection] = None
  var chatRoomActor: Option[ActorRef] = None
  var username: Option[String] = None

  def receive = {
    case RequestToJoin(serverAddress, portNumber, name) =>
      serverActor = Some(MyApp.system.actorSelection(s"akka.tcp://chat@$serverAddress:$portNumber/user/server"))
      username = Some(name)
      serverActor.get ! Server.Join(username.get)
    case Joined(users)  =>
      otherUsers = users
      MyApp.displayActor ! Display.ShowUserList(otherUsers)
      sender() ! Server.ReceivedJoined(username.get)
      context.become(joined)
    case _ => log.info("Received unknown message")
  }

  def joined: Receive = {
    case NewUser(ref, name) =>
      otherUsers += (ref -> name)
      val user = User(ref, name)
      MyApp.displayActor ! Display.ShowJoin(user)
    case RequestToCreateChat(actorRef) =>
      serverActor.get ! Server.CreateChatRoom(actorRef)
    case JoinChatRoom =>
      chatRoomActor = Some(sender())
      MyApp.displayActor ! Display.ShowChatRoom
      context.become(chatting)
    case _ => log.info("Received unknown message")
  }

  def chatting: Receive = {
    case RequestToSendMessage(msg) =>
      chatRoomActor.get ! ChatRoom.Message(username.get, msg)
    case ReceiveMessage(from, msg) =>
      MyApp.displayActor ! Display.AddMessage(from, msg)
    case _ => log.info("Received unknown message")
  }

}
