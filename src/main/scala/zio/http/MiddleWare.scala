package zio.http

import zhttp.http._
import zhttp.http.middleware.Cors.CorsConfig
import zhttp.service.Server
import zio._

object MiddleWare extends ZIOAppDefault {

  val port: Int = 9001

  val app: UHttpApp = Http.collect[Request] {
    case Method.GET -> !! / "owls"          => Response.text("Hoot!")
    case Method.GET -> "" /: "owls" /: name =>
      Response.text(s"$name says: Hoot!")
  } @@ Middleware.csrfGenerate()

  val zApp: UHttpApp =
    Http.collectZIO[Request] { case Method.POST -> !! / "owls" =>
      Random.nextIntBetween(3, 6).map(n => Response.text("Hoot! " * n))
    } @@ Middleware.csrfValidate()

  val authApp: UHttpApp = Http.collect[Request] {
    case Method.GET -> !! / "secret" / "owls" =>
      Response.text("The password is 'Hoot!'")
  } @@ Middleware.basicAuth("hooty", "tootie")

  // NOTE: Have to be careful on combining the HTTP app
  // If we add the authApp before then all the rest of the app needs auth for it
  val combined = app ++ authApp ++ zApp

  // CORS
  val config: CorsConfig = CorsConfig(
    anyOrigin = false,
    anyMethod = false,
    allowedOrigins = s => s.equals("localhost"),
    allowedMethods = Some(Set(Method.GET, Method.POST))
  )

  val allMiddleware
      : Middleware[Any, Throwable, Request, Response, Request, Response] =
    Middleware.cors(config) ++ Verbose.log

  val wrapped: Http[Any, Throwable, Request, Response] =
    combined @@ allMiddleware

  /*
    CSRF
    When you logged in to online store
    browse plugin can display a button => when clicked, trigger a form submission
    CSRF token + cookie = double submit cookie
   */

  val program: ZIO[Any, Throwable, ExitCode] = for {
    _ <- Console.printLine(s"Starting server on http://localhost:$port")
    _ <- Server.start(port, wrapped)
  } yield ExitCode.success

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = program
}
