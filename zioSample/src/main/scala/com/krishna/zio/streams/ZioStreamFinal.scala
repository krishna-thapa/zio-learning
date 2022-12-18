package com.krishna.zio.streams

import com.krishna.zio.streams.ZioStreamError.nonFailingStream
import com.krishna.zio.streams.ZioStreamIntro.businessLogic
import zio.json.EncoderOps
import zio.stream.{ZPipeline, ZSink, ZStream}
import zio.{ExitCode, Queue, ZIO, ZIOAppDefault, _}

import java.nio.charset.CharacterCodingException
import scala.util.matching.Regex

object ZioStreamFinal extends ZIOAppDefault {

  // https://blog.rockthejvm.com/zio-streams/#async-zstreams
  val program: ZIO[Any, Throwable, ExitCode] = for {
    queue    <- Queue.unbounded[Int]
    producer <- nonFailingStream
                  .via(businessLogic)
                  .run(ZSink.fromQueueWithShutdown(queue))
                  .fork
    result   <- ZStream
                  .fromQueue(queue)
                  .run(ZSink.sum[Int])
                  .debug("sum")
                  .fork
    _        <- producer.join
    _        <- result.join
  } yield ExitCode.success

  // =============================================================
  /*
    Objectives:
      1. Add tags to front matter
      2. change tags to links
      3. create a search file (search.json) with the tags associated to all the blog posts with that tag
   */

  val post1: String              = "hello-word.md"
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

  val post2: String              = "scala-3-extensions.md"
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

  val post3: String              = "zio-streams.md"
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
  )

  // Objective 1
  val hashFilter: String => Boolean = str =>
    str.startsWith("#") && str.count(_ == '#') == 1 && str.length > 2

  val punctRegex: Regex = """\p{Punct}""".r

  val parseHash: ZPipeline[Any, Nothing, String, String] =
    ZPipeline.filter(hashFilter)

  val removePunctuation: ZPipeline[Any, Nothing, String, String] =
    ZPipeline.map(str => punctRegex.replaceAllIn(str, " "))
  val lowercase: ZPipeline[Any, Nothing, String, String]         =
    ZPipeline.map(_.toLowerCase)

  val collectTagsPipleline =
    ZPipeline.utf8Decode >>>
      ZPipeline.splitLines >>>
      ZPipeline.splitOn(" ") >>>
      parseHash >>>
      removePunctuation >>>
      lowercase

  val collectTags: ZSink[Any, Nothing, String, Nothing, Set[String]] =
    ZSink.collectAllToSet

  val addTags: Set[String] => ZPipeline[Any, Nothing, String, String] =
    (tags: Set[String]) =>
      ZPipeline.map(contents =>
        contents.replace("tags: []", s"tags: [${tags.mkString(",")}]")
      )

  // Objective 2
  val addLinks: ZPipeline[Any, Nothing, String, String] = {
    ZPipeline.map { line =>
      line
        .split(" ")
        .map { word =>
          if (hashFilter(word)) {
            s"[$word](/tags/${punctRegex.replaceAllIn(word.toLowerCase, "")})"
          } else {
            word
          }
        }
        .mkString("")
    }
  }

  val addNewLine: ZPipeline[Any, Nothing, String, String] =
    ZPipeline.map(_ + "\n")

  // Objective 1 + 2
  val regeneratePost
      : Set[String] => ZPipeline[Any, CharacterCodingException, Byte, Byte] =
    tags =>
      ZPipeline.utf8Decode >>>
        ZPipeline.splitLines >>>
        addTags(tags) >>>
        addLinks >>>
        addNewLine >>>
        ZPipeline.utf8Encode

  def writeFile(
      dirPath: String,
      fileName: String
  ): ZSink[Any, Throwable, Byte, Byte, Long] =
    ZSink.fromFileName(dirPath + "/" + fileName)

  def autoTag(fileName: String, contents: Array[Byte]) =
    for {
      tags <- ZStream
                .fromIterable(contents)
                .via(collectTagsPipleline)
                .run(collectTags)
      _    <- Console.printLine(s"Generating file $fileName")
      _    <- ZStream
                .fromIterable(contents)
                .via(regeneratePost(tags))
                .run(writeFile("src/main/resources/zio-streams", fileName))
    } yield (fileName, tags)

  val autoTagAll: ZIO[Any, Throwable, Map[String, Set[String]]] =
    ZIO.foreach(fileMap) { case (filename, contents) =>
      autoTag(filename, contents)
    }

  // Map[filename, all the tags in that file]
  // Map[tag, all files with that tag]

  // Objective 3
  def createTagIndexFile(
      tagMap: Map[String, Set[String]]
  ): ZIO[Any, Throwable, Long] = {
    val searchMap: Map[String, Set[String]] =
      tagMap.values.toSet // Set[Set[String]]
        .flatten
        .map(tag =>
          tag -> tagMap.filter(_._2.contains(tag)).keys.toSet
        )                 // Set[(String, Set[String])]
        .toMap

    ZStream
      .fromIterable(searchMap.toJsonPretty.getBytes)
      .run(ZSink.fromFileName(s"src/main/resources/zio-streams/search.json"))
  }

  // Final call to all the changes
  val parseProgram: ZIO[Any, Throwable, Unit] = for {
    tagMap <- autoTagAll
    _      <- Console.printLine("Generating index file search.json")
    _      <- createTagIndexFile(tagMap)
  } yield ()

  override def run = parseProgram
}
