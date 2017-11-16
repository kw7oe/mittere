import scalafx.event.ActionEvent
import scalafx.scene.input.{MouseEvent, MouseButton}
import scalafx.scene.control.{Label, MenuItem}
import scalafx.beans.property.StringProperty
import scalafxml.core.macros.sfxml

@sfxml
class ListCellController(
  private val label: Label,
  private val kickMenuItem: MenuItem,
  private val unreadNumber: Label
) {

  private var _chattable: Option[Chattable] = None

  def chattable = _chattable
  def chattable_=(chattable: Chattable) {
    _chattable = Some(chattable)
    label.text = chattable.key
  }

  def handleShowChat(action: MouseEvent) {
    import Client._
    if (action.button == MouseButton.Primary) {
      MyApp.clientActor ! RequestToChatWith(chattable.get)
    }
    hideUnread()
  }

  def handleShowUnread(){
    unreadNumber.text = (unreadNumber.text.value.toInt + 1).toString
    unreadNumber.opacity = 1
  }

  def hideUnread(){
    unreadNumber.opacity = 0
  }

  def kick(action: ActionEvent) {
  }

}
