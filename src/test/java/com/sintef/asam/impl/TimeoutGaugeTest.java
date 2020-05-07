package com.sintef.asam.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.prometheus.client.CollectorRegistry;

public class TimeoutGaugeTest {

	@Test
	public void update() {
		final float value = 150f;
		
		final CollectorRegistry registry = new CollectorRegistry();
		final PrometheusFactory factory = new PrometheusFactory(registry);
		final TimeoutGauge gauge = new TimeoutGauge(factory, "ns", "ss", "n");
		
		gauge.publishSubject.subscribe((e)->{
			assertEquals((double)e, (double)value, 0.001);
		});
		
		gauge.update(value);
	}

}

