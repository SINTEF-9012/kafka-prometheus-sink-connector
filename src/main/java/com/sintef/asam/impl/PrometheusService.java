package com.sintef.asam.impl;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sintef.asam.PrometheusSinkConnectorConfig;

import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;

public class PrometheusService {
	private static final Logger logger = LogManager.getLogger(PrometheusService.class);

	private HTTPServer server;
	private PrometheusFactory factory;
	
	public PrometheusService(PrometheusSinkConnectorConfig props) throws IOException {	
		this(props.getPrometheusPort());
	}
	
	private PrometheusService(int port) throws IOException {
		factory = new PrometheusFactory();
		server = new HTTPServer(port);
	}
	
	public void process(String namespace, String subsystem, String name, long value) {
		if (server == null || factory == null) return;
		Gauge gauge = factory.createOrGetGauge(namespace, subsystem, name);
		gauge.set(value);
	}
	
	public void stop() {
		factory.clean();
		factory = null;
		if (server != null) {
			logger.warn("STOPPING prometheus HTTP endpoint on port '{}'", server.getPort());
			System.err.println("STOPPING prometheus HTTP endpoint on port " + server.getPort());
			server.stop();
			server = null;
		}
	}
	
	//FIXME: write a test
	public static void main(String args[]) {
		PrometheusService s = null;
		try {
			final PrometheusService service = new PrometheusService(8088);
			s = service;
			new Thread(new Runnable() {
				
				int i = 0;
				
				@Override
				public void run() {
					while(!Thread.interrupted()) {
						System.out.println("Exposing data " + i);
						service.process("ns", "ss", "t", 25+i);
						service.process("ns", "ss", "h", 50+i);
						i++;
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					service.stop();
				}
			}).start();
		} catch (IOException e) {
			e.printStackTrace();
			s.stop();
		}
	}
	
}
