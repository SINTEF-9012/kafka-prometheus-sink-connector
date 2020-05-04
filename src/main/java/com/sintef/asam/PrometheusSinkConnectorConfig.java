package com.sintef.asam;

import java.util.Map;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PrometheusSinkConnectorConfig extends AbstractConfig {

	private static final Logger logger = LogManager.getLogger(PrometheusSinkConnectorConfig.class);

	public static final String PROMETHEUS_PORT = "prometheus.port";
	private static final String PROMETHEUS_PORT_DOC = "Port for the local Prometheus HTTP endpoint";
	private static final int PROMETHEUS_PORT_DEFAULT = 8085;

	/*public static final String TIMESERIES = "timeseries";
	private static final String TIMESERIES_DOC = "Path (in JSON document) to the data to be monitored"*/;

	public PrometheusSinkConnectorConfig(ConfigDef config, Map<String, String> parsedConfig) {
		super(config, parsedConfig);
	}

	public PrometheusSinkConnectorConfig(Map<String, String> parsedConfig) {
		this(conf(), parsedConfig);
	}

	public static ConfigDef conf() {
		return new ConfigDef()
				.define(PROMETHEUS_PORT, Type.INT, PROMETHEUS_PORT_DEFAULT, Importance.HIGH, PROMETHEUS_PORT_DOC);
				//.define(TIMESERIES, Type.LIST, Importance.HIGH, TIMESERIES_DOC);
	}

	public int getPrometheusPort() {
		return this.getInt(PROMETHEUS_PORT);
	}

}
