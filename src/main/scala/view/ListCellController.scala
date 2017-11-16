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
  }

  def kick(action: ActionEvent) {
  }

}
