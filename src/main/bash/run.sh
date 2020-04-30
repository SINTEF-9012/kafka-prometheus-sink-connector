#!/usr/bin/env bash

WIN=1	#Set to 1 if running on Windows and having problems with Docker volumes or paths...

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd)


function _docker
{
((WIN)) && export MSYS_NO_PATHCONV=1
docker $@
((WIN)) && export MSYS_NO_PATHCONV=0
}

_docker run -v $DIR/prometheus.yml:/etc/prometheus/prometheus.yml -p 9090:9090 prom/prometheus