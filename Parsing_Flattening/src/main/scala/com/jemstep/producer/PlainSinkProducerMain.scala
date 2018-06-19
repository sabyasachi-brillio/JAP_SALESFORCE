package com.jemstep.producer

import java.io.{File}

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.jemstep.commons.Config._
import com.jemstep.commons.Util._
import com.jemstep.commons.{DataIO, JemstepKafkaAvroSerializer}
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.avro.util.Utf8

import scala.concurrent.Await
import scala.concurrent.duration._

object PlainSinkProducerMain extends App {

  implicit val system = ActorSystem("PlainSinkProducerMain")
  implicit val materializer = ActorMaterializer()

  val producerSettings = ProducerSettings(system, new ByteArraySerializer, new JemstepKafkaAvroSerializer)
    .withBootstrapServers(kafka_server)

  println(s"${getClass.getSimpleName} sending messages to $kafka_server")

  val done = Source(DataIO.streamFromFile(new File("./data/test_data.avro")))
    .map { record => (
      record.get("userId"),
      record.get("organizationId"), 
      record.get("userIdentified"),
      record.get("modelQuestionnaire"),
      record.get("goal"),
      record.get("portfolioByBroker"),
      record.get("backtestMetrics")
    ) }
    .collect { case (
        userId: Utf8,
        organizationId: Utf8,
        userIdentified: GenericRecord,
        modelQuestionnaire: GenericRecord,
        goal: GenericRecord,
        portfolioByBroker: GenericRecord,
        backtestMetrics: GenericRecord) => 
      (userId, organizationId, List(userIdentified, modelQuestionnaire, goal, portfolioByBroker, backtestMetrics))
    }
    .flatMapConcat { case (userId, organizationId, records) =>      
      val metaRecords = records.map { record => (userId.toString, organizationId.toString, record) }
      Source(metaRecords)
    }
    .map { case (userId, organizationId, record) =>
      
      val fullName = record.getSchema.getFullName
      println(s"\n Produced User: $userId, Org: $organizationId, Schema: $fullName\n")
      
      val pRecord = new ProducerRecord[Array[Byte], GenericRecord]("investor", userId.getBytes, record)
      discard(pRecord.headers().add("userId", userId.getBytes))
      discard(pRecord.headers().add("organizationId", organizationId.getBytes))
      discard(pRecord.headers().add("schema", fullName.getBytes))
      discard(pRecord.headers().add("operation", "UPSERT".getBytes))
      pRecord
    }
    .runWith(Producer.plainSink(producerSettings))

  println("Waiting for producer... ")
  discard(Await.result(done, 5.seconds))
  println("Waiting for producer... done")

  print("Waiting for actor termination... ")
  discard(Await.result(system.terminate, 5.seconds))
  println("done.")

  println("Shutting down JVM")
  System.exit(0)

}
