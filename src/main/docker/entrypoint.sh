#!/usr/bin/env bash

echo "adding dynamic discovery to prometheus ${@}"
echo "
    file_sd_configs:
      - files:
        - /etc/prometheus/targets.yml" >> /etc/prometheus/prometheus.yml

cat /etc/prometheus/prometheus.yml

/bin/discovery &
/bin/prometheus --config.file=/etc/prometheus/prometheus.yml --storage.tsdb.path=/prometheus --web.console.libraries=/etc/prometheus/console_libraries --web.console.templates=/etc/prometheus/consoles
