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
	
	public static final String PROMETHEUS_TIMEOUT = "prometheus.timeout";
	private static final String PROMETHEUS_TIMEOUT_DOC = "Time (in seconds) after which a non-updated timeseries can be considered (temporarily) inactive";
	private static final int PROMETHEUS_TIMEOUT_DEFAULT = 10;
	
	public static final String PROMETHEUS_DESERIALIZER = "prometheus.deserializer";
	private static final String PROMETHEUS_DESERIALIZER_DOC = "Class used by JSON.parseObject to produce Java objects from JSON payloads";
	private static final String PROMETHEUS_DESERIALIZER_DEFAULT = "com.sintef.asam.impl.cam.CAM";
	
	public static final String PROMETHEUS_BUFFER = "prometheus.timeout";
	private static final String PROMETHEUS_BUFFER_DOC = "Size of the buffer on which values are averaged";
	private static final int PROMETHEUS_BUFFER_DEFAULT = 5;

	public PrometheusSinkConnectorConfig(ConfigDef config, Map<String, String> parsedConfig) {
		super(config, parsedConfig);
	}

	public PrometheusSinkConnectorConfig(Map<String, String> parsedConfig) {
		this(conf(), parsedConfig);
	}

	public static ConfigDef conf() {
		return new ConfigDef()
				.define(PROMETHEUS_PORT, Type.INT, PROMETHEUS_PORT_DEFAULT, Importance.HIGH, PROMETHEUS_PORT_DOC)
				.define(PROMETHEUS_TIMEOUT, Type.INT, PROMETHEUS_TIMEOUT_DEFAULT, Importance.HIGH, PROMETHEUS_TIMEOUT_DOC)
				.define(PROMETHEUS_BUFFER, Type.INT, PROMETHEUS_BUFFER_DEFAULT, Importance.HIGH, PROMETHEUS_BUFFER_DOC)
				.define(PROMETHEUS_DESERIALIZER, Type.STRING, PROMETHEUS_DESERIALIZER_DEFAULT, Importance.HIGH, PROMETHEUS_DESERIALIZER_DOC);
	}

	public int getPrometheusPort() {
		return this.getInt(PROMETHEUS_PORT);
	}
	
	public int getPrometheusTimeout() {
		return this.getInt(PROMETHEUS_TIMEOUT);
	}
	
	public int getPrometheusBuffer() {
		return this.getInt(PROMETHEUS_BUFFER);
	}
	
	public String getPrometheusDeserializer() {
		return this.getString(PROMETHEUS_DESERIALIZER);
	}

}
