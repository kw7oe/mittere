import akka.actor.{Actor, Props, ActorLogging, ActorRef, ActorSelection}
import scala.collection.immutable.HashSet

object ChatRoom {
  def props(roomId: String, users: Set[String]): Props = 
    Props(new ChatRoom(roomId, users))

  case class Message(from: String, value: String)
}

class ChatRoom(
  roomId: String,
  users: Set[String]
) extends Actor with ActorLogging {
  import ChatRoom._

  var userSelections: Set[ActorSelection] = new HashSet()

  override def preStart() = {
    log.info(s"Chat Room $roomId started")
    users.foreach { actorPath => 
      val userActor = MyApp.system.actorSelection(actorPath)
      userSelections += userActor
      userActor ! Client.JoinChatRoom
    }
  }

  override def postStop() = log.info(s"Chat Room $roomId stopped")

  override def receive = {
    case Message(from, message) => 
      log.info(s"Received $message from $from")
      userSelections.foreach { actorSelection => 
        actorSelection ! Client.ReceiveMessage(from, message)
      }
    case _ => log.info("Receive unknown message")
  }
}

