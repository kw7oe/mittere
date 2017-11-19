import akka.actor.ActorRef
import scalafx.Includes._
import scalafx.scene.control.{Button, TextField}
import scalafx.scene.input.{KeyEvent, KeyCode}
import scalafxml.core.macros.sfxml

@sfxml
class JoinScreenController(
  private val serverField: TextField,
  private val portField: TextField,
  private val usernameField: TextField,
  private val joinButton: Button
) {

  def handleJoin() {
    import Node._
    var server = serverField.text.value
    var port = portField.text.value
    var username = usernameField.text.value

    if (username.length == 0) {
      MyApp.showAlert(("Input Expected",
       "Username is required.",
       "Please ensure the username is not blank."
      ))
    } else {
      MyApp.clientActor ! RequestToJoin(server,port,username)
    }
  }

  def handleKeyboard(action: KeyEvent) {
    if (action.code == KeyCode.ENTER) {
      handleJoin()
    }
  }
}
