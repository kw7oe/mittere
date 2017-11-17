import akka.actor.{Actor, ActorLogging, ActorSelection, ActorRef, DeadLetter}

trait JoinManagement extends ActorLogging { this: Actor =>
  import Node._

  var serverActor: Option[ActorSelection]
  var username: Option[String]
  var usernameToClient: Map[String,ActorRef]
  var roomNameToRoom: Map[String, Room]

  private val invalidAddressErrorMessage = (
    "Invalid Address",
    "Server and port combination provided cannot be connect.",
    "Please enter a different server and port combination."
  )
  private val invalidUsernameErrorMessage = (
    "Invalid Username", 
    "Username has already been taken.", 
    "Please enter a different username"
  )

  protected def joinManagement: Receive = {
    case d: DeadLetter =>
      log.info(s"Receive DeadLetter: $d")
      if (isJoinDeadLetter(d)) { 
        displayAlert(invalidAddressErrorMessage)
      }   
    case RequestToJoin(serverAddress, portNumber, name) =>
      log.info("RequestToJoin")
      serverActor = Some(MyApp.system.actorSelection(s"akka.tcp://chat@$serverAddress:$portNumber/user/server"))
      username = Some(name)
      serverActor.get ! Server.Join(username.get)  
    case InvalidUsername =>
      log.info("Receive InvalidUsername")
      displayAlert(invalidUsernameErrorMessage)
    case Joined(users, rooms)  =>
      // Receive online users and rooms info from Manager
      log.info("Joined")

      // Keep track of the info locally
      usernameToClient = users
      roomNameToRoom = rooms

      // Initialize Display with the info received
      MyApp.displayActor ! Display.Initialize(usernameToClient, roomNameToRoom)
  }
  
  private def isJoinDeadLetter(deadLetter: DeadLetter): Boolean = {
    return deadLetter.message == Server.Join(username.get)
  }

  private def displayAlert(messages: Tuple3[String, String, String]) {
    MyApp.displayActor ! Display.ShowAlert(messages)
  }
}