import scalafx.event.ActionEvent
import scalafx.scene.layout.{HBox}
import scalafx.scene.input.{MouseEvent, MouseButton}
import scalafx.scene.control.{Label, MenuItem}
import scalafx.beans.property.StringProperty
import scalafxml.core.macros.sfxml
import scalafx.scene.shape.Circle
import scalafx.Includes._

@sfxml
class ListCellController(
  private val hbox: HBox,
  private val onlineStatusCircle: Circle,
  private val name: Label,
  private val unreadNumber: Label
) {
  private var _room: Option[Room] = None

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

  def showUnread(number: Int){
    if(number > 0) {
      unreadNumber.text = number.toString()
      unreadNumber.opacity = 1
    } else {
      hideUnread()
    }
  }

  def hideCircle(){
    hbox.getChildren().remove(onlineStatusCircle)
  }

  def hideUnread(){
    unreadNumber.text = 0.toString()
    unreadNumber.opacity = 0
  }
}
