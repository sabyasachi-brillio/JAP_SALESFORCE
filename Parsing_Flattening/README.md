# Kafka Consumer Example

An example project that produces and consumes some example Kafka messages

## Running the example:

1. `docker run -d -p 2181:2181 -p 9092:9092 --rm --env ADVERTISED_HOST=kafka --env ADVERTISED_PORT=9092 --name kafka -h kafka johnnypark/kafka-zookeeper`
2. `docker build -t "kafka-consumer-example:latest" ./docker/.`
3. `docker run --rm -it --name=kafka-consumer-example --link kafka --volume=${HOME}/jemstep/code/kafka-consumer-example/:/code/kafka-consumer-example kafka-consumer-example`
4. `cd /code/kafka-consumer-example`
5. `sbt test`
5. `sbt "runMain com.jemstep.producer.PlainSinkProducerMain"`
6. `sbt "runMain com.jemstep.consumer.PlainSourceConsumerMain"`

## Connecting to another Kafka host

The examples connect to "kafka:9092" by default, which should be running on the started docker container from step 1.

Use the properties "kafka.host" and "kafka.port" in order to specify your own host and/or port, for example:

```bash
$ sbt -Dkafka.host=127.0.0.1 -Dkafka.port=9092 "runMain com.jemstep.producer.PlainSinkProducerMain"
$ sbt -Dkafka.host=127.0.0.1 -Dkafka.port=9092 "runMain com.jemstep.consumer.PlainSourceConsumerMain"
```
