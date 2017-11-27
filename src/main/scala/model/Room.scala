import akka.actor.ActorRef
import scala.collection.mutable.ArrayBuffer

sealed trait ChatRoomType
case object Personal extends ChatRoomType
case object Group extends ChatRoomType

sealed trait RoomAction
case object AddUsername extends RoomAction
case object RemoveUsername extends RoomAction

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

  // Use to generate a standardized and unique identifier for
  // Personal Room
  //
  // Given the input:
  // name1: "Kai Wern", name2: "Li Sheng"

  // It return:
  // => "Kai Wern:Li Sheng"
  def unique_identifier(name1: String, name2: String): String = {
    return Array(name1, name2).sorted.mkString(":")
  }
}
