package com.sintef.asam.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.prometheus.client.CollectorRegistry;

public class FactoryTest {

	@Test
	public void add() {
		CollectorRegistry registry = new CollectorRegistry();
		PrometheusFactory factory = new PrometheusFactory(registry);
		
		TimeoutGauge gauge = factory.createOrGetGauge("ns", "ss", "n");
		assertNotNull(gauge);
		factory.clean();
	}

	@Test
	public void remove() {
		CollectorRegistry registry = new CollectorRegistry();
		PrometheusFactory factory = new PrometheusFactory(registry);
		
		factory.createOrGetGauge("ns", "ss", "n");
		assertEquals(factory.gauges.size(), 1);
		factory.removeGauge("ns", "ss", "n");
		assertEquals(factory.gauges.size(), 0);
	}
	
	@Test
	public void clean() {
		CollectorRegistry registry = new CollectorRegistry();
		PrometheusFactory factory = new PrometheusFactory(registry);
		
		factory.createOrGetGauge("ns", "ss", "n");
		factory.createOrGetGauge("ns", "ss", "n2");
		assertEquals(factory.gauges.size(), 2);
		factory.clean();
		assertEquals(factory.gauges.size(), 0);
	}

}

