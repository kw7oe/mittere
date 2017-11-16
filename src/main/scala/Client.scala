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
  case class RequestToChatWith(user: User)
  case class RequestToCreateChatRoom(chatRoom: Room)
  case class NewChatRoom(room: Room)

  // DEPRECATED
  case class ChatRoomCreated(userPath: String, roomId: String)
  case class JoinChatRoom(user: User, roomId: String, messages: ArrayBuffer[ChatRoom.Message])

  // Chatting
  case class Typing(roomType: ChatRoomType, key: String)
  case class ReceiveShowTyping(roomId: String, username: String)
  case class RequestToSendMessage(roomType: ChatRoomType, key: String, msg: String)
  case class ReceiveMessage(roomType: ChatRoomType, key: String, message: ChatRoom.Message)
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
  var usernameToMessages: Map[String, ArrayBuffer[ChatRoom.Message]] = new HashMap()
  // The info of chatRoom
  // { "roomName" -> Room }
  var roomNameToRoom: Map[String, Room] = new HashMap()

  var chatRoomActors: Map[String, ActorRef] = new HashMap()
  var actorPathToChatRoomActors: Map[String, ActorRef] = new HashMap()

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
    case RequestToChatWith(user) => 
      log.info(s"RequestToChatWith: $user")
      if (!usernameToMessages.contains(user.username)) {
        usernameToMessages += (user.username -> new ArrayBuffer[ChatRoom.Message]())
      }
      val messages = usernameToMessages.get(user.username)
      MyApp.displayActor ! Display.ShowChatRoom(user, None, messages.get)
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
    // Get notified when chat room is created
    case ChatRoomCreated(userPath, roomId) =>
      log.info(s"ChatRoomCreated with $roomId")
      // actorPathToChatRoomActors += (userPath -> sender())
      // chatRoomActors += (roomId -> sender())
    // Get notified to show chat room
    case JoinChatRoom(user, roomId, messages) =>
      log.info(s"Join chatRoom $user - $roomId")
      // MyApp.displayActor ! Display.ShowChatRoom(user, roomId, messages)
    case Typing(roomType, key) =>
      log.info(s"Typing: $roomType, $key")
      val actor = roomType match {
        case Group => chatRoomActors.get(key)
        case Personal => usernameToClient.get(key)
      }
      actor.get ! ChatRoom.ShowTyping(username.get)
    case ReceiveShowTyping(roomId, username) =>
      log.info(s"ReceiveShowTyping: $roomId, $username")
      // if (username != this.username.get) {        
      //   MyApp.displayActor ! Display.ShowTyping(roomId, username)
      // }
    // Request to send message
    case RequestToSendMessage(roomType, key, msg) =>
      log.info(s"RequestToSendMessage: $roomType, $key, $msg")
      roomType match {
        case Group => 
          val actor = chatRoomActors.get(key)
          actor.get ! ChatRoom.Message(username.get, msg)
        case Personal => 
          val actor = usernameToClient.get(key)
          val message = new ChatRoom.Message(username.get, msg)
          actor.get ! ReceiveMessage(roomType, username.get, message)
          if (key != username.get) {
            self ! ReceiveMessage(roomType, key, message)            
          }
      }
    case ReceiveMessage(roomType, key, msg) =>
      log.info(s"ReceiveMessage: $roomType, $key, $msg")
      if (roomType == Personal) {
        // Track Messages Here
        if (!usernameToMessages.contains(key)) {
          usernameToMessages += (key -> new ArrayBuffer[ChatRoom.Message]())
        }
        usernameToMessages.get(key).get += msg
      }
      MyApp.displayActor ! Display.AddMessage(roomType, key, msg)
    case _ => log.info("Received unknown message")
  }

}
