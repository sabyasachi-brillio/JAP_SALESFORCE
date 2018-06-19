import java.io.File

import com.jemstep.commons._
import org.apache.avro.Schema
import org.apache.avro.Schema.Parser
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.specs2.mutable.Specification

object JemstepSerializationSpecFixture {
  trait JemstepAvroSchemaT {
    val file: File
    def schema: Schema
  }

  object JemstepAvroSchemaFile {
    def apply(filename: String): JemstepAvroSchemaFile = JemstepAvroSchemaFile(new File(s"./schema/$filename.avsc"))
  }
  case class JemstepAvroSchemaFile(file: File) extends JemstepAvroSchemaT {
    def schema = new Parser().parse(file)
    def fullname = schema.getFullName
  }

  val backTestMetrics = JemstepAvroSchemaFile("backtest_metrics")
  val goal = JemstepAvroSchemaFile("goal")
  val modelQuestionnaire = JemstepAvroSchemaFile("model_questionnaire")
  val portfolioByBroker= JemstepAvroSchemaFile("portfolio_by_broker")
  val userIdentified = JemstepAvroSchemaFile("user_identified")
  val unknown = JemstepAvroSchemaFile("user_identified")

  def findSchema(schemaFullname: String): Option[JemstepAvroSchemaFile] = {
    val listOfSchemas: Seq[JemstepAvroSchemaFile] = List(backTestMetrics,goal,modelQuestionnaire,portfolioByBroker,userIdentified)
    listOfSchemas.find((schema: JemstepAvroSchemaFile) => schema.fullname == schemaFullname)
  }
}

class JemstepSerializationSpec extends Specification  {

  "JemstepSerialization" should {

    "get correct names for the schemas" in {
      SchemaIO.findSchema("com.jemstep.model.assetliability.BacktestMetrics") must_== Some(JemstepSerializationSpecFixture.backTestMetrics.schema)
      SchemaIO.findSchema("com.jemstep.model.goal.Goal") must_== Some(JemstepSerializationSpecFixture.goal.schema)
      SchemaIO.findSchema("com.jemstep.model.questionnaire.ModelQuestionnaire") must_== Some(JemstepSerializationSpecFixture.modelQuestionnaire.schema)
      SchemaIO.findSchema("com.jemstep.model.portfolio.PortfolioByBroker") must_== Some(JemstepSerializationSpecFixture.portfolioByBroker.schema)
      SchemaIO.findSchema("com.jemstep.model.events.shared.UserIdentified") must_== Some(JemstepSerializationSpecFixture.userIdentified.schema)
    }

    "serialize and deserialize generic records" in {
      val serializer = new JemstepKafkaAvroSerializer
      val deserializer = new JemstepKafkaAvroDeserializer

      val serializedList = DataIO.streamFromFile(new File("./data/test_data.avro"))
        .take(1)
        .map { _.get("userIdentified") }
        .collect { case (userIdentified:GenericRecord) => userIdentified }
        .map { record =>
          val fullName = record.getSchema.getFullName
          println(s"\n $fullName Record: $record\n")
          val pRecord = new ProducerRecord[Array[Byte], GenericRecord]("investor", record)
          val headers = pRecord.headers().add("schema", fullName.getBytes)
          (record, headers, serializer.serialize("",headers,record))
      }.toList

      serializedList.map { case (originalGenericRecord, headers,genericRecordByteArray) =>
          originalGenericRecord must_== deserializer.deserialize("", headers, genericRecordByteArray)
      }
    }
  }
}
