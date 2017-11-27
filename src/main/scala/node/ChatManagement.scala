import akka.actor.{Actor, ActorLogging, ActorSelection, ActorRef, Props}
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.collection.immutable.{SortedMap, HashSet}

trait ChatManagement extends ActorLogging { this: Actor =>
  import Node._

  var superNodeActor: Option[ActorSelection]
  var username: Option[String]
  var usernameToClient: SortedMap[String, ActorRef]
  var clientToUsername: Map[ActorRef, String]
  var usernameToRoom: Map[String, Room]
  var roomNameToRoom: Map[String, Room]

  protected def chatManagement: Receive = {
    case RequestToChatWith(chatRoom) =>
      log.info(s"RequestToChatWith: $chatRoom")

      // Get the room according to the type
      val room = chatRoom.chatRoomType match {
        case Group => roomNameToRoom.get(chatRoom.identifier)
        case Personal => usernameToRoom.get(chatRoom.identifier)
      }

      room.foreach { r =>

        // Check if the user is in the `room.users`
        if (!r.users.contains(self)) {
          // If not, broadcast to other node that this node
          // is joining the room. Hence, have the other nodes
          // update their room data.
          superNodeActor.get ! SuperNode.UpdateRoom(r.identifier)
          usernameToClient.foreach { case (_, userActor) =>
            // Inform the actor that this particular node
            // has join the specified room.
            userActor ! JoinChatRoom(r.identifier, username.get)
          }
        }

        // Convert the users in the room into their respective
        // username.
        val users = usernameToClient.filter { case (name, ref) =>
          r.users.contains(ref)
        }.keySet

        // Display the chat room
        MyApp.displayActor ! Display.ShowChatRoom(r, users)
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

        // Get the username of the actor, to be used in display
        // error message if needed.

        val name = clientToUsername.get(actor).getOrElse("unknown")

        // Schedule the timeout after 2 second to the created
        // extra actor
        context.system.scheduler.scheduleOnce(3 second, extraActor, Timeout(name))
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

  private def connectionTimeoutMessage(name: String) : Tuple3[String, String, String]  = {
    return (
    "Connection Timeout",
    s"Failed to send the message to $name.",
    "Please try again later."
    )
  }

  // We create a temporary actor to handle the message passing
  // This approach is refer from Learning Akka, Chapter 3, section
  // "Designing with Tell"
  private def buildExtraActor: ActorRef = {
    return context.actorOf(Props(new Actor {
      override def receive = {
        case Timeout(name) =>
          // Inform user about timeout
          // Ask them to try again later.
          displayAlert(connectionTimeoutMessage(name))
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
