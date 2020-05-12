# prometheus-mqtt-sink-connector
A Kafka Sink Connector to listen to Kafka topics and publish to a Prometheus HTTP endpoint, which can then be scrapped by Prometheus.

See the test in `src/test/java` or the simple benchmark in `src/main/java/com/sintef/asam/impl/Util.java` to get an idea how to use it :-)

See also the script in `src/main/bash` to see how to deploy the connector with Kafka Connect REST API.

![Overview](https://github.com/SINTEF-9012/kafka-prometheus-sink-connector/blob/master/doc/overview.png?raw=true)
