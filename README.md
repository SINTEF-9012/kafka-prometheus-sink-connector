# prometheus-mqtt-sink-connector
A connector plugin to use with Kafka's Connect API. It contains functionality to listen to Kafka topics and publish to a Prometheus HTTP endpoint, which can then be scrapped by Prometheus.

## Prerequisites
- Java version >8 installed to run Kafka and this source-connector. Check if Java is installed, and which version by running `java -version` in your terminal. We use `openjdk version "11.0.6" 2020-01-14`.
- Linux. We are running this setup on Ubuntu 16.4.
- Maven. Check if maven is installed properly with running `mvn -v` in your terminal. We use Maven 3.6.0

## Download Kafka binaries
Download a binary Kafka release from https://kafka.apache.org/downloads. We work with the compressed download:
>kafka_2.13-2.4.1.tgz
Extract the download to your desired destination, here termed _"path-to-kafka"_.

## Zookeeper
About Zookeeper:
>"Zookeeper is a top-level software developed by Apache that acts as a centralized service and is used to maintain naming and configuration data and to provide flexible and robust synchronization within distributed systems. Zookeeper keeps track of status of the Kafka cluster nodes and it also keeps track of Kafka topics, partitions etc.
Zookeeper it self is allowing multiple clients to perform simultaneous reads and writes and acts as a shared configuration service within the system. The Zookeeper atomic broadcast (ZAB) protocol i s the brains of the whole system, making it possible for Zookeeper to act as an atomic broadcast system and issue orderly updates." [Cloudkarafka](https://www.cloudkarafka.com/blog/2018-07-04-cloudkarafka_what_is_zookeeper.html)

**Start Zookeeper**
```
"path-to-kafka"/kafka_2.13-2.4.1/bin/zookeeper-server-start.sh "path-to-kafka"/kafka_2.13-2.4.1/config/zookeeper.properties
```
P.S. The default properties of zookeeper.properties works well for this tutorial's purpose. It will start Zookeeper on the default port `2181`.

### Kafka Broker
As mentioned, we will only kick up a single instance Kafka Broker. The Kafka Broker will use `"path-to-kafka"/kafka_2.13-2.4.1/config/server.properties`, and it could be worth checking that
```
zookeeper.connect=localhost:2181
```
or set according to your custom configuration in `zookeeper.properties`.

**Start Kafka Broker**
```
"path-to-kafka"/kafka_2.13-2.4.1/bin/kafka-server-start.sh "path-to-kafka"/kafka_2.13-2.4.1/config/server.properties
```

**Create Kafka Topic**
```
"path-to-kafka"/kafka_2.13-2.4.1/bin/kafka-topics --create --bootstrap-server 127.0.0.1:9092 --replication-factor 1 --partitions 1 --topic test
```

### Kafka Connect
The Kafka Connect API is what we utilise as a framework around our connectors, to handle scaling, polling from Kafka, work distribution etc. Kafka Connect can run as _connect-standalone_ or as _connect-distributed_. The _connect-standalone_ is engineered for demo and test purposes, as it cannot provide fallback in a production environment.

**Start Kafka Connect**
Follow the respective steps below to start Kafka Connect in preferred mode.

_Connect in general_
Build this java maven project, but navigating to root `kafka-prometheus-sink-connector` in a terminal and typing:
```
mvn install
```
`Copy the kafka-prometheus-source-connector-"version".jar` from your maven target directory to the directory `/usr/share/java/kafka`:

```
sudo mkdir /usr/share/java/kafka
sudo cp ./target/*with-dependencies.jar /usr/share/java/kafka/.
```

__*Connect Distributed*__
Kafka Connect Distributed does not need properties files to configure connectors. It uses the Kafka Connect REST-interface.

5. Uncomment `plugin.path` in `"path-to-kafka"/kafka_2.13-2.4.1/config/connect-distributed.properties`, so that it is set to
```
plugin.path=/usr/share/java,/usr/local/share/kafka/plugins,/usr/local/share/java/
```
and that `rest.port` so that it is set to
```
rest.port=19005
```
which will help one to avoid some "bind" exceptions. This will be the port for the Connect REST-interface.

6. Start _Connect Distributed_ with by typing (this may take a minute or two):
```
"path-to-kafka"/kafka_2.13-2.4.1/bin/connect-distributed.sh "path-to-kafka"/kafka_2.13-2.4.1/config/connect-distributed.properties
```

7. Start our connector by posting the following command to the Connect REST-interface:
```
curl -s -X POST -H 'Content-Type: application/json' http://127.0.0.1:19005/connectors -d '{"name":"prometheus-sink-connector","config":{"connector.class":"com.sintef.asam.PrometheusSinkConnector","tasks.max":"1","topics.regex":"test*","value.converter":"org.apache.kafka.connect.converters.ByteArrayConverter"}}'
```

8. Inspect the terminal where you started Conncet Distributed, and after the connector seem to have successfully started, check the existence by typing:
```
curl 'Content-Type: application/json' http://127.0.0.1:19005/connectors
```
where the response is an array with connectors by name.

10. Then publish something to the test topic on the Kafka broker. Start a Kafka console producer in yet a new terminal window:
```
"path-to-kafka"/kafka_2.13-2.4.1/bin/kafka-console-producer --broker-list 127.0.0.1:9092 --topic test
```
Type the following in the _kafka-console-producer_:
```
{"header": {"protocolVersion":1, "messageID":0, "stationID":0}, "cam":{"speedValue":110, "headingValue":5}}
```

Note that the sink connector is made to receive JSON formatted messages. The JSON also needs to have a key/value pair where key is equal to the property `mqtt.connector.mqtt_topic_key`.
