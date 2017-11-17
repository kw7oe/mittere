import scalafx.scene.control.TextField
import scalafx.stage.Stage
import scalafxml.core.macros.sfxml
import scalafx.event.ActionEvent
import scalafx.scene.input.{KeyEvent, KeyCode}
import scalafx.Includes._

@sfxml 
class CreateChatRoomDialogController(
  private val roomNameField: TextField,
  ) {
  var dialogStage: Stage = null 
  private var _room: Room = null
  var okClicked: Boolean = false
  
  def room = _room
  def room_=(room: Room) {
    _room = room
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
      _room.name = roomNameField.text.value
      okClicked = true
      dialogStage.close()
    }
  }
  
  def handleKeyBoard(action: KeyEvent) {
    if(action.code == KeyCode.ENTER) {
      handleAddRoom()
    } else if (action.code == KeyCode.ESCAPE) {
      handleCancel()
    }
  }
  
  def handleCancel() {
    dialogStage.close()
  }
}