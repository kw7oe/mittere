import scalafx.event.ActionEvent
import akka.actor.ActorRef
import scalafxml.core.{FXMLLoader, NoDependencyResolver}
import scalafx.Includes._
import scalafx.scene.control.{Button, Label, TextArea, ListView, ListCell}
import scalafx.scene.input.{KeyEvent, KeyCode}
import scalafxml.core.macros.sfxml
import scala.collection.mutable.ArrayBuffer
import scalafx.collections.ObservableBuffer
import scalafx.application.Platform
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scalafx.geometry.Pos

@sfxml
class MainController(
  private val userList: ListView[Room],
  private val roomList: ListView[Room],
  private val createRoomButton: Button,
  private val quitButton: Button,
  private val roomNameLabel: Label,
  private val descriptionLabel: Label,
  private val messageList: ListView[Room.Message],
  private val messageArea: TextArea

) {

  var userListItems: ObservableBuffer[Room] = new ObservableBuffer[Room]()
  var roomListItems: ObservableBuffer[Room] = new ObservableBuffer[Room]()
  var messages: ObservableBuffer[Room.Message] = new ObservableBuffer[Room.Message]()
  private var _room: Option[Room] = None
  private var username: Option[String] = None
  final var shouldListenToTyping = true

  setupUserListCell()
  setupRoomListCell()
  setupMessageListCell()
  messageList.items = messages
  userList.items = userListItems
  roomList.items = roomListItems

  val usernameBlankErrorMessage = (
    "Input Expected",
    "Username is required.",
    "Please ensure the username is not blank."
  )
 
  def handleQuit(action: ActionEvent){
    System.exit(0)
  }

  def createRoom(action: ActionEvent) {
    import Node._
    MyApp.showCreateChatRoomDialog()
  }

  def initialize(userEntries: Map[String, Room], 
                 roomEntries: Map[String, Room],
                 username: String) {
    val users = Room(userEntries)
    userListItems.appendAll(users) 
    val rooms = Room(roomEntries)
    roomListItems.appendAll(rooms)
    this.username = Some(username)
  }

  def addChatroom(room: Room) {
    roomListItems += room
  }

  def removeChatroom(room: Room) {
    roomListItems -= room
  }

  def showJoin(name: Room) {
    userListItems += name
  }

  def removeJoin(name: Room) {
    userListItems -= name
  }

  def room: Option[Room] = _room
  def room_=(room: Room) {
    _room = Some(room)
    roomNameLabel.text = _room.get.name
  }
  
  def showRoom(room: Room, messages: ArrayBuffer[Room.Message]) {
    this.room = room
    this.messages = messages
    room.chatRoomType match {
      case Personal =>
        this.descriptionLabel.text = "online"
        this.messageArea.disable = false
      case Group=>
        var usernames = new ArrayBuffer[String]
        userListItems.foreach { room => 
          usernames.append(room.name)
        }
        var description = usernames.mkString(" · ")
        this.descriptionLabel.text = description
        this.messageArea.disable = false
    }
  }

  def showUnread(room: Room){
    //tell list cell to show unread
    room.chatRoomType match {
      case Personal =>
        for(userCell <- userListItems.toArray){
          if(userCell.identifier == room.identifier){
            userCell.unreadNumber += 1
            setupUserListCell()
          }
        }
      case Group =>
        for(chatroom <- roomListItems.toArray){
          if(chatroom.identifier == room.identifier){
            chatroom.unreadNumber += 1
            setupRoomListCell()
          }
        }
    }
  }

  def hideUnread(room: Room){
    room.chatRoomType match {
        case Group =>
          for(chatroom <- roomListItems.toArray){
            if(chatroom.identifier == room.identifier){
              chatroom.unreadNumber = 0
            }
          }
        case Personal =>
          for(chatroom <- userListItems.toArray){
            if(chatroom.identifier == room.identifier){
              chatroom.unreadNumber = 0
            }
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
            val root = loader.getRoot[javafx.scene.layout.HBox]
            val controller = loader.getController[ListCellController#Controller]

            controller.room = room.value
            controller.showUnread(room.value.unreadNumber)
            graphic = root
          }
        }}
      }
    }
  }

  private def setupRoomListCell() {
    roomList.cellFactory = { _ => 
      new ListCell[Room]() {
        item.onChange { (room, oldValue, newValue) => {
          if (newValue == null) {
            graphic = null
          } else {
            val loader = new FXMLLoader(null, NoDependencyResolver)
            val resource = getClass.getResourceAsStream("CustomListCell.fxml")
            loader.load(resource)
            val root = loader.getRoot[javafx.scene.layout.HBox]
            val controller = loader.getController[ListCellController#Controller]

            controller.room = room.value
            controller.showUnread(room.value.unreadNumber)
            controller.hideCircle()
            graphic = root
          }
        }}
      }
    }
  }
  private def setupMessageListCell() {
    messageList.cellFactory = { _ => 
      new ListCell[Room.Message]() { cell =>
        item.onChange { (message, oldValue, newValue) => {
          if (newValue == null) {
            graphic = null
          } else {
            room.get.chatRoomType match {
              case Personal =>
                val loader = new FXMLLoader(null, NoDependencyResolver)
                val resource = getClass.getResourceAsStream("CustomMessageCell.fxml")
                loader.load(resource)
                val root = loader.getRoot[javafx.scene.layout.HBox]
                val controller = loader.getController[MessageController#Controller]

                if(message.value.from == username.get){
                  controller.setAlign(Pos.CenterRight)
                }else{
                  controller.setAlign(Pos.CenterLeft)
                }
                controller.setMessage(message.value.value)
                graphic = root
                
              case Group =>
                if(message.value.from == username.get){
                  val loader = new FXMLLoader(null, NoDependencyResolver)
                  val resource = getClass.getResourceAsStream("CustomMessageCell.fxml")
                  loader.load(resource)
                  val root = loader.getRoot[javafx.scene.layout.HBox]
                  val controller = loader.getController[MessageController#Controller]
                  controller.setAlign(Pos.CenterRight)
                  controller.setMessage(message.value.value)
                  graphic = root
                }else{
                  val loader = new FXMLLoader(null, NoDependencyResolver)
                  val resource = getClass.getResourceAsStream("RoomMessageCell.fxml")
                  loader.load(resource)
                  val root = loader.getRoot[javafx.scene.layout.HBox]
                  val controller = loader.getController[RoomMessageController#Controller]
                  controller.setMessage(message.value.value)
                  controller.setSender(message.value.from)
                  graphic = root
                }
                
            }
            
          }
        }}
      }
    }
  }

  // chatroom controller
  def messages_=(messages: ArrayBuffer[Room.Message]) {
    this.messages = ObservableBuffer(messages)
    messageList.items = this.messages
  }

  def handleSend() {
    import Node._
    room match {
      case Some(c) => 
        MyApp.clientActor ! RequestToSendMessage(c, messageArea.text.value)
      case None => // Do Nothing
    }
  }

  def handleTyped(action: KeyEvent) {
    import Node._

    if (action.code == KeyCode.ENTER && action.shiftDown) {
      messageArea.appendText("\n")
    } else if (action.code == KeyCode.ENTER) {
      action.consume()
      handleSend()
      messageArea.text.value = ""
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

  def showStatus(name: String) {
    descriptionLabel.text = name + " is typing"
    val task = new Runnable {
      def run() { 
        Platform.runLater {
          descriptionLabel.text = ""
        }
      } 
    }
    MyApp.scheduler.scheduleOnce(5 second, task)
  }

  def addMessage(message: Room.Message) {
    messages += message
  }
}
