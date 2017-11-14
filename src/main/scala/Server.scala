import akka.actor.{Actor, Props, ActorLogging, ActorRef, Terminated}
import scalafx.collections.{ObservableHashMap, ObservableMap, ObservableSet, ObservableHashSet}
import scala.collection.immutable.HashMap
import java.util.UUID.randomUUID


object Server {
  case class Join(username: String)
  case class ReceivedJoined(username: String)
  case class CreateChatRoom(user: User)
  case class ChatRoomCreated()
}

class Server extends Actor with ActorLogging {
  import Server._

  // A collection of client username and actor path.
  //
  // E.g. 
  //   { "akka.tcp://..." -> "username" }
  var clientNamePairs: Map[String,String] = new HashMap()

  // A collection of joined Client ActorRef
  var clients: ObservableSet[ActorRef] = new ObservableHashSet() 

  // A collection of created ChatRoom ActorRef and their 
  // roomId
  //
  // E.g 
  //   { "actorRef" -> 'random_uuid' }
  var chatRoomToUUID: Map[ActorRef, String] = new HashMap()

  override def preStart() = log.info("Server started")
  override def postStop() = log.info("Server stopped")

  override def receive = {
    case Join(name) =>
      // Send all online users details to client
      sender() ! Client.Joined(clientNamePairs)
    case ReceivedJoined(name) =>
      val senderPath = sender().path.toString

      // Notify other users new user has joined
      clients.foreach { userActor =>
        userActor ! Client.NewUser(senderPath, name)
      }

      log.info(s"$clients")

      // Keep track of the clients info
      clientNamePairs += (senderPath -> name)
      clients += sender()
    case CreateChatRoom(user) =>
      val senderPath = sender().path.toString
      val roomId = generateUUID

      val userPaths = Array(senderPath, user.actorPath)
      val chatRoomActor = context.actorOf(
        ChatRoom.props(roomId, userPaths), s"chatroom-$roomId")
      
      context.watch(chatRoomActor)
      chatRoomToUUID += (chatRoomActor -> roomId)
      chatRoomActor ! ChatRoom.Invite(user, sender())
    case Terminated(chatRoomActor) =>
      log.info(s"Chat Room $chatRoomActor has been terminated")
      chatRoomToUUID -= chatRoomActor
    case _ => log.info("Unknown message received.")
  }

  // Generate unique id for each ChatRoom Actor
  private def generateUUID: String = {   
    return randomUUID.toString
  }



}
