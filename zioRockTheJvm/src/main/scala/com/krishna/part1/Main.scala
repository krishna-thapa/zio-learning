package com.krishna.part1

import Main.Environment
import zio.Console.printLine
import zio.{ Scope, ZIO, ZIOAppArgs, ZIOAppDefault }
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors

object Main extends ZIOAppDefault:

  // Futures in New Scala without import of implicit global variable
  given ExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(8))
  val aFuture = Future {
    42
  }

  // Higher Kinded Type
  trait HigherKindedType[F[_]]
  val implementValue = new HigherKindedType[List] {}

  override def run: ZIO[Environment with ZIOAppArgs with Scope, Any, Any] =
    printLine("Welcome to your first ZIO app!")