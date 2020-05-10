package com.sintef.asam.impl;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.prometheus.client.Gauge;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class TimeoutGauge {
	
	private static final Logger logger = LogManager.getLogger(TimeoutGauge.class);
	
	long timeout = 10; //timeout in seconds
	int buffer = 1;
	
	
	private final PrometheusFactory factory;
	final Gauge gauge;
	
    final PublishSubject<Float> publish;
    final Observable<List<Float>> obs;
    Disposable disp;
    
    private boolean terminated;
    
	public TimeoutGauge(PrometheusFactory factory, String namespace, String subsystem, String name, long timeout, int buffer) {
		this.buffer = buffer;
		this.timeout = timeout;
		this.factory = factory;
		this.gauge = Gauge.build().namespace(namespace).subsystem(subsystem).name(name)
				.help("Gauge " + namespace + "_" + subsystem + "_" + name).register(factory.registry);
		this.publish = PublishSubject.create();		
	    this.obs = publish.buffer(this.buffer).timeout(this.timeout, TimeUnit.SECONDS, Observable.empty()).onErrorComplete();
	    	    
	    this.disp = obs.subscribe(
	    		e -> {
	    			float sum = 0;
	    			for(float f : e) sum += f;
	    			this.gauge.set(sum/buffer);
	    			//logger.debug(e);
	    		},
	    		err -> {
	    			//System.out.println("err");
	    			terminate(namespace, subsystem, name);	    			
	    		},
	    		() -> {
	    			//System.out.println("complete");	
	    			terminate(namespace, subsystem, name);
	    		} 
	    );	    	    
	}

	private void terminate(String namespace, String subsystem, String name) {
		terminated = true;	    			
		this.factory.removeGauge(namespace, subsystem, name);
		//this.timeoutDisp.dispose();
		logger.info("Timeout: Cleaning gauge " + namespace + "_" + subsystem + "_" + name);
		//System.out.println("Timeout: Cleaning gauge " + namespace + "_" + subsystem + "_" + name);
	}
	
	public void update(float v) {
		if(!terminated) publish.onNext(v);
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
