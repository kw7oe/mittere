import scalafx.event.ActionEvent
import scalafxml.core.{FXMLLoader, NoDependencyResolver}
import scalafx.Includes._
import scalafx.scene.control.{Button, Label, TextField, ListView, ListCell}
import scalafx.scene.layout.GridPane
import scalafxml.core.macros.sfxml
import scalafx.collections.ObservableBuffer

@sfxml
class MainController(
  private val userList: ListView[String],
  private val chatRoomList: ListView[String],
  private val server: TextField,
  private val port: TextField,
  private val username: TextField,
  private val joinButton: Button,
  private val parentGridPane: GridPane
) {

  var userListItems: ObservableBuffer[String] = new ObservableBuffer[String]()
  showJoin("test")
  setupCell()
  userList.setItems(userListItems)

  def handleJoin(action: ActionEvent) {
    import Client._
    MyApp.clientActor ! JoinRequest(server.text.value, port.text.value, username.text.value)
  }

  def clearJoin(): Unit = {
    parentGridPane.getChildren().clear()
  }

  def showUserList(names: Seq[String]): Unit = {
    userListItems.appendAll(names)
  }

  def showJoin(name: String): Unit = {
    userListItems += name
  }

  // Customize the ListCell in the List View
  private def setupCell() {
    userList.cellFactory = { _ => 
      new ListCell[String]() {
        item.onChange { (item, oldValue, newValue) => {
          if (newValue == null) {
            graphic = null
          } else {
            val loader = new FXMLLoader(null, NoDependencyResolver)
            val resource = getClass.getResourceAsStream("CustomListCell.fxml")
            loader.load(resource)
            val root = loader.getRoot[javafx.scene.layout.AnchorPane]
            val controller = loader.getController[ListCellController#Controller]

            controller.item = item.value
            graphic = root
          }
        }}
      }
    }
  }

}
