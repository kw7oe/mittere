import scalafx.event.ActionEvent
import scalafx.scene.control.{Label, Button, TextArea, ListView}
import scalafx.scene.text.Text
import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scala.collection.mutable.ArrayBuffer
import scalafxml.core.macros.sfxml

@sfxml
class ChatRoomController(
  private val username: Label,
  private val sendButton: Button,
  private val textArea: TextArea,
  private val messageList: ListView[String]
) {

  var messages: ObservableBuffer[String] = new ObservableBuffer[String]()
  private var _user: User = null
  var roomId: String = null
  messageList.items = messages

  def messages_=(messages: ArrayBuffer[ChatRoom.Message]) {
    this.messages.appendAll(messages.map { m => s"${m.from}: ${m.value}" })
  }

  def user = _user
  def user_=(user: User) {
    _user = user
    username.text = _user.username
  }

  def handleSend(action: ActionEvent) {
    import Client._
    MyApp.clientActor ! RequestToSendMessage(roomId, textArea.text.value)
  }

  def addMessage(username: String, message: String) {
    messages += s"$username: $message"
  } 
}
