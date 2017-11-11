import scalafx.event.ActionEvent
import scalafx.scene.control.{Label, MenuItem}
import scalafx.beans.property.StringProperty
import scalafxml.core.macros.sfxml

@sfxml
class ListCellController(
  private val label: Label,
  private val sendMessageMenuItem: MenuItem,
  private val kickMenuItem: MenuItem
) {
  private var _item: String = null

  def item = _item
  def item_=(item: String) {
    _item = item
    label.text = item
  }

  def sendMessage(action: ActionEvent) {
  }

  def kick(action: ActionEvent) {
  }

}
