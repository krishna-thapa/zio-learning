package com.krishna.zio.http

import zhttp.http._
import zio._

/*
  request => contramap => http application => response => map => final response
            | -----------------middleware new http ------------|
 */
object Verbose {

  def log[R, E >: Throwable]
      : Middleware[R, E, Request, Response, Request, Response] = {
    new Middleware[R, E, Request, Response, Request, Response] {

      override def apply[R1 <: R, E1 >: E](
          http: Http[R1, E1, Request, Response]
      ): Http[R1, E1, Request, Response] = {
        http.contramapZIO[R1, E1, Request] { req =>
          for {
            _ <-
              Console.printLine(s"> ${req.method} ${req.path} ${req.version}")
            _ <- ZIO.foreach(req.headers.toList) { h =>
                   Console.printLine(s"> ${h._1}: ${h._2}")
                 }
          } yield req
        }
      }
        .mapZIO[R1, E1, Response] { res =>
          for {
            _ <- Console.printLine(s"< ${res.status}")
            _ <- ZIO.foreach(res.headers.toList) { h =>
                   Console.printLine(s"< ${h._1}: ${h._2}")
                 }
          } yield res
        }
    }
  }
}
