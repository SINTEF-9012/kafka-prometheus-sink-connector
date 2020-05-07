package com.sintef.asam.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.prometheus.client.CollectorRegistry;

public class PrometheusFactory {
	
	private static final Logger logger = LogManager.getLogger(PrometheusFactory.class);
	
	CollectorRegistry registry;
	private Map<String, Map<String, Map<String, TimeoutGauge>>> gauges = Collections.synchronizedMap(new HashMap<>());
	
	public PrometheusFactory(CollectorRegistry registry) {
		this.registry = registry;
	}

	public void clean() {
		gauges = new HashMap<>();
	}

	public void removeGauge(String namespace, String subsystem, String name) {
		final Map<String, Map<String, TimeoutGauge>> namespaceGauges = gauges.get(namespace);
		if (namespaceGauges == null) return;
		final Map<String, TimeoutGauge> subsystemGauges = namespaceGauges.get(subsystem);
		if (subsystemGauges == null) return;
		final TimeoutGauge gauge = subsystemGauges.get(name);
		if (gauge == null) return;
		gauge.gauge.clear();
		registry.unregister(gauge.gauge);
		subsystemGauges.remove(name);		
	}
	
	public TimeoutGauge createOrGetGauge(String namespace, String subsystem, String name) {		
		Map<String, Map<String, TimeoutGauge>> namespaceGauges = gauges.get(namespace);
		if (namespaceGauges == null) {
			logger.info("Namespace '{}' does not exist. Creating it.", namespace);
			namespaceGauges = Collections.synchronizedMap(new HashMap<>());
			gauges.put(namespace, namespaceGauges);
		}

		Map<String, TimeoutGauge> subsystemGauges = namespaceGauges.get(subsystem);
		if (subsystemGauges == null) {
			logger.info("Subsystem '{}' does not exist. Creating it.", subsystem);
			subsystemGauges = Collections.synchronizedMap(new HashMap<>());
			namespaceGauges.put(subsystem, subsystemGauges);
		}

		TimeoutGauge gauge = subsystemGauges.get(name);
		if (gauge == null) {
			logger.info("Gauge '{}' does not exist. Creating it.", (subsystem + "_" + namespace + "_" + name));
			gauge = new TimeoutGauge(this, namespace, subsystem, name);
			subsystemGauges.put(name, gauge);
		}

		return gauge;
	}

}
