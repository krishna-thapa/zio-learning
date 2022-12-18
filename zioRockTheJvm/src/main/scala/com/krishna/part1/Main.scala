package com.krishna.part1

import Main.Environment
import zio.Console.printLine
import zio.{ Scope, ZIO, ZIOAppArgs, ZIOAppDefault }

object Main extends ZIOAppDefault:
  override def run: ZIO[Environment with ZIOAppArgs with Scope, Any, Any] =
    printLine("Welcome to your first ZIO app!")