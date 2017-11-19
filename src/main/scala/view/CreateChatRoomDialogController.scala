import scalafx.scene.control.TextField
import scalafx.stage.Stage
import scalafxml.core.macros.sfxml
import scalafx.event.ActionEvent
import scalafx.scene.input.{KeyEvent, KeyCode}
import scalafx.Includes._

@sfxml
class CreateChatRoomDialogController(private val roomNameField: TextField) {

  var dialogStage: Stage = null
  private var _roomName: String = null

  def roomName = _roomName
  def roomName_=(name: String) {
    _roomName = name
  }

  val roomNameBlankErrorMessage = (
    "Input Expected",
    "Room Name is required.",
    "Please ensure the room name is not blank."
  )

  def handleAddRoom() {
    if (roomNameField.text.value.length == 0) {
      MyApp.showAlert(roomNameBlankErrorMessage)
    } else {
      import Node._
      _roomName = roomNameField.text.value
      MyApp.clientActor ! RequestToCreateChatRoom(_roomName)
      dialogStage.close()
    }
  }

  def handleKeyBoard(action: KeyEvent) {
    if (action.code == KeyCode.ENTER) {
      handleAddRoom()
    } else if (action.code == KeyCode.ESCAPE) {
      handleCancel()
    }
  }

  def handleCancel() {
    dialogStage.close()
  }

}