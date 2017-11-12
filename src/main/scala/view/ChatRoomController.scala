import scalafx.event.ActionEvent
import scalafx.scene.control.{Label, Button, TextArea}
import scalafx.scene.text.TextFlow
import scalafx.beans.property.StringProperty
import scalafxml.core.macros.sfxml

@sfxml
class ChatRoomController(
  private val username: Label,
  private val sendButton: Button,
  private val textArea: TextArea,
  private val textFlow: TextFlow
) {

  def handleSend(action: ActionEvent) {
    
  }
}
