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
  private var _room: Option[Room] = None
  final var shouldListenToTyping = true
  messageList.items = messages

  def messages_=(messages: ArrayBuffer[Room.Message]) {
    this.messages = ObservableBuffer(messages.map { m => convertMessageToString(m) })
    messageList.items = this.messages
  }

  def room = _room
  def room_=(room: Room) {
    _room = Some(room)
    usernameLabel.text = _room.get.name
  }

  // Reinitialize the state of the chat room
  def initialize(room: Room, messages: ArrayBuffer[Room.Message]) {
    this.room = room
    this.messages = messages
    this.typingLabel.text = ""
  }

  def handleSend(messages: String) {
    import Node._
    room match {
      case Some(c) => 
        MyApp.clientActor ! RequestToSendMessage(c, textArea.text.value)
      case None => // Do Nothing
    }
  }

  def handleTyped(action: KeyEvent) {
    import Node._

    if (action.code == KeyCode.ENTER && action.shiftDown) {
      textArea.appendText("\n")
    } else if (action.code == KeyCode.ENTER) {
      action.consume()
      handleSend(textArea.text.value)
      textArea.text.value = ""
    }

    if (shouldListenToTyping) {
      // Should let it crash if room is empty
      // As it should be technically impossible to 
      // have access to ChatRoomController without
      // room
      MyApp.clientActor ! Typing(room.get)      
      shouldListenToTyping = false
      val task = new Runnable { 
        def run() { 
          shouldListenToTyping = true
        } 
      }
      MyApp.scheduler.scheduleOnce(5 second, task)
    }

  }

  def showStatus(value: String) {
    typingLabel.text = value
    val task = new Runnable { 
      def run() { 
        Platform.runLater {
          typingLabel.text = ""
        }
      } 
    }
    MyApp.scheduler.scheduleOnce(5 second, task)
  }

  def addMessage(message: Room.Message) {
    messages += convertMessageToString(message)
  } 

  def handleFocus(){
    //when focused remove unread
  }

  private def convertMessageToString(message: Room.Message): String = {
    return s"${message.from}: ${message.value}"
  }
}
