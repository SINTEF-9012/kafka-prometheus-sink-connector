package com.sintef.asam.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;

public class PrometheusFactory {
	
	private static final Logger logger = LogManager.getLogger(PrometheusFactory.class);
	
	private CollectorRegistry registry;
	private Map<String, Map<String, Map<String, Gauge>>> gauges = new HashMap<>();
	
	public PrometheusFactory(CollectorRegistry registry) {
		this.registry = registry;
	}

	public void clean() {
		gauges = new HashMap<>();
	}

	public Gauge createOrGetGauge(String namespace, String subsystem, String name) {		
		Map<String, Map<String, Gauge>> namespaceGauges = gauges.get(namespace);
		if (namespaceGauges == null) {
			logger.info("Namespace '{}' does not exist. Creating it.", namespace);
			namespaceGauges = new HashMap<String, Map<String, Gauge>>();
			gauges.put(namespace, namespaceGauges);
		}

		Map<String, Gauge> subsystemGauges = namespaceGauges.get(subsystem);
		if (subsystemGauges == null) {
			logger.info("Subsystem '{}' does not exist. Creating it.", subsystem);
			subsystemGauges = new HashMap<String, Gauge>();
			namespaceGauges.put(subsystem, subsystemGauges);
		}

		Gauge gauge = subsystemGauges.get(name);
		if (gauge == null) {
			logger.info("Gauge '{}' does not exist. Creating it.", (subsystem + "_" + namespace + "_" + name));
			gauge = Gauge.build().namespace(namespace).subsystem(subsystem).name(name)
					.help("Gauge " + namespace + "_" + subsystem + "_" + name).register(registry);
			subsystemGauges.put(name, gauge);
		}

		return gauge;
	}

}
