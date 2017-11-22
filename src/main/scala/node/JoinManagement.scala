import akka.actor.{Actor, ActorLogging, ActorSelection, ActorRef, DeadLetter, ReceiveTimeout}
import scala.collection.immutable.{HashSet, SortedMap}
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait JoinManagement extends ActorLogging { this: Actor =>
  import Node._

  var superNodeActor: Option[ActorSelection]
  var username: Option[String]
  var usernameToClient: SortedMap[String, ActorRef]
  var usernameToRoom: Map[String, Room]
  var roomNameToRoom: Map[String, Room]


  private val invalidAddressErrorMessage = (
    "Invalid Address",
    "Server and port combination provided cannot be connect.",
    "Please enter a different server and port combination."
  )
  private val invalidUsernameErrorMessage = (
    "Invalid Username",
    "Username has already been taken.",
    "Please enter a different username"
  )
  private val connectionTimeoutMessage = (
    "Connection Timeout",
    "The connection has timed out",
    "Please try again later."
  )

  protected def joinManagement: Receive = {
    case DeadLetter(message: SuperNode.Join, _, _) =>
      context.setReceiveTimeout(Duration.Undefined)
      log.info(s"Receive DeadLetter: $message")
      if (isJoinDeadLetter(message)) {
        displayAlert(invalidAddressErrorMessage)
      }
    case RequestToJoin(serverAddress, portNumber, name) =>
      log.info("RequestToJoin")
      superNodeActor = Some(MyApp.system.actorSelection(s"akka.tcp://chat@$serverAddress:$portNumber/user/super-node"))
      username = Some(name)
      superNodeActor.get ! SuperNode.Join(username.get)

      context.setReceiveTimeout(2 second)
    case InvalidUsername =>
      context.setReceiveTimeout(Duration.Undefined)
      log.info("Receive InvalidUsername")
      displayAlert(invalidUsernameErrorMessage)
    case Joined(users, rooms)  =>
      context.setReceiveTimeout(Duration.Undefined)
      // Receive online users and rooms info from Manager
      log.info("Joined")

      // Keep track of the info locally
      usernameToClient = users

      // Create One to One Room for each online users
      usernameToClient.foreach { case (name, ref) =>
        // Create a unique identifier for the room
        val identifier = Room.unique_identifier(name, username.get)
        val room = new Room(
          name = name,
          identifier = identifier,
          chatRoomType = Personal,
          messages = new ArrayBuffer[Room.Message](),
          users = HashSet(self, ref)
        )

        usernameToRoom += (identifier -> room)
      }
      roomNameToRoom = rooms

      // Initialize Display with the info received
      MyApp.displayActor ! Display.Initialize(usernameToRoom, roomNameToRoom, username.get)
    case ReceiveTimeout =>
      context.setReceiveTimeout(Duration.Undefined)
      displayAlert(connectionTimeoutMessage)
      log.info("Receive Timeout")
  }

  private def isJoinDeadLetter(message: SuperNode.Join): Boolean = {
    return message == SuperNode.Join(username.get)
  }

  private def displayAlert(messages: Tuple3[String, String, String]) {
    MyApp.displayActor ! Display.ShowAlert(messages)
  }
}