import akka.actor.ActorRef

case class User(var username: String, 
                val actorRef: ActorRef) extends Chattable {
  def key = username
  def key_=(key: String) {
    username = key
  }

  def chattableType = Personal
}

object User {
  def apply(userMaps: Map[String, ActorRef]): Seq[User] = {
    userMaps.map { case (username, actorRef) =>
      User(username, actorRef)
    }.toSeq
  }
}

