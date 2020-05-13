#!/usr/bin/env bash

WIN=1

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd)
ROOT_DIR=$( cd "$DIR/../../.." && pwd)

((WIN)) && LOCAL_IP=$( ipconfig | perl -nle'/(\d+\.\d+\.\d+\.\d+)/ && print $1' | sed '2 d' | head -n1 )
((!WIN)) && LOCAL_IP=$( ip -o route get to 8.8.8.8 | sed -n 's/.*src \([0-9.]\+\).*/\1/p' )

echo "Local IP: ${LOCAL_IP}"

ENDPOINT_PORT=8089
NUMBER_OF_ENDPOINT=10
DURATION=60
SCRAP_PERIOD=5

##DEPLOYING PROMETHEUS
cd $ROOT_DIR/src/main/docker
echo "Building custom Docker image for Prometheus"
((!WIN)) && sudo docker build -t kafkaprom/prometheus . > /dev/null 2>&1
((WIN)) && docker build -t kafkaprom/prometheus .

for e in `seq 0 $((NUMBER_OF_ENDPOINT-1))`; do
 endpoints="$endpoints ${LOCAL_IP}:$((ENDPOINT_PORT + e))"   
done

echo "Running custom Prometheus scrapping ${LOCAL_IP}:${ENDPOINT_PORT}"
((!WIN)) && sudo docker run --name prom -p 9090:9090 kafkaprom/prometheus ${SCRAP_PERIOD}s $endpoints > /dev/null 2>&1 &
((WIN)) && docker run --name prom -p 9090:9090 kafkaprom/prometheus ${SCRAP_PERIOD}s $endpoints &

sleep 5

##BUILDING CONNECTOR
cd $ROOT_DIR
echo "Building connector from sources..."
mvn clean install -DskipTests > /dev/null 2>&1
echo "Running simulation..."
mvn exec:java -Dexec.mainClass=com.sintef.asam.impl.Util -Dexec.args="--buffer ${SCRAP_PERIOD} --timeout 10 --consumer ${NUMBER_OF_ENDPOINT} --station-id 5000 --duration $DURATION --port ${ENDPOINT_PORT}" > /dev/null 2>&1 &

sleep 5

echo "Opening the Prometheus GUI (http://${LOCAL_IP}:9090/targets) in your web browser..."
((!WIN)) && xdg-open http://${LOCAL_IP}:9090/targets
((WIN)) && start http://localhost:9090/targets
((WIN)) && start "http://localhost:9090/graph?g0.range_input=5m&g0.step_input=5&g0.expr=cam_0_speedValue&g0.tab=0"

sleep $DURATION

#echo "Killing Prometheus"
#((!WIN)) && sudo docker rm -f prom
#((WIN)) && docker rm -f prom

echo "The end".