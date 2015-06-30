package lila.relay

trait FICS with Actor with Stash with LoggingFSM[FICS.State, Option[FICS.Request]] {

  def log(lines: List[String]) {
    lines filterNot noise foreach { l =>
      println(s"FICS[$stateName] $l")
    }
    // lines filter noise foreach { l =>
    //   println(s"            (noise) [$stateName] $l")
    // }
  }

  val noiseR = List(
    """^\\ .*""".r,
    """^:$""".r,
    """^fics%""".r,
    """^You will not.*""".r,
    """.*You are now observing.*""".r,
    """.*You are already observing.*""".r,
    """^Game \d+: .*""".r,
    """.*To find more about Relay.*""".r,
    """.*You are already observing game \d+""".r,
    """.*Removing game \d+.*""".r,
    """.*There are no tournaments in progress.*""".r,
    """.*Challenge from.*""".r,
    """.*who was challenging you.*""".r,
    // """.*in the history of both players.*""".r,
    // """.*will be closed in a few minutes.*""".r,
    """^\(told relay\)$""".r,
    """^relay\(.+\)\[\d+\] kibitzes: .*""".r,
    // """Welcome to the Free Internet Chess Server""".r,
    // """Starting FICS session""".r,
    """.*ROBOadmin.*""".r,
    """.*ANNOUNCEMENT.*""".r)

  def noise(str: String) = noiseR exists matches(str)

  def matches(str: String)(r: scala.util.matching.Regex) = r.pattern.matcher(str).matches
}

object FICS {

  case class Config(host: String, port: Int, login: String, password: String, enabled: Boolean) {
    def remote = new java.net.InetSocketAddress(host, port)
  }

  sealed trait State
  case object Connect extends State
  case object Login extends State
  case object Enter extends State
  case object Configure extends State
  case object Ready extends State
  case object Run extends State
  case object Throttle extends State

  case class Request(cmd: command.Command, replyTo: ActorRef)

  trait Stashable
  case class Observe(ficsId: Int) extends Stashable
  case class Unobserve(ficsId: Int) extends Stashable

  case object Limited {
    val R = "You are already observing the maximum number of games"
    def apply(str: String): Option[Limited.type] = str contains R option (Limited)
  }

  private val EOM = "fics% "
}
