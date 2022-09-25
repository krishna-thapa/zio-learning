package zio.streams

import zio.stream.{ ZSink, ZStream }
import zio.streams.ZioStreamError.nonFailingStream
import zio.streams.ZioStreamIntro.businessLogic
import zio.{ ExitCode, Queue, ZIO, ZIOAppDefault }

object ZioStreamFinal extends ZIOAppDefault {

  // https://blog.rockthejvm.com/zio-streams/#async-zstreams
  val program: ZIO[Any, Throwable, ExitCode] = for {
    queue <- Queue.unbounded[Int]
    producer <- nonFailingStream.via(businessLogic)
      .run(ZSink.fromQueueWithShutdown(queue))
      .fork
    result <- ZStream.fromQueue(queue)
      .run(ZSink.sum[Int]).debug("sum")
      .fork
    _ <- producer.join
    _ <- result.join
  } yield ExitCode.success

  val post1: String = "hello-word.md"
  val post1_content: Array[Byte] =
    """---
      |title: "Hello World"
      |tags: []
      |---
      |======
      |
      |## Generic Heading
      |
      |Even pretend blog posts need a #generic intro.
      |""".stripMargin.getBytes

  val post2: String = "scala-3-extensions.md"
  val post2_content: Array[Byte] =
    """---
      |title: "Scala 3 for You and Me"
      |tags: []
      |---
      |======
      |
      |## Cool Heading
      |
      |This is a post about #Scala and their re-work of #implicits via thing like #extensions.
      |""".stripMargin.getBytes

  val post3: String = "zio-streams.md"
  val post3_content: Array[Byte] =
    """---
      |title: "ZIO Streams: An Introduction"
      |tags: []
      |---
      |======
      |
      |## Some Heading
      |
      |This is a post about #Scala and #ZIO #ZStreams!
  """.stripMargin.getBytes

  val fileMap: Map[String, Array[Byte]] = Map(
    post1 -> post1_content,
    post2 -> post2_content,
    post3 -> post3_content

  override def run = ???
}
