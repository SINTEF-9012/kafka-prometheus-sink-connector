#!/usr/bin/env bash

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd)
ROOT_DIR=$( cd "$DIR/../../.." && pwd)
LOG_DIR=$ROOT_DIR/target/log
KAFKA_HOME=/home/bmorin/dev/kafka_2.13-2.4.1

KAFKA=$KAFKA_HOME/bin
KAFKA_CFG=$KAFKA_HOME/config

CONNECTOR_CLASS="com.sintef.asam.PrometheusSinkConnector"
CONNECTOR_NAME="prometheus-sink-connector"
LOCAL_IP=$( ip -o route get to 8.8.8.8 | sed -n 's/.*src \([0-9.]\+\).*/\1/p' )
ENDPOINT_PORT=8085

##BUILDING CONNECTOR
cd $ROOT_DIR
echo "Building connector from sources..."
mvn clean install > /dev/null 2>&1
echo "Copying JAR with deps to /usr/share/java/kafka (will as for root password)"
sudo mkdir /usr/share/java/kafka
sudo cp ./target/*with-dependencies.jar /usr/share/java/kafka/.

mkdir -p $LOG_DIR

##DEPLOYING PROMETHEUS
cd $ROOT_DIR/src/main/docker
echo "Building custom Docker image for Prometheus"
docker build -t kafkaprom/prometheus . > /dev/null 2>&1
echo "Running custom Prometheus scrapping ${LOCAL_IP}:${ENDPOINT_PORT}"
docker run -p 9090:9090 kafkaprom/prometheus 1s ${LOCAL_IP}:${ENDPOINT_PORT} > /dev/null 2>&1 &

##STARTING KAFKA
echo "Starting Zookeeper..."
$KAFKA/zookeeper-server-start.sh $KAFKA_CFG/zookeeper.properties > $LOG_DIR/zookeeper.log 2>&1 &
sleep 10

echo "Starting Kafka broker..."
$KAFKA/kafka-server-start.sh $KAFKA_CFG/server.properties  > $LOG_DIR/kafkabroker.log 2>&1 &
sleep 10

echo "Creating Kafka topics..."
if [[ $# -gt 0 ]] ; then
  echo "${#} topic(s) was/were passed as argument: ${@}"
  TOPICS=${@}
  while [ "$1" != "" ]; do
    echo "  Creating topic ${1}"
    $KAFKA/kafka-topics.sh --create --bootstrap-server 127.0.0.1:9092 --replication-factor 1 --partitions 1 --topic $1 >> $LOG_DIR/kafkatopics.log 2>&1
    shift
  done
else
  echo "No topic passed as argument to this script. Creating a 'test' topic"
  TOPICS="test"
  $KAFKA/kafka-topics.sh --create --bootstrap-server 127.0.0.1:9092 --replication-factor 1 --partitions 1 --topic test >> $LOG_DIR/kafkatopics.log 2>&1
fi

##DEPLOYING CONNECTOR TO KAFKA CONNECT
echo "Starting Kafka Connect Distributed"
$KAFKA/connect-distributed.sh $KAFKA_CFG/connect-distributed.properties > $LOG_DIR/kafkaconnect.log 2>&1 &
sleep 15
echo "Deploying connector for topic(s): ${TOPICS}"
echo '{"name":"'${CONNECTOR_NAME}'","config":{"connector.class":"'${CONNECTOR_CLASS}'","tasks.max":"1","topics":"'${TOPICS}'","value.converter":"org.apache.kafka.connect.converters.ByteArrayConverter"}}'
curl -s -X POST -H 'Content-Type: application/json' http://127.0.0.1:19005/connectors -d '{"name":"'${CONNECTOR_NAME}'","config":{"connector.class":"'${CONNECTOR_CLASS}'","tasks.max":"1","topics":"'${TOPICS}'","value.converter":"org.apache.kafka.connect.converters.ByteArrayConverter"}}' > $LOG_DIR/$CONNECTOR_NAME.log 2>&1
sleep 5
echo "Check that connector is deployed"
curl -s -X GET 'Content-Type: application/json' http://127.0.0.1:19005/connectors/${CONNECTOR_NAME}
