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
		final int buffer = 3;
		
		final CollectorRegistry registry = new CollectorRegistry();
		final PrometheusFactory factory = new PrometheusFactory(registry,2,buffer);
		final TimeoutGauge gauge = new TimeoutGauge(factory, "ns", "ss", "n",2,buffer);
		
		final Counter count = new Counter();
		Disposable d = gauge.obs.subscribe(
				e -> {
					assertEquals(buffer, e.size());
					if(count.i == 0) assertEquals((double)e.get(0), (double)value, 0.001);
					else assertEquals((double)e.get(0), (double)value+1, 0.001);
					count.inc();
	    		},
	    		err -> {
	    			fail();
	    		});
		for(int i=0; i<buffer; i++)	gauge.update(value);
		assertEquals(1, count.i);
		
		for(int i=0; i<buffer; i++)	gauge.update(value+1);
		assertEquals(2, count.i);
		
		d.dispose();
		
		gauge.obs.subscribe(
			e -> {
				fail();
    		},
    		err -> {}
    	);
		
		try {
			Thread.sleep((gauge.timeout+1)*1000);
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

