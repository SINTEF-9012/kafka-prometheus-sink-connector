package com.sintef.asam.impl;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.prometheus.client.Gauge;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class TimeoutGauge {
	
	private static final Logger logger = LogManager.getLogger(TimeoutGauge.class);
	
	private static final long TIMEOUT = 10; //timeout in seconds
	
	private final PrometheusFactory factory;
	final Gauge gauge;
	
    private final PublishSubject<Float> publishSubject;
    private final Observable<Float> timeout;
    
	public TimeoutGauge(PrometheusFactory factory, String namespace, String subsystem, String name) {
		this.factory = factory;
		this.gauge = Gauge.build().namespace(namespace).subsystem(subsystem).name(name)
				.help("Gauge " + namespace + "_" + subsystem + "_" + name).register(factory.registry);
		this.publishSubject = PublishSubject.create();
	    this.timeout = publishSubject.timeout(TIMEOUT, TimeUnit.SECONDS);
	    
	    timeout.subscribe(
	    		e -> {
	    			this.gauge.set(e);
	    			logger.debug(e);
	    		},
	    		err -> {
	    			this.factory.removeGauge(namespace, subsystem, name); 
	    			logger.info("Timeout: Cleaning gauge " + namespace + "_" + subsystem + "_" + name);
	    		},
	    		() -> logger.debug("Complete!")
	    );
	    
	}

	public void update(float v) {		
		publishSubject.onNext(v);
	}
	
	/*public static void main(String args[]) {
		try {
		final TimeoutGauge tog = new TimeoutGauge(null, "ns", "ss", "n");
		tog.update(10);
		Thread.sleep(500);
		tog.update(20);
		Thread.sleep(1500);
		tog.update(30);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}*/
		
}
