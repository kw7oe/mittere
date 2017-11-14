import akka.actor.{Actor, Props, ActorLogging, ActorSelection, ActorRef}
import scala.collection.mutable.ArrayBuffer
import scala.collection.immutable.HashMap

object Client {
  // Before Join
  case class RequestToJoin(serverAddress: String, portNumber: String, username: String)
  case class Joined(otherUsers: Map[String,String])

  // Joined
  case class NewUser(ref: String, name: String)
  case class RequestToGetChatWith(user: User)
  case class ChatRoomCreated(userPath: String, roomId: String)
  case class JoinChatRoom(user: User, roomId: String, messages: ArrayBuffer[ChatRoom.Message])

  // Chatting
  case class RequestToSendMessage(roomId: String, msg: String)
  case class ReceiveMessage(roomId: String, message: ChatRoom.Message)
}

class Client extends Actor with ActorLogging {
  import Client._

  // { "akka.tcp://..." -> "username" }
  var otherUsers: Map[String,String] = new HashMap()
  var serverActor: Option[ActorSelection] = None
  var chatRoomActors: Map[String, ActorRef] = new HashMap()
  var actorPathToChatRoomActors: Map[String, ActorRef] = new HashMap()
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
    // When new user connect to the same server
    case NewUser(ref, name) =>
      otherUsers += (ref -> name)
      val user = User(ref, name)
      MyApp.displayActor ! Display.ShowJoin(user)
    // When user click the username on the user lists
    case RequestToGetChatWith(user) => 
      log.info(s"RequestToGetChatWith $user")

      // Check if ChatRoom already existed
      val actorSelection = actorPathToChatRoomActors.get(user.actorPath)

      actorSelection match {
        // Join if exist
        case Some(value) => value ! ChatRoom.Join(user)
        // Request server to create a chat room
        case None => serverActor.get ! Server.CreateChatRoom(user)
      }
    // Get notified when chat room is created
    case ChatRoomCreated(userPath, roomId) =>
      log.info(s"ChatRoomCreated with $roomId")
      actorPathToChatRoomActors += (userPath -> sender())
      chatRoomActors += (roomId -> sender())
    // Get notified to show chat room
    case JoinChatRoom(user, roomId, messages) =>
      log.info(s"Join chatRoom $user - $roomId")
      MyApp.displayActor ! Display.ShowChatRoom(user, roomId, messages)
    // Request to send message
    case RequestToSendMessage(roomId, msg) =>
      val actor = chatRoomActors.get(roomId)
      actor.get ! ChatRoom.Message(username.get, msg)
    case ReceiveMessage(roomId, msg) =>
      MyApp.displayActor ! Display.AddMessage(roomId, msg)
    case _ => log.info("Received unknown message")
  }

}
