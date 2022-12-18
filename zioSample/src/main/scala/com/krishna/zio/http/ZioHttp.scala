package com.krishna.zio.http

import zhttp.http._
import zhttp.service.Server
import zio._

object ZioHttp extends ZIOAppDefault {

  val port: Int = 9000

  val app: Http[Any, Nothing, Request, Response] = Http.collect[Request] {
    case Method.GET -> !!                   => Response.text("Home page!!")
    case Method.GET -> !! / "owls"          => Response.text("Hoot!!")
    case Method.GET -> "" /: "owls" /: name =>
      Response.text(s"$name says: Hoot!")
  }

  /*
    / is a path delimiter that starts extraction of the left-hand side (a left associative operator).
    /: is a path delimiter that starts extraction of the right-hand side (a right associative operator),
      and can match paths partially.

     you can’t use / and /: in the same case statement, as left- and right-associative operators
     with same precedence may not be mixed.
   */

  // UHttpApp is Type alias of Http[Any, Nothing, Request, Response]
  val zApp: UHttpApp = Http.collectZIO[Request] {
    case Method.POST -> !! / "owls" =>
      Random.nextIntBetween(3, 6).map(n => Response.text("Hoot! " * n))
  }

  /*
    There are four operators to compose these “HTTP applications”: ++, <>, >>> and <<<,
    and the behavior of each is as described from the official documentation.
   */
  val combined: Http[Any, Nothing, Request, Response] = app ++ zApp

  val program: ZIO[Any, Throwable, ExitCode] = for {
    _ <- Console.printLine(s"Starting server on http://localhost:$port")
    _ <- Server.start(port, combined)
  } yield ExitCode.success

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = program
}
