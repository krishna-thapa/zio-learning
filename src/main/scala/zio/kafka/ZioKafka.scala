package zio.kafka

import zio.ZIO.console
import zio.json._
import zio.kafka.consumer.{ CommittableRecord, Consumer, ConsumerSettings, Offset, Subscription }
import zio.kafka.serde.Serde
import zio._
import zio.stream.ZStream

object ZioKafka extends ZIOAppDefault {

  val consumerSettings = ConsumerSettings(List("localhost:9092"))
    .withGroupId("updates-consumer") // settings for the kafka consumer

  val managedConsumer = Consumer.make(consumerSettings) // effectful resource

  val consumer = ZLayer.fromZIO(managedConsumer) // effectful DI

  // stream od strings, read the kafka topic
  val footballMatchesStream = Consumer.subscribeAnd(Subscription.topics("updates"))
    .plainStream(Serde.string, Serde.string)

  case class MatchPlayer(name: String, score: Int) {
    override def toString: String = s"$name: $score"
  }

  object MatchPlayer {
    implicit val encoder: JsonEncoder[MatchPlayer] = DeriveJsonEncoder.gen[MatchPlayer]
    implicit val decoder: JsonDecoder[MatchPlayer] = DeriveJsonDecoder.gen[MatchPlayer]
  }

  case class Match(players: Array[MatchPlayer]) {
    def score: String = s"${players(0)} - ${players(1)}"
  }

  object Match {
    implicit val encoder: JsonEncoder[Match] = DeriveJsonEncoder.gen[Match]
    implicit val decoder: JsonDecoder[Match] = DeriveJsonDecoder.gen[Match]
  }

  // json strings -> kafka -> jsons -> Match instances
  val matchSerde: Serde[Any, Match] = Serde.string.inmapM { string =>
    // deserialization
    ZIO.fromEither(string.fromJson[Match].left.map(new RuntimeException(_)))
  } { theMatch =>
    ZIO.attempt(theMatch.toJson)
  }

  val matchesStreams: ZStream[Any with Consumer, Throwable, CommittableRecord[String, Match]] =
    Consumer.subscribeAnd(Subscription.topics("updates"))
      .plainStream(Serde.string, matchSerde)

  val matchesPrintableStream: ZStream[Any with Consumer, Throwable, (String, Offset)] =
    matchesStreams
      .map(cr => (cr.value.score, cr.offset))
      .tap { case (score, _) => Console.printLine(s"| $score |") }

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = ???
}
