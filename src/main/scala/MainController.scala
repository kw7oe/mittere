import scalafx.event.ActionEvent
import scalafx.scene.control.{Button, Label, TextField}
import scalafxml.core.macros.sfxml

@sfxml
class MainController(
  private val server: TextField,
  private val port: TextField,
  private val username: TextField,
  private val joinButton: Button
) {

  def handleJoin(action: ActionEvent) {
    import Client._
    MyApp.clientActor ! JoinRequest(server.text.value, port.text.value, username.text.value)
  }

  def showJoined(name: String): Unit = {

  }

}
