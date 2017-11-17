import scalafx.event.ActionEvent
import akka.actor.ActorRef
import scalafxml.core.{FXMLLoader, NoDependencyResolver}
import scalafx.Includes._
import scalafx.scene.control.{Button, Label, TextField, ListView, ListCell}
import scalafx.scene.layout.BorderPane
import scalafx.scene.input.{KeyEvent, KeyCode}
import scalafxml.core.macros.sfxml
import scala.collection.mutable.ArrayBuffer
import scalafx.collections.ObservableBuffer

@sfxml
class MainController(
  private val userList: ListView[Room],
  private val chatRoomList: ListView[Room],
  private val server: TextField,
  private val port: TextField,
  private val username: TextField,
  private val joinButton: Button,
  private val borderPane: BorderPane
) {

  var userListItems: ObservableBuffer[Room] = new ObservableBuffer[Room]()
  var chatRoomListItems: ObservableBuffer[Room] = new ObservableBuffer[Room]()
  setupUserListCell()
  setupRoomListCell()
  userList.items = userListItems
  chatRoomList.items = chatRoomListItems

  val usernameBlankErrorMessage = (
    "Input Expected",
    "Username is required.",
    "Please ensure the username is not blank."
  )
  
  def handleJoin() {
    import Node._
    if (username.text.value.length == 0) {
      MyApp.showAlert(usernameBlankErrorMessage)
    } else {
      MyApp.clientActor ! RequestToJoin(server.text.value, port.text.value, username.text.value)
    }
  }

  def handleCreateChatRoom(action: ActionEvent) {
    import Node._
    MyApp.showCreateChatRoomDialog()
  }

  def handleKeyBoard(action: KeyEvent) {
    if(action.code == KeyCode.ENTER) {
      handleJoin()
    }
  }

  def initialize(userEntries: Map[String, Room], 
                 roomEntries: Map[String, Room]) {
    val users = Room(userEntries)
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

  def showJoin(name: Room) {
    userListItems += name
  }

  def removeJoin(name: Room) {
    userListItems -= name
  }

  def showChatRoom {  
    borderPane.center = MyApp.chatRoomUI
  }

  def showUnread(from: String){
    //tell list cell to show unread
    println("Showing unread")
    for(userCell <- userListItems.toArray){
      if(userCell.name == from){
        userCell.unreadNumber += 1
        setupUserListCell()
      }
    }
    for(chatRoom <- chatRoomListItems.toArray){
      if(chatRoom.name == from){
        chatRoom.unreadNumber += 1
        setupRoomListCell()
      }
    }
  }
  def hideUnread(from: String){
    for(userCell <- userListItems.toArray){
      if(userCell.name == from){
        userCell.unreadNumber = 0
        setupUserListCell()
      }
    }
    for(chatRoom <- chatRoomListItems.toArray){
      if(chatRoom.name == from){
        chatRoom.unreadNumber = 0
        setupRoomListCell()
      }
    }
  }
  // Customize the ListCell in the List View
  private def setupUserListCell() {
    userList.cellFactory = { _ => 
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
            controller.unread = room.value.unreadNumber
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
            controller.unread = room.value.unreadNumber
            graphic = root
          }
        }}
      }
    }
  }
}
