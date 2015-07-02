package lila.relay

import akka.actor._

abstract class FICS[A](config: FICS.Config)
    extends Actor with LoggingFSM[FICS.State, A] {

  import FICS._
  import Telnet._

  var send: String => Unit = _

  def afterConfigure: State

  val telnet = context.actorOf(Props(classOf[Telnet], config.remote, self), name = "telnet")

  when(Connect) {
    case Event(Connection(s), _) =>
      send = s
      goto(Login)
  }

  when(Login) {
    case Event(In(data), _) if data endsWith "login: " =>
      send("guest")
      goto(Enter)
    case Event(in: In, _) => stay
  }

  when(Enter) {
    case Event(In(data), _) if data contains "Press return to enter the server" =>
      telnet ! BufferUntil(FICS.EOM.some)
      send("")
      for (v <- Seq("seek", "shout", "cshout", "pin", "gin")) send(s"set $v 0")
      for (c <- Seq(1, 4, 53)) send(s"- channel $c")
      send("set kiblevel 3000") // shut up if your ELO is < 3000
      send("style 12")
      stay
    case Event(In(data), _) if data contains "Style 12 set." => afterConfigure
    case Event(in: In, _)                                    => stay
  }

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

  case class Pair(async: ActorRef, block: ActorRef)

  sealed trait State
  case object Connect extends State
  case object Login extends State
  case object Enter extends State
  case object Configure extends State
  case object Ready extends State
  case object Run extends State
  case object Throttle extends State

  val EOM = "fics% "
}
