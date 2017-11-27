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
  private var _roomUsers: Option[Set[String]] = None
  final var shouldListenToTyping = true

  setupListCell()
  setupMessageListCell()
  messageList.items = messages
  userList.items = userListItems
  roomList.items = roomListItems

  val usernameBlankErrorMessage = (
    "Input Expected",
    "Username is required.",
    "Please ensure the username is not blank."
  )

  def handleQuit(action: ActionEvent) {
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

  def showJoin(user: Room) {
    userListItems += user
  }

  def removeJoin(user: Room) {
    userListItems -= user
    _room match {
      case Some(c) =>
        if (user.identifier == c.identifier) {
          descriptionLabel.text = "offline"
          messageArea.disable = true
        }
      case None=>
    }
  }

  def room: Option[Room] = _room
  def room_=(room: Room) {
    _room = Some(room)
    roomNameLabel.text = _room.get.name
  }

  def roomUsers: Option[Set[String]] = _roomUsers
  def roomUsers_=(users: Set[String]) {
    _roomUsers = Some(users)
    descriptionLabel.text = users.mkString(" · ")
  }



  def showRoom(room: Room, users: Set[String]) {
    this.room = room
    this.messages = room.messages
    room.chatRoomType match {
      case Personal => descriptionLabel.text = "online"
      case Group => roomUsers = users
    }
    this.messageArea.disable = false
    messageList.scrollTo(messages.length)
    setupListCell()
  }

  def showUnread(room: Room) {
    //tell list cell to show unread
    if(checkIsMe(room)){
      return
    }
    var items = room.chatRoomType match {
      case Personal => userListItems
      case Group => roomListItems
    }

    for (index <- 0 until items.size) {
      var item = items(index)
      if (item.identifier == room.identifier) {
        item.unreadNumber += 1
        items.remove(index)
        items.prepend(item)
      }
    }
  }

  def hideUnread(room: Room) {
    if(checkIsMe(room)){
      return
    }
    var items = room.chatRoomType match {
      case Personal => userListItems
      case Group => roomListItems
    }

    for (item <- items) {
      if (item.identifier == room.identifier) {
        item.unreadNumber = 0
      }
    }
  }

  // Customize the ListCell in the List View
  private def setupListCell() {
    val callback: (ListView[Room]) => ListCell[Room]  = { _ =>
      new ListCell[Room]() { cell =>
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
            if(checkIsMe(room.value)){
              controller.isMe = true
            }
            _room match {
              case Some(c) =>
                var highlighted = (c.identifier==room.value.identifier)
                controller.setHighlight(highlighted)
              case None =>
            }
            graphic = root
          }
        }}
      }
    }
    roomList.cellFactory = callback
    userList.cellFactory = callback
  }

  private def setupMessageListCell() {
    messageList.cellFactory = { _ =>
      new ListCell[Room.Message]() { cell =>
        item.onChange { (message, oldValue, newValue) => {
          if (newValue == null) {
            graphic = null
          } else {
            val loader = new FXMLLoader(null, NoDependencyResolver)
            room.get.chatRoomType match {
              case Personal =>

                val resource = getClass.getResourceAsStream("CustomMessageCell.fxml")
                loader.load(resource)
                val root = loader.getRoot[javafx.scene.layout.HBox]
                val controller = loader.getController[MessageController#Controller]

                if (message.value.from == username.get) {
                  controller.setAlign(Pos.CenterRight)
                } else {
                  controller.setAlign(Pos.CenterLeft)
                }
                controller.setMessage(message.value.value)
                graphic = root

              case Group =>
                if (message.value.from == username.get) {
                  val resource = getClass.getResourceAsStream("CustomMessageCell.fxml")
                  loader.load(resource)
                  val root = loader.getRoot[javafx.scene.layout.HBox]
                  val controller = loader.getController[MessageController#Controller]
                  controller.setAlign(Pos.CenterRight)
                  controller.setMessage(message.value.value)
                  graphic = root
                } else {
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

  // Chatroom related
  def refreshRoom(room: Room, username: String, action: RoomAction) {
    this.room = room

    action match {
      case AddUsername => roomUsers = (_roomUsers.get + username)
      case RemoveUsername => roomUsers = (_roomUsers.get - username)
    }
  }

  def messages_=(messages: ArrayBuffer[Room.Message]) {
    this.messages = ObservableBuffer(messages)
    messageList.items = this.messages
  }

  def handleSend() {
    import Node._
    room.foreach { c =>
      MyApp.clientActor ! RequestToSendMessage(c, messageArea.text.value)
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
      // have access to without room
      MyApp.clientActor ! Typing(room.get)
      shouldListenToTyping = false
      val task = new Runnable {
        def run() {
          shouldListenToTyping = true
        }
      }
      MyApp.scheduler.scheduleOnce(2 second, task)
    }

  }

  def showStatus(name: String) {
    descriptionLabel.text = name + " is typing"
    val task = new Runnable {
      def run() {
        Platform.runLater { resetStatus() }
      }
    }
    MyApp.scheduler.scheduleOnce(2 second, task)
  }

  def resetStatus() {
    if (room.get.chatRoomType == Group) {
      descriptionLabel.text = roomUsers.get.mkString(" · ")
    } else {
      descriptionLabel.text = "online"
    }
  }

  def addMessage(message: Room.Message) {
    // If receive message from the user that is typing
    // reset the description
    if (message.from + " is typing" == descriptionLabel.text.value) {
      resetStatus()
    }
    messages += message
    messageList.scrollTo(messages.length)
  }

  def checkIsMe(r: Room):Boolean = {
    return (username.get+":"+username.get == r.identifier)
  }

  def updateJoinedUsers(identifier:String, users: Set[String]){
    if(identifier == room.get.identifier){
      roomUsers = users
    }
  }
}
