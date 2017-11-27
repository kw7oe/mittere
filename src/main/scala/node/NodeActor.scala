import akka.actor.{Actor, ActorLogging, ActorSelection, ActorRef, DeadLetter}
import scala.collection.mutable.ArrayBuffer
import scala.collection.immutable.{HashMap, HashSet,TreeMap, SortedMap}

trait NodeActor extends Actor {

  // The ActorSelection of Supernode
  var superNodeActor: Option[ActorSelection] = None

  // The username of the node
  var username: Option[String] = None

  // A HashMap of ActorRef -> Username
  var clientToUsername: Map[ActorRef, String] = new HashMap()

  // A SortedMap of Username -> ActorRef
  var usernameToClient: SortedMap[String,ActorRef] = new TreeMap()

  // A HashMap of Username -> Room (Personal)
  var usernameToRoom: Map[String, Room] = new HashMap()

  // A HashMap of room name -> Room (Group)
  var roomNameToRoom: Map[String, Room] = new HashMap()

  override def preStart(): Unit = {
    context.system.eventStream.subscribe(self, classOf[akka.remote.DisassociatedEvent])
    context.system.eventStream.subscribe(self, classOf[DeadLetter])
  }

  def receive: Receive =
    joinManagement orElse
    sessionManagement orElse
    chatManagement

  protected def joinManagement: Receive
  protected def sessionManagement: Receive
  protected def chatManagement: Receive
}