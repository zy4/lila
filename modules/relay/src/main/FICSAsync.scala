package lila.relay

import akka.actor._

private[relay] final class FICSAsync(c: FICS.Config)
    extends FICS[FICSAsync.NoState.type](c) {

  import FICS._
  import FICSAsync._
  import Telnet._
  import GameEvent._

  def afterConfigure = goto(Ready)

  startWith(Connect, NoState)

  when(Ready) {
    case Event(Observe(ficsId), _) =>
      send(s"observe $ficsId")
      stay
    case Event(Unobserve(ficsId), _) =>
      send(s"unobserve $ficsId")
      stay
    case Event(in: In, _) =>
      log(handle(in))
      stay
    case Event(wut, _) =>
      println(s"FICS ERR unhandled $wut")
      stay
  }

  def handle(in: In): List[String] = in.lines.foldLeft(List.empty[String]) {
    case (lines, line) =>
      Move(line) orElse Clock(line) orElse Resign(line) orElse Draw(line) orElse Limited(line) map {
        case move: Move =>
          context.parent ! move
          lines
        case clock: Clock =>
          context.parent ! clock
          lines
        case resign: Resign =>
          context.parent ! resign
          lines
        case draw: Draw =>
          context.parent ! draw
          lines
        case Limited =>
          println(s"FICS ERR $line")
          lines
      } getOrElse {
        line :: lines
      }
  }.reverse
}

object FICSAsync {

  case object NoState

  case class Observe(ficsId: Int)
  case class Unobserve(ficsId: Int)

  case object Limited {
    val R = "You are already observing the maximum number of games"
    def apply(str: String): Option[Limited.type] = str contains R option (Limited)
  }
}
