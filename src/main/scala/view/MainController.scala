import scalafx.event.ActionEvent
import scalafxml.core.{FXMLLoader, NoDependencyResolver}
import scalafx.Includes._
import scalafx.scene.control.{Button, Label, TextField, ListView, ListCell}
import scalafxml.core.macros.sfxml
import scalafx.collections.ObservableBuffer

@sfxml
class MainController(
  private val userList: ListView[String],
  private val chatRoomList: ListView[String],
  private val server: TextField,
  private val port: TextField,
  private val username: TextField,
  private val joinButton: Button
) {

  var userListItems: ObservableBuffer[String] = new ObservableBuffer[String]()
  userList.setItems(userListItems)
  setupCell()

  def handleJoin(action: ActionEvent) {
    import Client._
    MyApp.clientActor ! JoinRequest(server.text.value, port.text.value, username.text.value)
  }

  def showUserList(names: Seq[String]): Unit = {
    userListItems.appendAll(names)
  }

  def showJoin(name: String): Unit = {
    userListItems += name
  }

  // Customize the ListCell in the List View
  private def setupCell() {
    new ListCell[String]() {
      styleClass = List("custom_cell")
      item.onChange { (item, oldValue, newValue) => {
        if (newValue == null) {
          graphic = null
        } else {
          val loader = new FXMLLoader(null, NoDependencyResolver)
          val resource = getClass.getResourceAsStream("ListCell.fxml")
          loader.load(resource)
          val root = loader.getRoot[javafx.scene.layout.AnchorPane]
          val controller = loader.getController[ListCellController#Controller]

          controller.item = item.toString()
          graphic = root
        }
      }}
    }
  }


}
