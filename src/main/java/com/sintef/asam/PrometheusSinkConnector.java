package com.sintef.asam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.sink.SinkConnector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PrometheusSinkConnector extends SinkConnector {

	private static final Logger logger = LogManager.getLogger(PrometheusSinkConnector.class);

	private Map<String, String> configProperties;

	@Override
	public String version() {
		return VersionUtil.getVersion();
	}

	@Override
	public void start(Map<String, String> props) {
		try {
			configProperties = props;
			new PrometheusSinkConnectorConfig(props);
		} catch (ConfigException e) {
			final String err = "Couldn't start PrometheusSinkConnector due to configuration error: " + e.getMessage();
			logger.error(err);
			System.err.println(err);
			e.printStackTrace();
		}
	}

	@Override
	public Class<? extends Task> taskClass() {
		return PrometheusSinkConnectorTask.class;
	}

	@Override
	public List<Map<String, String>> taskConfigs(int maxTasks) {
		List<Map<String, String>> taskConfigs = new ArrayList<>();
		Map<String, String> taskProps = new HashMap<>();
		taskProps.putAll(configProperties);
		for (int i = 0; i < maxTasks; i++) {
			taskConfigs.add(taskProps);
		}
		return taskConfigs;
	}

	@Override
	public void stop() {
	}

	@Override
	public ConfigDef config() {
		return PrometheusSinkConnectorConfig.conf();
	}

}
