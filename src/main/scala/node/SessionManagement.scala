import akka.actor.{Actor, ActorLogging, ActorSelection, ActorRef}
import scala.collection.immutable.{HashSet, SortedMap}
import scala.collection.mutable.ArrayBuffer

trait SessionManagement extends ActorLogging { this: Actor =>
  import Node._

  var superNodeActor: Option[ActorSelection]
  var username: Option[String]
  var clientToUsername: Map[ActorRef, String]
  var usernameToClient: SortedMap[String,ActorRef]
  var usernameToRoom: Map[String, Room]
  var roomNameToRoom: Map[String, Room]

  private val invalidRoomNameErrorMessage = (
    "Invalid Room Name",
    "Room name has already been taken.",
    "Please enter a different room name."
  )

  protected def sessionManagement: Receive = {
    case akka.remote.DisassociatedEvent(local, remote, _) =>
      log.info("Node receive DisassociatedEvent")

      // When Remote User disconnected
      // Check which user disconnected
      val userInfo = usernameToClient.find { case(_, x) =>
       x.path.address == remote
      }

      // If the user exists in our record
      userInfo.foreach { value =>
        val disconnectedUsername = value._1
        val userRef = value._2

        // Remove tracking
        usernameToClient -= disconnectedUsername

        usernameToClient.foreach { case(_, ref) =>
          ref ! Node.RemoveUser(disconnectedUsername, userRef)
        }
      }

      //  Check if Supernode disconnected
      if (superNodeActor.get.anchorPath.address == remote) {

        // Inform another node to become Supernode
        val firstKey = usernameToClient.firstKey
        if (firstKey == username.get) {
          MyApp.superNodeActor ! SuperNode.BecomeSuperNode(usernameToClient, roomNameToRoom)
        }
      }
    case NewSuperNode =>
      log.info("Receive NewSuperNode")
      superNodeActor = Some(MyApp.system.actorSelection(sender().path))

      displayAlert(newSuperNodeMessage(sender().path.address.toString))
    case NewUser(name, ref) =>
      // Keep track of new user
      usernameToClient += (name -> ref)
      clientToUsername += (ref -> name)

      // Create Personal Room with the User
      val identifier = Room.unique_identifier(name, username.get)
      val room = new Room(
        name = name,
        identifier = identifier,
        chatRoomType = Personal,
        messages = new ArrayBuffer[Room.Message](),
        users = HashSet(self, ref)
      )
      usernameToRoom += (identifier -> room)

      // Inform Display to show new user
      MyApp.displayActor ! Display.ShowJoin(room)
    case RemoveUser(disconnectedUsername, ref) =>
      // Remove from tracking
      usernameToClient -= disconnectedUsername
      clientToUsername -= ref

      val identifier = Room.unique_identifier(disconnectedUsername, username.get)
      val room = usernameToRoom.get(identifier)
      usernameToRoom -= identifier

      room.foreach { r =>
        MyApp.displayActor ! Display.RemoveJoin(r)
      }

      // Remove from each room
      roomNameToRoom.foreach { case (name, room) =>
        if (room.users.contains(ref)) {
          log.info("Contain userRef")
          room.users -= ref
          MyApp.displayActor ! Display.RefreshRoom(room, disconnectedUsername, RemoveUsername)
        }
      }

    case RequestToCreateChatRoom(roomName) =>
      log.info(s"RequestToCreateChatRoom: $roomName")

      // Check if the room name already used
      if (roomNameToRoom.contains(roomName)) {
        displayAlert(invalidRoomNameErrorMessage)
      } else {
        val room = new Room(
          name = roomName,
          identifier = roomName,
          chatRoomType = Group,
          messages = new ArrayBuffer[Room.Message](),
          users = HashSet(self)
        )

        // Keep track of the info of each room
        roomNameToRoom += (roomName -> room)

        // Get the room from our record
        val createdRoom = roomNameToRoom.get(roomName).get

        // Inform the host about the new room
        superNodeActor.get ! SuperNode.ChatRoomCreated(createdRoom)

        // Broadcast the created room to other users
        usernameToClient.foreach { case (_, userActor) =>
          userActor ! NewChatRoom(createdRoom)
        }
      }
    case JoinChatRoom(identifier, name) =>
      log.info(s"JoinChatRoom: $identifier, $name")
      val room = roomNameToRoom.get(identifier)
      room.foreach { r =>
        r.users += sender()
        MyApp.displayActor ! Display.RefreshRoom(r, name, AddUsername)
      }
    case NewChatRoom(room) =>
      // Received broadcast from other node
      // about new room
      log.info(s"NewChatRoom: $room")

      // Keep track of the room
      roomNameToRoom += (room.name -> room)

      // Inform Display to show new room
      MyApp.displayActor ! Display.ShowNewChatRoom(room)
  }

  // To create alert message when there is a new supernode
  private def newSuperNodeMessage(path: String): Tuple3[String, String, String] = {

    val res = path match {
      case "akka://chat" => "You're now the new supernode."
      case other => s"The new supernode is now at $other"
    }

    return (
      "New Supernode",
      "The previous supernode has been disconnected.",
      res
    )
  }

  private def displayAlert(messages: Tuple3[String, String, String]) {
    MyApp.displayActor ! Display.ShowAlert(messages)
  }

}