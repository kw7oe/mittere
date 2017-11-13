case class User(val actorPath: String, val username: String) 

object User {
  def apply(userMaps: Map[String, String]): Seq[User] = {
    userMaps.map { case (actorPath, username) =>
      User(actorPath, username)
    }.toSeq
  }
}

