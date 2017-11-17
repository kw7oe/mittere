import akka.actor.ActorRef
import scala.collection.mutable.ArrayBuffer

sealed trait ChatRoomType
case object Personal extends ChatRoomType
case object Group extends ChatRoomType

case class Room(
  var name: String, 
  var identifier: String,
  val chatRoomType: ChatRoomType,
  val messages: ArrayBuffer[Room.Message],
  var users: Set[ActorRef]) {
  private var _unreadNumber = 0

  def unreadNumber = _unreadNumber
  def unreadNumber_=(number: Int) {
    _unreadNumber = number
  }
}

object Room {
  case class Message(from: String, value: String)
  
  def apply(roomEntries: Map[String, Room]): Seq[Room] = {
    roomEntries.values.toSeq
  }
}
