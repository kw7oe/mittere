import scalafx.scene.control.{Label}
import scalafx.scene.layout.HBox
import scalafxml.core.macros.sfxml
import scalafx.geometry.Pos

@sfxml
class MessageController(
  private val hbox: HBox,
  private val messageLabel: Label
) {

  def setMessage(message: String) {
    messageLabel.text = message
  }

  def setAlign(position: Pos){
    if (position == Pos.CenterRight) {
      messageLabel.setStyle("-fx-text-fill:#FAFAFC;-fx-background-color:#797DA5;-fx-background-radius:6px;")
    } else {
      messageLabel.setStyle("-fx-text-fill:#000;-fx-background-color:#FAFAFC;-fx-background-radius:6px;")
    }
    hbox.alignment = position
  }

}
