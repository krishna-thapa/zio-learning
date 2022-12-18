package com.krishna.zio.streams

import zio.Console.printLine
import zio.stream.{ ZPipeline, ZSink, ZStream }
import zio.{ Chunk, Scope, ZIO, ZIOAppArgs, ZIOAppDefault }

object ZioStreamIntro extends ZIOAppDefault {

  // effects
  val aSuccess: ZIO[Any, Nothing, Int] = ZIO.succeed(42)

  // A ZStream represents the source of data in your work flow.
  // ZStreams = "collection" (source) of 0 or more (maybe infinite) elements
  val aStream: ZStream[Any, Nothing, Int] = ZStream.fromIterable(1 to 10)
  val intStream = ZStream(1, 2, 3, 4, 5, 6, 7)
  val stringStream: ZStream[Any, Nothing, String] = intStream.map(_.toString)

  // sink = destination of your elements
  /*
    At the opposite end of our stream, we have a ZSink, with the signature:
      ZSink[R, E, I, L, Z]. R, and E are as described above.
      It will consume elements of type I, and produce a value of type Z along
      with any elements of type L that may be left over.
   */
  val sum: ZSink[Any, Nothing, Int, Nothing, Int] = ZSink.sum[Int]

  val take5: ZSink[Any, Nothing, Int, Int, Chunk[Int]] = ZSink.take(5)
  val take5Map: ZSink[Any, Nothing, Int, Int, Chunk[String]] = take5.map(_.map(_.toString))

  // leftovers
  // first chunk is output and second chunk is leftovers
  val take5Leftovers: ZSink[Any, Nothing, Int, Nothing, (Chunk[String], Chunk[Int])] = take5Map.collectLeftover
  val take5Ignore: ZSink[Any, Nothing, Int, Nothing, Chunk[String]] = take5Map.ignoreLeftover

  // contramap:  to convert the input type appropriately
  val take5String: ZSink[Any, Nothing, String, Int, Chunk[Int]] = take5.contramap[String](_.toInt)
  val take5Dimap: ZSink[Any, Nothing, String, Int, Chunk[String]] = take5.dimap[String, Chunk[String]](_.toInt, _.map(_.toString))
  // ZStream[String] -> ZSink[Int].contramap(_.toInt)
  // same as
  // ZStream[String].map(_.toInt) -> ZSink[Int]

  val zio: ZIO[Any, Nothing, Int] = intStream.run(sum)

  // ZPipeline: converts one ZStream to another ZStream
  // ZPipeline[Env, Err, In, Out] = ZStream[Env, Err, In] => ZStream[Env, Err, Out].
  val businessLogic: ZPipeline[Any, Nothing, String, Int] =
  ZPipeline.map[String, Int](_.toInt)

  val filterLogic: ZPipeline[Any, Nothing, Int, Int] =
    ZPipeline.filter[Int](_ > 3)

  val appLogic: ZPipeline[Any, Nothing, String, Int] =
    businessLogic >>> filterLogic

  val zio2: ZIO[Any, Nothing, Int] =
    stringStream.via(appLogic).run(sum)

  override def run: ZIO[Environment with ZIOAppArgs with Scope, Any, Any] = {
    printLine("Welcome to your first ZIO app!")
    zio2.debug
  }
}