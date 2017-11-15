import akka.actor.ActorRef

sealed trait ChatRoomType
case object Personal extends ChatRoomType
case object Group extends ChatRoomType
case class Room(val name: String, val roomId: String, val actorRef: ActorRef) 


