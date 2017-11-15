import scalafx.event.ActionEvent
import scalafx.scene.input.{KeyEvent, KeyCode}
import scalafx.scene.control.{Label, Button, TextArea, ListView}
import scalafx.scene.text.Text
import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.collection.mutable.ArrayBuffer
import scalafx.application.Platform
import scalafxml.core.macros.sfxml

@sfxml
class ChatRoomController(
  private val usernameLabel: Label,
  private val typingLabel: Label,
  private val textArea: TextArea,
  private val messageList: ListView[String]
) {

  var messages: ObservableBuffer[String] = new ObservableBuffer[String]()
  private var _user: Option[User] = None

  // If roomId is None, it means that is a personal chat.
  // If roomId has value, it is a group chat.
  var roomId: Option[String] = None
  messageList.items = messages

  def messages_=(messages: ArrayBuffer[ChatRoom.Message]) {
    this.messages = ObservableBuffer(messages.map { m => convertMessageToString(m) })
    messageList.items = this.messages
  }

  def user = _user
  def user_=(user: User) {
    _user = Some(user)
    usernameLabel.text = _user.get.username
  }

  def username: Option[String] = {
    val u = _user.getOrElse {
      return None
    }
    return Some(u.username)
  }

  def handleSend(messages: String) {
    import Client._
    roomId match {
      case Some(id) =>  MyApp.clientActor ! RequestToSendMessage(Group, id, textArea.text.value)
      case None => MyApp.clientActor ! RequestToSendMessage(Personal, user.get.username, textArea.text.value)
    }
  }

  def handleTyped(action: KeyEvent) {
    import Client._
    if(action.code == KeyCode.ENTER && action.shiftDown){
      println("Shift + Enter")
      textArea.text.value = textArea.text.value + "\n"
    }else if (action.code == KeyCode.ENTER){
      action.consume()
      handleSend(textArea.text.value)
      textArea.text.value = ""
    } else {
      // roomId match {
      //   case Some(id) =>  MyApp.clientActor ! Typing(Group, roomId.get)
      //   case None => MyApp.clientActor ! Typing(Personal, user.username)
      // }
    }
  }

  def showTyping(username: String) {
    // typingLabel.text = s"$username is typing..."
    // val task = new Runnable { 
    //   def run() { 
    //     Platform.runLater {
    //       typingLabel.text = ""
    //     }
    //   } 
    // }
    // MyApp.scheduler.scheduleOnce(2 second, task)
  }

  def addMessage(message: ChatRoom.Message) {
    messages += convertMessageToString(message)
  } 

  private def convertMessageToString(message: ChatRoom.Message): String = {
    return s"${message.from}: ${message.value}"
  }
}
