package com.sintef.asam.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import io.prometheus.client.CollectorRegistry;
import io.reactivex.rxjava3.disposables.Disposable;

public class TimeoutGaugeTest {

	@Test
	public void update() {
		final float value = 150f;
		
		final CollectorRegistry registry = new CollectorRegistry();
		final PrometheusFactory factory = new PrometheusFactory(registry);
		final TimeoutGauge gauge = new TimeoutGauge(factory, "ns", "ss", "n");
		TimeoutGauge.TIMEOUT = 2;
		
		final Counter count = new Counter();
		Disposable d = gauge.timeout.subscribe(
				e -> {
					if(count.i == 0) assertEquals((double)e, (double)value, 0.001);
					else assertEquals((double)e, (double)value+1, 0.001);
					count.inc();
	    		},
	    		err -> {
	    			fail();
	    		});
		
		gauge.update(value);
		gauge.update(value);
		gauge.update(value);
		
		assertEquals(1, count.i); //the gauge should debounce consecutive similar values
		
		gauge.update(value+1);
		assertEquals(2, count.i);
		
		d.dispose();
		
		gauge.timeout.subscribe(
			e -> {
				fail();
    		},
    		err -> {}
    	);
		
		try {
			Thread.sleep((TimeoutGauge.TIMEOUT+1)*1000);
			gauge.update(value);	
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}
	
	private class Counter {
		int i = 0;
		void inc() {i++;}
	}

}

