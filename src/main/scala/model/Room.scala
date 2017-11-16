import akka.actor.ActorRef
import scala.collection.mutable.ArrayBuffer

sealed trait ChatRoomType
case object Personal extends ChatRoomType
case object Group extends ChatRoomType

trait Chattable {
  def key: String
  def chattableType: ChatRoomType
}

case class Room(
  var name: String, 
  val messages: ArrayBuffer[Room.Message],
  var users: List[ActorRef]) extends Chattable {

  def key = name
  def chattableType = Group
}

object Room {
  case class Message(from: String, value: String)

  def apply(roomEntries: Map[String, Room]): Seq[Room] = {
    roomEntries.values.toSeq
  }
}
