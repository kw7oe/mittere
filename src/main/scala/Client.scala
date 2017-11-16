import akka.actor.{Actor, ActorLogging, ActorSelection, ActorRef}
import scala.collection.mutable.ArrayBuffer
import scala.collection.immutable.HashMap

object Client {
  // Before Join
  case class RequestToJoin(serverAddress: String,
                           portNumber: String,
                           username: String)
  case class Joined(clients: Map[String, ActorRef],
                    rooms:  Map[String, Room])

  // Joined
  case class NewUser(name: String, actorRef: ActorRef)
  case class RequestToChatWith(chattable: Chattable)
  case class RequestToCreateChatRoom(chatRoom: Room)
  case class NewChatRoom(room: Room)

  // Chatting
  case class Typing(roomType: ChatRoomType, key: String)
  case class ReceiveShowTyping(roomId: String, username: String)
  case class RequestToSendMessage(chattable: Chattable, msg: String)
  case class ReceiveMessage(chattable: Chattable, key: String, message: Room.Message)
}

class Client extends Actor with ActorLogging {
  import Client._

  // The ActorSelection of host
  var serverActor: Option[ActorSelection] = None
  // Current client username
  var username: Option[String] = None
  // { "username" -> ActorRef }
  var usernameToClient: Map[String,ActorRef] = new HashMap()
  // The message history with other user
  // { "username" -> [Mesage, Message] }
  var usernameToMessages: Map[String, ArrayBuffer[Room.Message]] = new HashMap()
  // The info of chatRoom
  // { "roomName" -> Room }
  var roomNameToRoom: Map[String, Room] = new HashMap()

  override def preStart(): Unit = {
    context.system.eventStream.subscribe(self, classOf[akka.remote.DisassociatedEvent])
  }

  def receive = {
    // Request to join server
    case RequestToJoin(serverAddress, portNumber, name) =>
      log.info("RequestToJoin")
      serverActor = Some(MyApp.system.actorSelection(s"akka.tcp://chat@$serverAddress:$portNumber/user/server"))
      username = Some(name)
      serverActor.get ! Server.Join(username.get)
    // Get online users from server
    case Joined(users, rooms)  =>
      log.info("Joined")
      usernameToClient = users
      roomNameToRoom = rooms
      MyApp.displayActor ! Display.Initialize(usernameToClient, roomNameToRoom)
      context.become(joined)
    case _ => log.info("Received unknown message")
  }

  def joined: Receive = {
    case akka.remote.DisassociatedEvent(local, remote, _) =>
      val userInfo = usernameToClient.find { case(_, x) =>
        x.path.address == remote
      }

      userInfo match {
        case Some(value) =>
          usernameToClient -= value._1
          val user = User(value._1, value._2)
          MyApp.displayActor ! Display.RemoveJoin(user)
        case None => // Do Nothing
      }
    // When new user connect to the same server
    case NewUser(name, ref) =>
      usernameToClient += (name -> ref)
      val user = User(name, ref)
      MyApp.displayActor ! Display.ShowJoin(user)
    // When user click the username on the user lists
    case RequestToChatWith(chattable) =>
      log.info(s"RequestToChatWith: $chattable")

      chattable.chattableType match {
        case Personal =>
          if (!usernameToMessages.contains(chattable.key)) {
            usernameToMessages += (chattable.key -> new ArrayBuffer[Room.Message]())
          }
          val messages = usernameToMessages.get(chattable.key)
          MyApp.displayActor ! Display.ShowChatRoom(chattable, messages.get)
        case Group =>
          // WARNING
          val room = roomNameToRoom.get(chattable.key).get
          MyApp.displayActor ! Display.ShowChatRoom(chattable, room.messages)
      }

    // Create a chat room and broadcast to other user
    case RequestToCreateChatRoom(tempRoom) =>
      log.info(s"RequestToCreateChatRoom: $tempRoom")

      // Initialize Room and add into roomNameToRoom
      if (!roomNameToRoom.contains(tempRoom.name)) {
        tempRoom.users = List(self)
        roomNameToRoom += (tempRoom.name -> tempRoom)
      }

      val room = roomNameToRoom.get(tempRoom.name)

      // Inform the host
      serverActor.get ! Server.ChatRoomCreated(room.get)

      // Broadcast the created room to other users
      usernameToClient.foreach { case (_, userActor) =>
        userActor ! Client.NewChatRoom(room.get)
      }
    case NewChatRoom(room) =>
      log.info(s"NewChatRoom: $room")
      roomNameToRoom += (room.name -> room)
      MyApp.displayActor ! Display.ShowNewChatRoom(room)
    case Typing(roomType, key) =>
      log.info(s"Typing: $roomType, $key")
      // val actor = roomType match {
      //   case Group => chatRoomActors.get(key)
      //   case Personal => usernameToClient.get(key)
      // }
      // actor.get ! ChatRoom.ShowTyping(username.get)
    case ReceiveShowTyping(roomId, username) =>
      log.info(s"ReceiveShowTyping: $roomId, $username")
      // if (username != this.username.get) {
      //   MyApp.displayActor ! Display.ShowTyping(roomId, username)
      // }
    // Request to send message
    case RequestToSendMessage(chattable, msg) =>
      log.info(s"RequestToSendMessage: $chattable, $msg")

      chattable.chattableType match {
        case Group =>
          val room = roomNameToRoom.get(chattable.key).get
          room.users.foreach { actor =>
            val message = new Room.Message(username.get, msg)
            val key = chattable.key
            actor ! ReceiveMessage(chattable, key, message)
          }
        case Personal =>
          val actor = usernameToClient.get(chattable.key)
          val message = new Room.Message(username.get, msg)
          val key = username.get
          actor.get ! ReceiveMessage(chattable, key, message)
          if (key != chattable.key) {
            self ! ReceiveMessage(chattable, chattable.key, message)
          }
      }
    case ReceiveMessage(chattable, key, msg) =>
      log.info(s"ReceiveMessage: $chattable, $msg")

      chattable.chattableType match {
        case Group =>
          // Extremely DANGER
          roomNameToRoom.get(key).get.messages += msg
        case Personal =>
          if (!usernameToMessages.contains(key)) {
            usernameToMessages += (key -> new ArrayBuffer[Room.Message]())
          }
          usernameToMessages.get(key).get += msg
      }
      MyApp.displayActor ! Display.AddMessage(chattable, key, msg)
    case _ => log.info("Received unknown message")
  }

}
