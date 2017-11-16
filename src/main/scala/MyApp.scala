import java.net.NetworkInterface
import akka.actor.{ActorSystem, Props}
import com.typesafe.config.{Config, ConfigFactory}
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene
import scalafx.stage.Stage
import scalafx.scene.control.{Dialog, DialogPane, Alert}
import scalafx.scene.control.Alert.AlertType
import scalafx.stage.Modality
import scalafxml.core.{FXMLLoader, NoDependencyResolver}
import scalafx.Includes._
import collection.JavaConverters._

object MyApp extends JFXApp {

  // Initialize Actor System
  val address = getBindingAddress()
  val config = setConfigWith(address)
  val system = ActorSystem("chat", config)

  // To be used in scehduling stop showing typing
  val scheduler = system.scheduler
  implicit val executor = system.dispatcher

  // Initialize Actor
  val serverActor = system.actorOf(Props[Server], "server")
  val clientActor = system.actorOf(Props[Client], "client")
  val displayActor = system.actorOf(Props[Display], "display")

  // Initialize Main UI
  val mainFXML = getClass.getResourceAsStream("MainUI.fxml")
  val mainLoader: FXMLLoader = new FXMLLoader(null, NoDependencyResolver)
  mainLoader.load(mainFXML)
  val main = mainLoader.getRoot[javafx.scene.layout.BorderPane]
  val mainController = mainLoader.getController[MainController#Controller]()

  // Initialize ChatRoom UI
  val chatLoader: FXMLLoader = new FXMLLoader(null, NoDependencyResolver)
  val chatRoomFXML = getClass.getResourceAsStream("ChatRoomUI.fxml")
  chatLoader.load(chatRoomFXML)
  val chatRoomUI = chatLoader.getRoot[javafx.scene.layout.AnchorPane]
  val chatController = chatLoader.getController[ChatRoomController#Controller]
  // Initialize Stage
  stage = new PrimaryStage() {
    scene = new Scene() {
      root = main
    }
  }

  stage.onCloseRequest = handle {
    system.terminate
  }

  def showCreateChatRoomDialog(room: Room) : Boolean = {
    val resource = getClass.getResourceAsStream("CreateChatRoomDialog.fxml")
    val loader: FXMLLoader = new FXMLLoader(null, NoDependencyResolver)
    loader.load(resource)
    val root2 = loader.getRoot[javafx.scene.layout.AnchorPane]
    val controller = loader.getController[CreateChatRoomDialogController#Controller]
    val dialog = new Stage() {
      initModality(Modality.APPLICATION_MODAL)
      initOwner(stage)
      title = "Create Chat Room"
      scene = new Scene {
        root = root2
      }
    }
    controller.dialogStage = dialog
    controller.room = room
    dialog.showAndWait();
    return controller.okClicked;
  }

  def showAlert(_title: String,
                _headerText: String,
                _contentText: String) : Unit = {
    val alert = new Alert(AlertType.Warning) {
      initOwner(stage)
      title       = _title
      headerText  = _headerText
      contentText = _contentText
    }.showAndWait
  }

  // Set Config with specified IP address
  private def setConfigWith(ipaddress: String): Config = {
    val overrideConf = ConfigFactory.parseString(
      s"""
      |akka {
       |  loglevel = "INFO"
       |
       |  actor {
         |    provider = "akka.remote.RemoteActorRefProvider"
         |  }
         |
         |  remote {
           |    enabled-transports = ["akka.remote.netty.tcp"]
           |    netty.tcp {
             |      hostname = "${ipaddress}"
             |      port = 0
             |    }
             |
             |    log-sent-messages = on
             |    log-received-messages = on
             |  }
             |
             |}
             |
             """.stripMargin)

    return overrideConf.withFallback(ConfigFactory.load())
  }

  // Get Binding Address from User
  private def getBindingAddress(): String = {
    var count = -1
    val addresses = (for (inf <- NetworkInterface.getNetworkInterfaces.asScala;
                          add <- inf.getInetAddresses.asScala) yield {
      count = count + 1
      (count -> add)
    }).toMap

    for ((i, add) <- addresses) {
      println(s"$i = $add")
    }

    val max = addresses.size - 1
    var selection: Int = 0
    do {
      println(s"Please select between 0 to ${max} to bind the interface.")
      try{
        selection = scala.io.StdIn.readInt()
      } catch {
        case e: Exception => println("Please select using number")
      }
    } while (selection < 0 || selection > max)

    return addresses(selection).getHostAddress
  }
}

