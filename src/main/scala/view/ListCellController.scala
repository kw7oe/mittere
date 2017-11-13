import scalafx.event.ActionEvent
import scalafx.scene.input.{MouseEvent, MouseButton}
import scalafx.scene.control.{Label, MenuItem}
import scalafx.beans.property.StringProperty
import scalafxml.core.macros.sfxml

@sfxml
class ListCellController(
  private val label: Label,
  private val sendMessageMenuItem: MenuItem,
  private val kickMenuItem: MenuItem
) {
  private var _user: User = null

  def user = _user
  def user_=(user: User) {
    _user = user
    label.text = user.username
  }

  def sendMessage(action: ActionEvent) {
    import Client._
    MyApp.clientActor ! RequestToCreateChat(user)
  }

  def handleShowChat(action: MouseEvent) {
    import Client._
    if (action.button == MouseButton.Primary) {
      MyApp.clientActor ! RequestToGetChatWith(user)
    }
  }

  def kick(action: ActionEvent) {
  }

}
