import akka.actor.{Actor, Props, ActorLogging, ActorRef, ActorSelection}
import scala.collection.mutable.ArrayBuffer
import scala.collection.immutable.HashSet

object ChatRoom {
  def props(roomId: String, usersPath: Set[String]): Props = 
    Props(new ChatRoom(roomId, usersPath))

  case class Join(user: User)
  case class Invite(user: User, actor: ActorRef)
  case class Message(from: String, value: String)
}

class ChatRoom(
  roomId: String,
  usersPath: Set[String]
) extends Actor with ActorLogging {
  import ChatRoom._

  var userSelections: Set[ActorSelection] = new HashSet()
  var messages: ArrayBuffer[Message] = new ArrayBuffer[Message]()

  override def preStart() = {
    log.info(s"Chat Room $roomId started")
    usersPath.foreach { actorPath => 
      val userActor = MyApp.system.actorSelection(actorPath)
      userSelections += userActor
      userActor ! Client.ChatRoomCreated(roomId)
    }
  }

  override def postStop() = log.info(s"Chat Room $roomId stopped")

  override def receive = {
    case Invite(user, actor) => 
      actor ! Client.JoinChatRoom(user, roomId, messages)
    case Join(user) =>
      sender() ! Client.JoinChatRoom(user, roomId, messages)
    case msg @ Message(from, message) => 
      messages += msg
      log.info(s"Received $message from $from")
      userSelections.foreach { actorSelection => 
        actorSelection ! Client.ReceiveMessage(from, message)
      }
    case _ => log.info("Receive unknown message")
  }
}

