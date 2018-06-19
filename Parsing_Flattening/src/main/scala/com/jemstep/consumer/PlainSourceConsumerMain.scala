package com.jemstep.consumer

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.jemstep.commons.Config._
import com.jemstep.commons.JemstepKafkaAvroDeserializer
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.common.serialization.ByteArrayDeserializer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import com.jemstep.commons.Util._
import com.jemstep.commons.JSONCSVWriter._

object PlainSourceConsumerMain extends App {

  implicit val system: ActorSystem = ActorSystem("PlainSourceConsumerMain")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val consumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new JemstepKafkaAvroDeserializer)
  .withBootstrapServers(kafka_server)
  .withGroupId("PlainSourceConsumer")
  .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  println(s"${getClass.getSimpleName} receiving messages from $kafka_server")
  //val timeStamp = "timestamp"//GetDate.getTimeStamp()
  val source: Source[Done, Consumer.Control] = Consumer.committableSource(consumerSettings, Subscriptions.topics("investor"))
    .mapAsync(1) { msg =>
      DB.save(msg.record).flatMap { _done =>
        msg.committableOffset.commitScaladsl()
      }
    }
  //println("after source :: before future")
  val done: Future[Done] = source.runWith(Sink.ignore)
  //println("after source :: after future")
}

object DB {

  def save(record: ConsumerRecord[Array[Byte], GenericRecord]): Future[Done] = {
    val userId: String = headerValue("userId", record.headers)
    val organizationId: String = headerValue("organizationId", record.headers)
    val operation: String = headerValue("operation", record.headers)
    val recordSchemaFullName: String = record.value.getSchema.getFullName
    println(record)
    println(s"DB.$operation User: $userId, Org: $organizationId, Schema: $recordSchemaFullName\n")
	recordSchemaFullName match {
		case "com.jemstep.model.portfolio.PortfolioByBroker" => writeAccountPE(record.value.toString, userId)
		case "com.jemstep.model.goal.Goal" => writeQuestionnariePE(record.value.toString, userId)
		case _ => println("nothing")
	}
    Future.successful(Done)
  }

}



