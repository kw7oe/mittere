import akka.actor.{Actor, ActorLogging, ActorSelection, ActorRef}
import scala.collection.immutable.HashSet

trait SessionManagement extends ActorLogging { this: Actor =>
  import Node._

  var serverActor: Option[ActorSelection]
  var username: Option[String]
  var usernameToClient: Map[String,ActorRef]
  var roomNameToRoom: Map[String, Room]


  private val invalidRoomNameErrorMessage = (
    "Invalid Room Name", 
    "Room name has already been taken.", 
    "Please enter a different room name."
  )

  protected def sessionManagement: Receive = {
    case akka.remote.DisassociatedEvent(local, remote, _) =>
      // When Remote User disconnected
      // Check which user disconnected
      val userInfo = usernameToClient.find { case(_, x) =>
       x.path.address == remote
      }

      // If the user exists in our record
      userInfo.foreach { value => 
        // Remove tracking
        usernameToClient -= value._1

        // Inform Display to remove user
        val user = User(value._1, value._2)
        MyApp.displayActor ! Display.RemoveJoin(user)
      }
    case NewUser(name, ref) =>
      // Keep track of new user
      usernameToClient += (name -> ref)
      val user = User(name, ref)

      // Inform Display to show new user
      MyApp.displayActor ! Display.ShowJoin(user)
    case RequestToCreateChatRoom(tempRoom) =>
      log.info(s"RequestToCreateChatRoom: $tempRoom")

      // Check if the room name already used
      if (roomNameToRoom.contains(tempRoom.name)) {
        displayAlert(invalidRoomNameErrorMessage)
      } else {

        // Add creator to the room users
        tempRoom.users = HashSet(self)
        // Keep track of the info of each room
        roomNameToRoom += (tempRoom.name -> tempRoom)

        // Get the room from our record
        val room = roomNameToRoom.get(tempRoom.name)

        // Inform the host about the new room
        serverActor.get ! Server.ChatRoomCreated(room.get)

        // Broadcast the created room to other users
        usernameToClient.foreach { case (_, userActor) =>
          userActor ! NewChatRoom(room.get)
        }
      }     
    case JoinChatRoom(key) =>
      log.info(s"JoinChatRoom: $key")
      val room = roomNameToRoom.get(key)
      room.foreach { r => r.users += sender() }
    case NewChatRoom(room) =>
      // Received broadcast from other node 
      // about new room
      log.info(s"NewChatRoom: $room")

      // Keep track of the room
      roomNameToRoom += (room.name -> room)

      // Inform Display to show new room
      MyApp.displayActor ! Display.ShowNewChatRoom(room)
  }

  private def displayAlert(messages: Tuple3[String, String, String]) {
    MyApp.displayActor ! Display.ShowAlert(messages)
  }
  
}