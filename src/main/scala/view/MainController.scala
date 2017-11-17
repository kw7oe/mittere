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
  private val userList: ListView[Chattable],
  private val roomList: ListView[Chattable],
  private val createRoomButton: Button,
  private val quitButton: Button,
  private val roomNameLabel: Label,
  private val descriptionLabel: Label,
  private val messageList: ListView[Messages],
  private val messageArea: TextArea
) {

  var userListItems: ObservableBuffer[User] = new ObservableBuffer[User]()
  var chatRoomListItems: ObservableBuffer[Room] = new ObservableBuffer[Room]()
  setupUserListCell()
  setupRoomListCell()
  userList.items = userListItems
  chatRoomList.items = chatRoomListItems

  //menu item
  def handleClose(action: ActionEvent){
    System.exit(0)
  }
  def showAbout(action: ActionEvent){
    MyApp.showAbout()
  }

  def handleJoin() {
    import Client._
    if (username.text.value.length == 0) {
      MyApp.showAlert(
       _title =  "Input Expected",
       _headerText = "Username is required.",
       _contentText = "Please ensure the username is not blank."
      )
    } else {
      MyApp.clientActor ! RequestToJoin(server.text.value, port.text.value, username.text.value)
    }
  }

  def handleCreateChatRoom(action: ActionEvent) {
    import Client._
    val tempRoom = Room(null, new ArrayBuffer[Room.Message](), null)
    val okClicked = MyApp.showCreateChatRoomDialog(tempRoom)
    if (okClicked) {
      MyApp.clientActor ! RequestToCreateChatRoom(tempRoom)
    }
  }

  def handleKeyBoard(action: KeyEvent) {
    if(action.code == KeyCode.ENTER) {
      handleJoin()
    }
  }

  def initialize(userEntries: Map[String, ActorRef], 
                 roomEntries: Map[String, Room]) {
    val users = User(userEntries)
    userListItems.appendAll(users) 
    val rooms = Room(roomEntries)
    chatRoomListItems.appendAll(rooms)
  }


  def showMain() {
    MyApp.showMain()
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

  def showUnread(from: String, roomType: String){
    //tell list cell to show unread
    println("Showing unread")
    if(roomType == "personal"){
      for(userCell <- userListItems.toArray){
        if(userCell.username == from){
          userCell.unreadNumber += 1
          setupUserListCell()
        }
      }
    }else if (roomType == "group"){
      for(chatRoom <- chatRoomListItems.toArray){
        if(chatRoom.name == from){
          chatRoom.unreadNumber += 1
          setupRoomListCell()
        }
      }
    } 
  }
  def hideUnread(chattable: Chattable){
    chattable.chattableType match {
        case Group =>
          for(chatRoom <- chatRoomListItems.toArray){
            if(chatRoom.name == chattable.key){
              chatRoom.unreadNumber = 0
              setupRoomListCell()
            }
          }
        case Personal =>
          for(userCell <- userListItems.toArray){
            if(userCell.username == chattable.key){
              userCell.unreadNumber = 0
              setupUserListCell()
            }
          }
      }
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

            controller.chattable = user.value
            controller.unread = user.value.unreadNumber
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

            controller.chattable = room.value
            controller.unread = room.value.unreadNumber
            graphic = root
          }
        }}
      }
    }
  }
}
