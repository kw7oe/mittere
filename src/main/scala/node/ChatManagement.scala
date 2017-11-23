import akka.actor.{Actor, ActorLogging, ActorSelection, ActorRef, Props}
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.collection.immutable.{SortedMap, HashSet}

trait ChatManagement extends ActorLogging { this: Actor =>
  import Node._

  var superNodeActor: Option[ActorSelection]
  var username: Option[String]
  var usernameToClient: SortedMap[String,ActorRef]
  var usernameToRoom: Map[String, Room]
  var roomNameToRoom: Map[String, Room]

  val connectionTimeoutMessage = (
    "Connection Timeout",
    "Failed to send the message.",
    "Please try again later."
  )

  protected def chatManagement: Receive = {
    case RequestToChatWith(chatRoom) =>
      log.info(s"RequestToChatWith: $chatRoom")

      val room = chatRoom.chatRoomType match {
        case Group => roomNameToRoom.get(chatRoom.identifier)
        case Personal => usernameToRoom.get(chatRoom.identifier)
      }

      room.foreach { r =>
        if (!r.users.contains(self)) {
          // Broadcast to other users
          superNodeActor.get ! SuperNode.UpdateRoom(chatRoom.identifier)
          usernameToClient.foreach { case (_, userActor) =>
            userActor ! JoinChatRoom(chatRoom.identifier)
          }
        }
        val users = usernameToClient.filter { case (name, ref) =>
          r.users.contains(ref)
        }.keySet
        MyApp.displayActor ! Display.ShowChatRoom(chatRoom, r.messages, users)
      }
    case Typing(chatRoom) =>
      log.info(s"Typing: $chatRoom")
      val room = chatRoom.chatRoomType match {
        case Group => roomNameToRoom.get(chatRoom.identifier)
        case Personal => usernameToRoom.get(chatRoom.identifier)
      }

      room foreach { r =>
        r.users.foreach { u =>
          u ! ReceiveShowTyping(chatRoom, username.get)
        }
      }
    case ReceiveShowTyping(chatRoom, username) =>
      log.info(s"ReceiveShowTyping: $chatRoom, $username")
      if (username != this.username.get) {
        MyApp.displayActor ! Display.ShowTyping(chatRoom, username)
      }
    case RequestToSendMessage(chatRoom, msg) =>
      log.info(s"RequestToSendMessage: $chatRoom, $msg")
      val room = chatRoom.chatRoomType match {
        case Group => roomNameToRoom.get(chatRoom.identifier).get
        case Personal => usernameToRoom.get(chatRoom.identifier).get
      }
      room.users.foreach { actor =>
        val message = new Room.Message(username.get, msg)
        val key = chatRoom.identifier

        // Ask is avoided to reduce the overhead of the system.
        // According to Akka documentation, Tell gives the best
        // concurrency and scalability characteristics. Hence,
        // Tell is used as the frequency of this action is
        // quite high in a chat system.
        //
        // We create another actor to send the message
        //
        // This allow the received actor to send
        // back acknowledgement seperately to the
        // created extra actor.
        val extraActor = buildExtraActor
        actor.tell(ReceiveMessage(chatRoom, message), extraActor)

        // Schedule the timeout after 2 second to the created
        // extra actor
        context.system.scheduler.scheduleOnce(2 second, extraActor, Timeout)

      }
    case ReceiveMessage(chatRoom, msg) =>
      log.info(s"ReceiveMessage: $chatRoom, $msg")

      // Send back to acknowledge the message is received
      // successfully
      sender() ! Acknowledge

      val room = chatRoom.chatRoomType match {
        case Group => roomNameToRoom.get(chatRoom.identifier)
        case Personal => usernameToRoom.get(chatRoom.identifier)
      }
      room.foreach { r => r.messages += msg }
      MyApp.displayActor ! Display.AddMessage(chatRoom,msg)
    case t => log.info(s"Receive unhandled message: $t")
  }

  private def displayAlert(messages: Tuple3[String, String, String]) {
    MyApp.displayActor ! Display.ShowAlert(messages)
  }

  // We create a temporary actor to handle the message passing
  // This approach is refer from Learning Akka, Chapter 3, section
  // "Designing with Tell"
  private def buildExtraActor: ActorRef = {
    return context.actorOf(Props(new Actor {
      override def receive = {
        case Timeout =>
          // Inform user about timeout
          // Ask them to try again later.
          displayAlert(connectionTimeoutMessage)
          log.info("Timeout")

          // Stop this actor
          context.stop(self)
        case Acknowledge =>
          // Receive acknowledgement from receiver
          // We know that the message is sent out
          // successfully.
          //
          // Nothing need to be done
          println("Receive acknowledgement")

          // Stop this actor
          context.stop(self)
        case t => println(s"Anonymous actor receive unknown message: $t")
      }
    }))
  }
}
