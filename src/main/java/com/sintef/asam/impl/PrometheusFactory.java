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
	Map<String, TimeoutGauge> gauges = Collections.synchronizedMap(new HashMap<>());
	
	public PrometheusFactory(CollectorRegistry registry) {
		this.registry = registry;
	}

	public void clean() {
		synchronized (gauges) {
			for(TimeoutGauge gauge : gauges.values()) {
				gauge.gauge.clear();
				registry.unregister(gauge.gauge);
			}
		}				
		gauges.clear();
	}

	public void removeGauge(String namespace, String subsystem, String name) {
		final String qname = namespace + "_" + subsystem + "_" + name;
		final TimeoutGauge gauge = gauges.get(qname);
		if (gauge == null) return;		
		registry.unregister(gauge.gauge);
		//gauge.gauge.clear();
		gauges.remove(qname);		
	}
	
	public TimeoutGauge createOrGetGauge(String namespace, String subsystem, String name) {		
		final String qname = namespace + "_" + subsystem + "_" + name;
		TimeoutGauge gauge = gauges.get(qname);
		if (gauge == null) {
			logger.info("Gauge '{}' does not exist. Creating it.", (subsystem + "_" + namespace + "_" + name));
			gauge = new TimeoutGauge(this, namespace, subsystem, name);
			gauges.put(qname, gauge);
		}

		return gauge;
	}

}
