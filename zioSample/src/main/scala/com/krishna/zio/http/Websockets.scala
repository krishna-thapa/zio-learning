package com.krishna.zio.http

import zhttp.http._
import zhttp.service.ChannelEvent.UserEvent.{
  HandshakeComplete,
  HandshakeTimeout
}
import zhttp.service.ChannelEvent.{
  ChannelRead,
  ChannelUnregistered,
  UserEventTriggered
}
import zhttp.service._
import zhttp.socket._
import zio._
import zio.stream.ZStream

object Websockets extends ZIOAppDefault {

  val port: Int = 9002

  val content: String =
    "All work and no Play framework makes Jack a dully boy\n" * 10000

  val data: Chunk[Byte] = Chunk.fromArray(content.getBytes(HTTP_CHARSET))

  val stream: Http[Any, Nothing, Request, Response] = Http.collect[Request] {
    case Method.GET -> !! / "stream" =>
      Response(
        status = Status.Ok,
        headers = Headers.contentLength(data.length.toLong),
        body = Body.fromStream(ZStream.fromChunk(data))
      )
  }

  val sarcastically: String => String = text =>
    text.toList.zipWithIndex.map { case (c, i) =>
      if (i % 2 == 0) c.toUpper else c.toLower
    }.mkString

  val wsLogic: Http[Any, Throwable, WebSocketChannelEvent, Unit] =
    Http.collectZIO[WebSocketChannelEvent] {
      case ChannelEvent(channel, ChannelRead(WebSocketFrame.Text(msg))) =>
        channel.writeAndFlush(WebSocketFrame.text(sarcastically(msg)))
      case ChannelEvent(_, UserEventTriggered(event))                   =>
        event match {
          case HandshakeComplete => ZIO.logInfo("Connection started!")
          case HandshakeTimeout  => ZIO.logInfo("Connection failed!")
        }
      case ChannelEvent(_, ChannelUnregistered)                         =>
        ZIO.logInfo("Connection closed!")
    }

  val wsApp: Http[Any, Nothing, Request, Response] = Http.collectZIO[Request] {
    case Method.GET -> !! / "chat" => wsLogic.toSocketApp.toResponse
  }

  val program: ZIO[Any, Throwable, ExitCode] = for {
    _ <- Console.printLine(s"Starting server on http://localhost:$port")
    _ <- Server.start(port, stream ++ wsApp)
  } yield ExitCode.success

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = program
}
