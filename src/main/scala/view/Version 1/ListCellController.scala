import scalafx.event.ActionEvent
import scalafx.scene.input.{MouseEvent, MouseButton}
import scalafx.scene.control.{Label, MenuItem}
import scalafx.beans.property.StringProperty
import scalafxml.core.macros.sfxml
import javafx.beans.property.SimpleIntegerProperty;

@sfxml
class ListCellController(
  private val name: Label,
  private val kickMenuItem: MenuItem,
  private val unreadNumber: Label
) {

  private var _room: Option[Room] = None
  private var _unread = 0

  def room = _room
  def room_=(room: Room) {
    _room = Some(room)
    name.text = room.name
  }
  def handleShowChat(action: MouseEvent) {
    import Node._
    if (action.button == MouseButton.Primary) {
      MyApp.clientActor ! RequestToChatWith(room.get)
    }
    hideUnread()
  }
  def unread = _unread
  def unread_=(number: Int){
    if(number > 0) {
      _unread = number
      unreadNumber.text = _unread.toString()
      unreadNumber.opacity = 1
    } else {
      hideUnread()
    }
  }

  def hideUnread(){
    _unread = 0
    unreadNumber.text = _unread.toString()
    unreadNumber.opacity = 0
  }

  def kick(action: ActionEvent) {
  }

}
