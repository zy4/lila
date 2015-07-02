package lila.relay

import akka.actor._
import akka.pattern.pipe
import scala.concurrent.duration._

import lila.hub.actorApi.map.Tell

private[relay] final class FICSBlock(c: FICS.Config)
    extends FICS[Option[FICSBlock.Request]](c) with Stash {

  import FICS._
  import FICSBlock._
  import Telnet._
  import command.Command

  var counter = 1

  def increment = {
    counter = counter + 1
    counter
  }

  def afterConfigure = {
    send("iset block")
    goto(Throttle)
  }

  startWith(Connect, none)

  when(Ready) {
    case Event(cmd: Command, _) =>
      send(cmd.str)
      goto(Run) using Request(cmd, sender).some
  }

  when(Run, stateTimeout = 20 second) {
    case Event(in: In, Some(Request(cmd, replyTo))) =>
      val lines = in.lines
      cmd parse lines match {
        case Some(res) =>
          replyTo ! res
          goto(Throttle) using none
        case None =>
          log(lines)
          stay
      }
    case Event(StateTimeout, req) =>
      req.foreach { r =>
        r.replyTo ! Status.Failure(new Exception(s"FICS:Run timeout on ${r.cmd.str}"))
      }
      goto(Ready) using none
  }

  when(Throttle, stateTimeout = 500 millis) {
    case Event(StateTimeout, _) => goto(Ready) using none
  }

  whenUnhandled {
    case Event(_: command.Command, _) =>
      stash()
      stay
    case Event(in: In, _) =>
      log(in.lines)
      stay
  }

  onTransition {
    case _ -> Ready => unstashAll()
  }
}

object FICSBlock {

  case class Request(cmd: command.Command, replyTo: ActorRef)
}
