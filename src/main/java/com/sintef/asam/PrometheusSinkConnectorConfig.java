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

	public static final String PROMETHEUS = "prometheus";
	private static final String PROMETHEUS_DOC = "Address of the Prometheus servser";
	private static final String PROMETHEUS_DEFAULT = "localhost:8080";
	
	public static final String PORT = "port";
	private static final String PORT_DOC = "Port for the local Prometheus HTTP endpoint";
	private static final int PORT_DEFAULT = 8085;
	
	public static final String TIMEOUT = "timeout";
	private static final String TIMEOUT_DOC = "Time (in seconds) after which a non-updated timeseries can be considered (temporarily) inactive";
	private static final int TIMEOUT_DEFAULT = 10;
	
	public static final String DESERIALIZER = "deserializer";
	private static final String DESERIALIZER_DOC = "Class used by JSON.parseObject to produce Java objects from JSON payloads";
	private static final String DESERIALIZER_DEFAULT = "com.sintef.asam.impl.cam.CAM";
	
	public static final String BUFFER = "buffer";
	private static final String BUFFER_DOC = "Size of the buffer on which values are averaged";
	private static final int BUFFER_DEFAULT = 5;

	public PrometheusSinkConnectorConfig(ConfigDef config, Map<String, String> parsedConfig) {
		super(config, parsedConfig);
	}

	public PrometheusSinkConnectorConfig(Map<String, String> parsedConfig) {
		this(conf(), parsedConfig);
	}

	public static ConfigDef conf() {
		return new ConfigDef()
				.define(PORT, Type.INT, PORT_DEFAULT, Importance.HIGH, PORT_DOC)
				.define(TIMEOUT, Type.INT, TIMEOUT_DEFAULT, Importance.HIGH, TIMEOUT_DOC)
				.define(BUFFER, Type.INT, BUFFER_DEFAULT, Importance.HIGH, BUFFER_DOC)
				.define(DESERIALIZER, Type.STRING, DESERIALIZER_DEFAULT, Importance.HIGH, DESERIALIZER_DOC);
	}

	public int getPort() {
		return this.getInt(PORT);
	}
	
	public int getTimeout() {
		return this.getInt(TIMEOUT);
	}
	
	public int getBuffer() {
		return this.getInt(BUFFER);
	}
	
	public String getDeserializer() {
		return this.getString(DESERIALIZER);
	}
	
	public String getPrometheus() {
		return this.getString(PROMETHEUS);
	}

}
