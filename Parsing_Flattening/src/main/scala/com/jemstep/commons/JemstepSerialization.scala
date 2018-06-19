package com.jemstep.commons

import java.io.ByteArrayOutputStream

import com.jemstep.commons.Config.default_avro_reuse
import org.apache.avro.Schema
import org.apache.avro.Schema.Type._
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io._
import org.apache.avro.specific.{SpecificDatumReader, SpecificDatumWriter}
import org.apache.kafka.common.header.{Headers}
import org.apache.kafka.common.serialization.{ExtendedDeserializer, ExtendedSerializer}
import com.jemstep.commons.Util._

class JemstepKafkaAvroSerializer extends ExtendedSerializer[GenericRecord] {

  def serialize(topic: String, headers: Headers, record: GenericRecord): Array[Byte] = {
    val writer:SpecificDatumWriter[GenericRecord] = new SpecificDatumWriter[GenericRecord](record.getSchema)
    val out:ByteArrayOutputStream = new ByteArrayOutputStream()
    val encoder: BinaryEncoder = EncoderFactory.get().binaryEncoder(out, default_avro_reuse)
    writer.write(record, encoder)
    encoder.flush()
    out.close()
    out.toByteArray()
  }
  
  def close(): Unit = {}
  
  def configure(configs: java.util.Map[String, _], isKey: Boolean): Unit = ???
  
  def serialize(topic: String, record: GenericRecord): Array[Byte] = ???

}

class JemstepKafkaAvroDeserializer extends ExtendedDeserializer[GenericRecord] {

  def deserialize(topic: String, headers: Headers, data: Array[Byte]): GenericRecord = {
    val schemaFullName = headerValue("schema", headers)
    val schema: Schema = SchemaIO.findSchema(schemaFullName).getOrElse(Schema.create(STRING))
    val reader: DatumReader[GenericRecord] = new SpecificDatumReader[GenericRecord](schema)
    val decoder: Decoder = DecoderFactory.get().binaryDecoder(data, default_avro_reuse)
    reader.read(default_avro_reuse, decoder)
  }
  
  def close(): Unit = {}

  def configure(configs: java.util.Map[String, _], isKey: Boolean): Unit = ???
  
  def deserialize(topic: String, data: Array[Byte]): GenericRecord = ???

}

