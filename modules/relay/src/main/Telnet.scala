package lila.relay

import akka.actor.{ Actor, ActorRef, Props }
import akka.io.{ IO, Tcp }
import akka.util.ByteString
import java.net.InetSocketAddress

// private[relay] final class Telnet(
final class Telnet(
    remote: InetSocketAddress,
    listener: ActorRef) extends Actor {

  import Tcp._
  import context.system

  IO(Tcp) ! Connect(remote, options = List(
    SO.TcpNoDelay(false)
  ))

  def receive = {
    case CommandFailed(_: Connect) =>
      listener ! "connect failed"
      context stop self

    case Connected(remote, local) =>
      val connection = sender()
      connection ! Register(self)
      listener ! Telnet.Connection({ str =>
        // println(s"TELNET> $str")
        connection ! Write(ByteString(s"$str\n"))
      })
      context become LineMode.receive(connection)
  }

  def baseMode(connection: ActorRef): Receive = {
    case Telnet.EnterBlockMode => context become BlockMode.receive(connection)
    case Telnet.BufferUntil(str) =>
      BufferMode.eom = str
      context become BufferMode.receive(connection)
    case CommandFailed(w: Write) => listener ! Telnet.WriteFailed
    case "close"                 => connection ! Close
    case str: String             => connection ! Write(ByteString(s"$str\n"))
    case _: ConnectionClosed =>
      listener ! Telnet.Close
      context stop self
  }

  object LineMode {
    def receive(connection: ActorRef): Receive = ({
      case Received(data) =>
        val chunk = data decodeString "UTF-8"
        listener ! Telnet.In(chunk)
    }: Receive) orElse baseMode(connection)
  }

  object BufferMode {
    var eom: String = _
    val buffer = new collection.mutable.StringBuilder
    def receive(connection: ActorRef): Receive = ({
      case Received(data) =>
        val chunk = data decodeString "UTF-8"
        // println(chunk(0).toInt, chunk)
        buffer append chunk
        if (buffer endsWith eom) {
          listener ! Telnet.In(buffer.toString)
          buffer.clear()
        }
    }: Receive) orElse baseMode(connection)
  }

  object BlockMode {

    import Telnet.Block._

    val buffers = new collection.mutable.Map[Int, collection.mutable.StringBuilder]

    def receive(connection: ActorRef): Receive = ({
      case Received(data) =>
        data.foldLeft(List.empty[Frame])
        val chunk = data decodeString "UTF-8"
      // println(chunk(0).toInt, chunk)
      // buffer append chunk
      // if (buffer endsWith bufferUntil) {
      //   listener ! Telnet.In(buffer.toString)
      //   buffer.clear()
      // }
    }: Receive) orElse baseMode(connection)
  }
}

object Telnet {

  case class In(data: String) {
    lazy val lines: List[String] = data.split(Array('\r', '\n')).toList.map(_.trim).filter(_.nonEmpty)
    def last: Option[String] = lines.lastOption
  }
  case class Connection(send: String => Unit)
  case class BufferUntil(str: String)
  case object EnterBlockMode
  case object ConnectFailed
  case object WriteFailed
  case object Close

  object Block {
    val START = 21.toByte
    val SEPARATOR = 22.toByte
    val END = 23.toByte
    val POSE_START = 24.toByte
    val POSE_END = 25.toByte

    sealed trait Frame
    object Frame {
      case class Chunk(str: String) extends Frame
      case object Start extends Frame
      case object End extends Frame
    }
  }
}
