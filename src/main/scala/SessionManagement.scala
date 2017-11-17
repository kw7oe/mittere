import akka.actor.{Actor, ActorLogging, ActorSelection, ActorRef, DeadLetter}
import scala.collection.mutable.ArrayBuffer
import scala.collection.immutable.{HashMap, HashSet}

trait SessionManagement extends ActorLogging { this: Actor =>
  import Node._

  var serverActor: Option[ActorSelection]
  var username: Option[String]
  var usernameToClient: Map[String,ActorRef]
  var roomNameToRoom: Map[String, Room]

  val invalidAddressErrorMessage = (
    "Invalid Address",
    "Server and port combination provided cannot be connect.",
    "Please enter a different server and port combination."
  )

  val invalidUsernameErrorMessage = (
    "Invalid Username", 
    "Username has already been taken.", 
    "Please enter a different username"
  )

  val invalidRoomNameErrorMessage = (
    "Invalid Room Name", 
    "Room name has already been taken.", 
    "Please enter a different room name."
  )

  protected def sessionManagement: Receive = {
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
      log.info("Joined")
      usernameToClient = users
      roomNameToRoom = rooms
      MyApp.displayActor ! Display.Initialize(usernameToClient, roomNameToRoom)
    case NewUser(name, ref) =>
      usernameToClient += (name -> ref)
      val user = User(name, ref)
      MyApp.displayActor ! Display.ShowJoin(user)
    case RequestToCreateChatRoom(tempRoom) =>
      log.info(s"RequestToCreateChatRoom: $tempRoom")

      if (roomNameToRoom.contains(tempRoom.name)) {
        displayAlert(invalidRoomNameErrorMessage)
      } else {
        // Initialize Room and add into roomNameToRoom
        if (!roomNameToRoom.contains(tempRoom.name)) {
          tempRoom.users = HashSet(self)
          roomNameToRoom += (tempRoom.name -> tempRoom)
        }

        val room = roomNameToRoom.get(tempRoom.name)

        // Inform the host
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
      log.info(s"NewChatRoom: $room")
      roomNameToRoom += (room.name -> room)
      MyApp.displayActor ! Display.ShowNewChatRoom(room)
  }

  private def isJoinDeadLetter(deadLetter: DeadLetter): Boolean = {
    return deadLetter.message == Server.Join(username.get)
  }

  private def displayAlert(messages: Tuple3[String, String, String]) {
    MyApp.displayActor ! Display.ShowAlert(messages)
  }
  
}