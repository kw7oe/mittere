import scalafx.event.ActionEvent
import scalafx.scene.input.{MouseEvent, MouseButton}
import scalafx.scene.control.{Label, MenuItem}
import scalafx.beans.property.StringProperty
import scalafxml.core.macros.sfxml

@sfxml
class ListCellController(
  private val label: Label,
  private val kickMenuItem: MenuItem
) {
  private var _user: User = null
  private var _room: Room = null

  def user = _user
  def user_=(user: User) {
    _user = user
    label.text = user.username
  }

  def room = _room
  def room_=(room: Room) {
    _room = room
    label.text = room.name
  }

  def handleShowChat(action: MouseEvent) {
    import Client._
    if (action.button == MouseButton.Primary) {
      MyApp.clientActor ! RequestToChatWith(user)
    }
  }

  def kick(action: ActionEvent) {
  }

}
