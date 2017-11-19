import scalafx.scene.control.{Label}
import scalafx.scene.layout.HBox
import scalafxml.core.macros.sfxml
import scalafx.geometry.Pos

@sfxml
class RoomMessageController(
  private val nameLabel: Label,
  private val messageLabel: Label
) {

  def setMessage(message: String) {
    messageLabel.text = message
  }
  def setSender(name: String) {
    nameLabel.text = name
  }

}
