import scalafx.event.ActionEvent
import akka.actor.ActorRef
import scalafxml.core.{FXMLLoader, NoDependencyResolver}
import scalafx.Includes._
import scalafx.scene.control.{Button, Label, TextField, ListView, ListCell}
import scalafx.scene.layout.BorderPane
import scala.collection.mutable.ArrayBuffer
import scalafxml.core.macros.sfxml
import scalafx.collections.ObservableBuffer

@sfxml
class MainController(
  private val userList: ListView[User],
  private val chatRoomList: ListView[Room],
  private val server: TextField,
  private val port: TextField,
  private val username: TextField,
  private val joinButton: Button,
  private val borderPane: BorderPane
) {

  var userListItems: ObservableBuffer[User] = new ObservableBuffer[User]()
  var chatRoomListItems: ObservableBuffer[Room] = new ObservableBuffer[Room]()
  setupUserListCell()
  setupRoomListCell()
  userList.items = userListItems
  chatRoomList.items = chatRoomListItems

  def handleJoin(action: ActionEvent) {
    import Client._
    MyApp.clientActor ! RequestToJoin(server.text.value, port.text.value, username.text.value)
  }

  def handleCreateChatRoom(action: ActionEvent) {
    import Client._
    val tempRoom = Room(null, new ArrayBuffer[Room.Message](), null)
    val okClicked = MyApp.showCreateChatRoomDialog(tempRoom)
    if (okClicked) {
      MyApp.clientActor ! RequestToCreateChatRoom(tempRoom)
    }
  }

  def initialize(userEntries: Map[String, ActorRef], 
                 roomEntries: Map[String, Room]) {
    val users = User(userEntries)
    userListItems.appendAll(users) 
    val rooms = Room(roomEntries)
    chatRoomListItems.appendAll(rooms)
  }


  def clearJoin() {
    borderPane.center = null
  }

  def showNewChatRoom(room: Room) {
    chatRoomListItems += room
  }

  def removeChatRoom(room: Room) {
    chatRoomListItems -= room
  }

  def showJoin(name: User) {
    userListItems += name
  }

  def removeJoin(name: User) {
    userListItems -= name
  }

  def showChatRoom {  
    borderPane.center = MyApp.chatRoomUI
  }

  // Customize the ListCell in the List View
  private def setupUserListCell() {
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

  private def setupRoomListCell() {
    chatRoomList.cellFactory = { _ => 
      new ListCell[Room]() {
        item.onChange { (room, oldValue, newValue) => {
          if (newValue == null) {
            graphic = null
          } else {
            val loader = new FXMLLoader(null, NoDependencyResolver)
            val resource = getClass.getResourceAsStream("CustomListCell.fxml")
            loader.load(resource)
            val root = loader.getRoot[javafx.scene.layout.AnchorPane]
            val controller = loader.getController[ListCellController#Controller]

            controller.room = room.value
            graphic = root
          }
        }}
      }
    }
  }
}
