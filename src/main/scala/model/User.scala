case class User(val actorRef: String, val username: String) 

object User {
  def apply(userMaps: Map[String, String]): Seq[User] = {
    userMaps.map { case (actorRef, username) =>
      User(actorRef, username)
    }.toSeq
  }
}

