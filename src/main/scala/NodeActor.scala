import akka.actor.{Actor, ActorLogging, ActorSelection, ActorRef, DeadLetter}
import scala.collection.mutable.ArrayBuffer
import scala.collection.immutable.{HashMap, HashSet}

trait NodeActor extends Actor {
  var serverActor: Option[ActorSelection] = None
  var username: Option[String] = None
  var usernameToClient: Map[String,ActorRef] = new HashMap()
  var usernameToRoom: Map[String, Room] = new HashMap()
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