import java.net.NetworkInterface
import akka.actor.{ActorSystem, Props}
import com.typesafe.config.{Config, ConfigFactory}
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene
import scalafxml.core.{FXMLLoader, NoDependencyResolver}
import scalafx.Includes._
import collection.JavaConverters._

object MyApp extends JFXApp {

  // Initialize Actor System
  val address = getBindingAddress()
  val config = setConfigWith(address)
  val system = ActorSystem("chat", config)

  // Initialize Actor
  val serverActor = system.actorOf(Props[Server], "server")
  val clientActor = system.actorOf(Props[Client], "client")
  val displayActor = system.actorOf(Props[Display], "display")

  // Initialize ScalaFX
  val ui = getClass.getResourceAsStream("MainUI.fxml")
  val loader: FXMLLoader = new FXMLLoader(null, NoDependencyResolver)
  loader.load(ui)
  val main = loader.getRoot[javafx.scene.layout.BorderPane]
  val controller = loader.getController[MainController#Controller]()

  // Initialize Stage
  stage = new PrimaryStage() {
    scene = new Scene() {
      root = main
    }
  }

  stage.onCloseRequest = handle {
    system.terminate
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

    println("Please select which interface to bind.")
    var selection: Int = 0
    do {
      selection = scala.io.StdIn.readInt()
    } while (selection <= 0 || selection >= addresses.size)

    return addresses(selection).getHostAddress
  }
}

