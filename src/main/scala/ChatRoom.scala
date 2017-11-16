// import akka.actor.{Actor, Props, ActorLogging, ActorRef, ActorSelection}
// import scala.collection.mutable.ArrayBuffer
// import scala.collection.immutable.HashSet

// object ChatRoom {
//   def props(roomId: String, usersPath: Array[String]): Props = 
//     Props(new ChatRoom(roomId, usersPath))

//   case class Join(user: User)
//   case class Invite(user: User, actor: ActorRef)
//   case class ShowTyping(username: String)
//   case class Message(from: String, value: String)
// }

// class ChatRoom(
//   roomId: String,
//   usersPath: Array[String]
// ) extends Actor with ActorLogging {
//   import ChatRoom._

//   var userSelections: Set[ActorSelection] = new HashSet()
//   var messages: ArrayBuffer[Message] = new ArrayBuffer[Message]()

//   override def preStart() = {
//     log.info(s"Chat Room $roomId started")
//     for (i <- 0 until usersPath.size) {
//       val userActor = MyApp.system.actorSelection(usersPath(i))
//       userSelections += userActor
//       // The operation below is to convert 0 to 1, 1 to 0
//       // The reason is to identify the actorPath to store
//       // in the client side, indicating which user chat 
//       // are equipped with which roomId.
//       val index = (i - 1) * -1 
//       log.info(s"$index")
//       val userPath = usersPath(index)
//       userActor ! Client.ChatRoomCreated(userPath, roomId)
//     }
//   }

//   override def postStop() = log.info(s"Chat Room $roomId stopped")

//   override def receive = {
//     case Invite(user, actor) => 
//       actor ! Client.JoinChatRoom(user, roomId, messages)
//     case Join(user) =>
//       sender() ! Client.JoinChatRoom(user, roomId, messages)
//     case ShowTyping(username) =>
//       userSelections.foreach { actorSelection =>
//         actorSelection ! Client.ReceiveShowTyping(roomId, username)
//       }
//     case msg @ Message(from, message) => 
//       messages += msg
//       log.info(s"Received $message from $from")
//       userSelections.foreach { actorSelection => 
//         actorSelection ! Client.ReceiveMessage(Group, roomId, msg)
//       }
//     case _ => log.info("Receive unknown message")
//   }
// }

