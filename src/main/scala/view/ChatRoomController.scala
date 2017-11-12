import scalafx.event.ActionEvent
import scalafx.scene.control.{Label, Button, TextArea, ListView}
import scalafx.scene.text.Text
import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scalafxml.core.macros.sfxml

@sfxml
class ChatRoomController(
  private val username: Label,
  private val sendButton: Button,
  private val textArea: TextArea,
  private val messageList: ListView[String]
) {

  var messages: ObservableBuffer[String] = new ObservableBuffer[String]()
  messageList.setItems(messages)

  def handleSend(action: ActionEvent) {
    import Client._
    MyApp.clientActor ! RequestToSendMessage(textArea.text.value)
  }

  def addMessage(username: String, message: String) {
    messages += s"$username: $message"
  } 
}
