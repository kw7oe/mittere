import akka.actor.ActorRef

sealed trait ChatRoomType
case object Personal extends ChatRoomType
case object Group extends ChatRoomType

case class Room(
  var name: String, 
  roomId: String, 
  actorRef: ActorRef) 


