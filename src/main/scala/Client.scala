import akka.actor.{Actor, Props, ActorLogging, ActorSelection}
import scalafx.collections.{ObservableHashMap, ObservableMap}
import scala.collection.mutable.{Map, HashMap}

object Client {
  case class JoinRequest(serverAddress: String, portNumber: String, username: String)
  case class Joined(otherUsers: Map[String,String])
  case class NewUser(ref: String, name: String)
}

class Client extends Actor with ActorLogging {
  import Client._

  // { "akka.tcp://..." -> "username" }
  var otherUsers: Map[String,String] = new HashMap()
  var serverActor: Option[ActorSelection] = None
  var username: Option[String] = None

  def receive = {
    case JoinRequest(serverAddress, portNumber, name) =>
      serverActor = Some(MyApp.system.actorSelection(s"akka.tcp://chat@$serverAddress:$portNumber/user/server"))
      username = Some(name)
      serverActor.get ! Server.Join(username.get)
    case Joined(users)  =>
      otherUsers = users
      MyApp.displayActor ! Display.ShowUserList(otherUsers.values.toSeq)
      sender() ! Server.ReceivedJoined(username.get)
      context.become(joined)
    case _ => log.info("Received unknown message")
  }

  def joined: Receive = {
    case NewUser(ref, name) =>
      otherUsers += (ref -> name)
      MyApp.displayActor ! Display.ShowJoin(name)
    case _ => log.info("Received unknown message")
  }

}
