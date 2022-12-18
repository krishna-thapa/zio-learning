package com.krishna.zio.streams

import zio.Cause.{Die, Fail}
import zio.stream.{ZSink, ZStream}
import zio.{ZIO, ZIOAppDefault}

import java.io.{InputStream, IOException}

object ZioStreamError extends ZIOAppDefault {

  /** Handling a failure in ZIO with errors handling
    */
  val failStream: ZStream[Any, String, Int] =
    ZStream(1, 2) ++ ZStream.fail("Abstract reason") ++ ZStream(4, 5)

  class FakeInputStream[T <: Throwable](limit: Int, failAt: Int, failWith: => T)
      extends InputStream {
    val data: Array[Byte] = "0123456789".getBytes
    var counter: Int      = 0
    var index: Int        = 0

    override def read(): Int = {
      if (counter == limit) -1 // Stream end
      else if (counter == failAt) throw failWith
      else {
        val result: Byte = data(index)
        index = (index + 1) % data.length
        counter += 1
        result
      }
    }
  }

  val sink: ZSink[Any, Nothing, String, Nothing, String] =
    ZSink.collectAll[String].map(chunk => chunk.mkString("-"))

  // 99 is higher than the length of our data, so we won't fail
  val nonFailingStream: ZStream[Any, IOException, String] =
    ZStream
      .fromInputStream(
        new FakeInputStream(12, 99, new IOException("Something went bad")),
        chunkSize = 1
      )
      .map(byte => new String(Array(byte)))

  // will fail, and the error type matches ZStream error channel
  val failingStream: ZStream[Any, IOException, String] =
    ZStream
      .fromInputStream(
        new FakeInputStream(10, 5, new IOException("Something went bad")),
        chunkSize = 1
      )
      .map(byte => new String(Array(byte)))

  // will fail, but the error does not match the ZStream error channel
  val defectStream: ZStream[Any, IOException, String] =
    ZStream
      .fromInputStream(
        new FakeInputStream(10, 5, new IndexOutOfBoundsException("")),
        chunkSize = 1
      )
      .map(b => new String(Array(b)))

  // When recovering, we will use this ZStream as the fall-back
  val recoveryStream: ZStream[Any, Throwable, String] =
    ZStream("a", "b", "c")

  // recover with orElse/orElseEither
  // elements of the original stream were processed up to the point of failure,
  // and then elements of the recovery stream started to be processed afterwards
  val recoveredEffect: ZStream[Any, Throwable, String] =
    failingStream.orElse(recoveryStream)

  // all elements from the original stream = Left, all elements from the fallback = Right
  val recoveredEffectWithEither
      : ZStream[Any, Throwable, Either[String, String]] =
    failingStream.orElseEither(recoveryStream)
  val debugRecoveredEffectWithEither =
    recoveredEffectWithEither.run(ZSink.foreach(ZIO.succeed(_).debug("Either")))

  // catch, catchSomeCause, catchAll, catchAllCause
  val caughtErrors = failingStream.catchSome { case _: IOException =>
    recoveryStream
  }

  val caughtErrors2 = defectStream.catchSomeCause {
    case Fail(e: IOException, _)              => recoveryStream
    case Die(e: IndexOutOfBoundsException, _) => recoveryStream
  }

  // to exhaustively recover from errors via E => ZStream[R1, E2, A1]) (and causes),
  val caughtErrors3 = failingStream.catchAll {
    case _: IOException => recoveryStream
    case _              => ZStream("x", "y", "z")
  }

  // processed values up until the failures as Rights, ending with a Left of the failure
  val errorContained: ZStream[Any, Nothing, Either[IOException, String]] =
    failingStream.either
  val errorContainedDebug                                                =
    errorContained.run(ZSink.collectAll[Either[IOException, String]]).debug

  // Quick to get the result
  val finalResult = failingStream.either.collectRight.run(sink)

  override def run = recoveredEffect.run(sink).debug("Debug")
}
