import scalafx.event.ActionEvent
import scalafx.scene.control.{Label}
import scalafxml.core.macros.sfxml

@sfxml
class ListCellController(
  private val label: Label,
) {
  private var _item: String = null

  def item = _item
  def item_=(item: String) {
    _item = item
  }

}
