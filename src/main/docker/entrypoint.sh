#!/usr/bin/env bash

if [[ $# -gt 1 ]] ; then
  echo "adding custom targets to prometheus ${@}"
  echo "  - job_name: custom
    scrape_interval: ${1}
    static_configs:
    - targets:" >> /etc/prometheus/prometheus.yml
  shift
  while [ "$1" != "" ]; do
    echo "      - ${1}" >> /etc/prometheus/prometheus.yml
    shift
  done
fi

/bin/prometheus --config.file=/etc/prometheus/prometheus.yml --storage.tsdb.path=/prometheus --web.console.libraries=/etc/prometheus/console_libraries --web.console.templates=/etc/prometheus/consoles