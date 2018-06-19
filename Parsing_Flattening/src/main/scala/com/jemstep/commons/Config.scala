package com.jemstep.commons

object Config {

  val kafka_host = System.getProperty("kafka.host", "kafka")
  val kafka_port = System.getProperty("kafka.port", "9092").toInt
  val kafka_server = s"$kafka_host:$kafka_port"

  // Looks like some libraries still insist on using null in weird and wonderful ways
  // https://www.infoq.com/presentations/Null-References-The-Billion-Dollar-Mistake-Tony-Hoare
  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  val default_avro_reuse = null

}
