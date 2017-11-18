import akka.actor.{Actor, ActorLogging, ActorSelection, ActorRef, DeadLetter}
import scala.collection.immutable.HashSet
import scala.collection.mutable.ArrayBuffer

trait JoinManagement extends ActorLogging { this: Actor =>
  import Node._

  var serverActor: Option[ActorSelection]
  var username: Option[String]
  var usernameToClient: Map[String, ActorRef]
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

  protected def joinManagement: Receive = {
    case d: DeadLetter =>
      log.info(s"Receive DeadLetter: $d")
      if (isJoinDeadLetter(d)) { 
        displayAlert(invalidAddressErrorMessage)
      }   
    case RequestToJoin(serverAddress, portNumber, name) =>
      log.info("RequestToJoin")
      serverActor = Some(MyApp.system.actorSelection(s"akka.tcp://chat@$serverAddress:$portNumber/user/server"))
      username = Some(name)
      serverActor.get ! Server.Join(username.get)  
    case InvalidUsername =>
      log.info("Receive InvalidUsername")
      displayAlert(invalidUsernameErrorMessage)
    case Joined(users, rooms)  =>
      // Receive online users and rooms info from Manager
      log.info("Joined")

      // Keep track of the info locally
      usernameToClient = users

      // Create One to One Room for each online users
      usernameToClient.foreach { case (name, ref) => 
        // Create a unique identifier for the room
        val identifier = Array(name, username.get).sorted.mkString(":")
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
      MyApp.displayActor ! Display.Initialize(usernameToRoom, roomNameToRoom)
  }
  
  private def isJoinDeadLetter(deadLetter: DeadLetter): Boolean = {
    return deadLetter.message == Server.Join(username.get)
  }

  private def displayAlert(messages: Tuple3[String, String, String]) {
    MyApp.displayActor ! Display.ShowAlert(messages)
  }
}