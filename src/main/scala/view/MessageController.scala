import scalafx.scene.control.{Label}
import scalafx.scene.layout.HBox
import scalafxml.core.macros.sfxml
import scalafx.geometry.Pos

@sfxml
class MessageController(
  private val hbox: HBox,
  private val messageLabel: Label
) {
//   private var _message: Option[String] = None

//   def message = _message
//   def message_=(message: String) {
//     _message = Some(message)
//     messageLabel.text = message
//   }
    def setMessage(message: String){
        messageLabel.text = message
    }
    def setAlign(position: Pos){
        hbox.alignment = position
    }
}
