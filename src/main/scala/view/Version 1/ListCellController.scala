import scalafx.event.ActionEvent
import scalafx.scene.input.{MouseEvent, MouseButton}
import scalafx.scene.control.{Label, MenuItem}
import scalafx.beans.property.StringProperty
import scalafxml.core.macros.sfxml
import javafx.beans.property.SimpleIntegerProperty;

@sfxml
class ListCellController(
  private val label: Label,
  private val kickMenuItem: MenuItem,
  private val unreadNumber: Label
) {

  private var _chattable: Option[Chattable] = None
  private var _unread = 0

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
