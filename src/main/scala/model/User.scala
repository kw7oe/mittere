import akka.actor.ActorRef

case class User(val username: String, 
                val actorRef: ActorRef) extends Chattable {
  def key = username
  def chattableType = Personal
}

object User {
  def apply(userMaps: Map[String, ActorRef]): Seq[User] = {
    userMaps.map { case (username, actorRef) =>
      User(username, actorRef)
    }.toSeq
  }
}

