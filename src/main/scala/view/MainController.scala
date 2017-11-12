import scalafx.event.ActionEvent
import scalafxml.core.{FXMLLoader, NoDependencyResolver}
import scalafx.Includes._
import scalafx.scene.control.{Button, Label, TextField, ListView, ListCell}
import scalafx.scene.layout.BorderPane
import scalafxml.core.macros.sfxml
import scalafx.collections.ObservableBuffer

@sfxml
class MainController(
  private val userList: ListView[User],
  private val chatRoomList: ListView[User],
  private val server: TextField,
  private val port: TextField,
  private val username: TextField,
  private val joinButton: Button,
  private val borderPane: BorderPane
) {

  var userListItems: ObservableBuffer[User] = new ObservableBuffer[User]()
  setupCell()
  userList.setItems(userListItems)

  def handleJoin(action: ActionEvent) {
    import Client._
    MyApp.clientActor ! RequestToJoin(server.text.value, port.text.value, username.text.value)
  }

  def clearJoin(): Unit = {
    borderPane.center = null
  }

  def showUserList(names: Map[String, String]): Unit = {
    val users = User(names)
    userListItems.appendAll(users)
  }

  def showJoin(name: User): Unit = {
    userListItems += name
  }

  def showChatRoom(): Unit = {
    val loader = new FXMLLoader(null, NoDependencyResolver)
    val resource = getClass.getResourceAsStream("ChatRoomUI.fxml")
    loader.load(resource)
    val root = loader.getRoot[javafx.scene.layout.AnchorPane]
    val controller = loader.getController[ChatRoomController#Controller]
    borderPane.center = root
  }

  // Customize the ListCell in the List View
  private def setupCell() {
    userList.cellFactory = { _ => 
      new ListCell[User]() {
        item.onChange { (user, oldValue, newValue) => {
          if (newValue == null) {
            graphic = null
          } else {
            val loader = new FXMLLoader(null, NoDependencyResolver)
            val resource = getClass.getResourceAsStream("CustomListCell.fxml")
            loader.load(resource)
            val root = loader.getRoot[javafx.scene.layout.AnchorPane]
            val controller = loader.getController[ListCellController#Controller]

            controller.user = user.value
            graphic = root
          }
        }}
      }
    }
  }

}
