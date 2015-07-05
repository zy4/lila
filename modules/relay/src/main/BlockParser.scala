package lila.relay

import akka.actor._

private[relay] final class BlockParser
    extends Actor with LoggingFSM[BlockParser.State, Option[BlockParser.Block]] {

  import BlockParser._

  startWith(State.Ready, None)

  when(State.Ready) {
    case Event(Begin(id), _) => goto(State.Start) using Some(Block(id, ""))
  }

  when(State.Start) {
    case Event(b: Byte, Some(block)) =>
  }

}

object BlockParser {

  case class Begin(id: Int)
  case class Data(bs: ByteString)

  sealed trait State
  object State {
    case object Ready extends State
    case object Start
  }

  case class Block(id: Int, buffer: String)
}
