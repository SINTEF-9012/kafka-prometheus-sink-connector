package com.sintef.asam.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.sintef.asam.PrometheusSinkConnectorConfig;
import com.sintef.asam.impl.cam.CAM;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;

public class PrometheusService {
	private static final Logger logger = LogManager.getLogger(PrometheusService.class);

	private HTTPServer server;
	private CollectorRegistry registry;
	private PrometheusFactory factory;
	
	private static int portOffset = 0;
	int port;

	public PrometheusFactory getFactory() {
		return factory;
	}

	public PrometheusService(PrometheusSinkConnectorConfig props) throws IOException {
		this(props.getPrometheusPort()+portOffset, props.getPrometheusTimeout());
		portOffset++;		
	}

	public PrometheusService(int port, int timeout) throws IOException {
		TimeoutGauge.TIMEOUT = timeout;
		registry = new CollectorRegistry();
		factory = new PrometheusFactory(registry);		
		try {
			server = new HTTPServer(new InetSocketAddress(port), registry, false);
			logger.info("Starting Prometheus service with HTTP endpoint available on port '{}'", port);
		} catch (IOException e) {//we try on a port chosen by Java
			server = new HTTPServer(new InetSocketAddress(0), registry, false);
			logger.info("Starting Prometheus service with HTTP endpoint available on port '{}'", server.getPort());
			System.out.println("Starting Prometheus service with HTTP endpoint available on port " + server.getPort());
		}
		this.port = server.getPort();
	}

	public void process(String namespace, String json, Class<?> messageType) {
		if (server == null || factory == null)
			return;
		final Message m = (Message) JSON.parseObject(json, messageType);
		m.process(namespace, this);
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

	private class Producer implements Runnable {

		boolean stopRequested = false;
		long baseID = 0;
		int count = 0;
		long range;
		PrometheusService service;

		public Producer(long baseID, long range, PrometheusService service) {
			this.baseID = baseID;
			this.service = service;
			this.range = range;
		}

		@Override
		public void run() {
			
			while (!stopRequested) {
				final String json = "{\"header\":{" + "\"protocolVersion\":1," + "\"messageID\":2," + "\"stationID\":"
						+ (baseID + (count % range)) + "}," + "\"cam\":{" + "\"speedValue\":" + (count % 80) + ","
						+ "\"headingValue\":" + (count % 50) + "}" + "}";
				service.process("cam", json, CAM.class);
				count++;				 
			}

		}

	}

	// FIXME: write a test
	public static void main(String args[]) {
		final int MAX_SERVICES = 5;		
		final int MAX_PRODUCERS = 20;
		final long MAX_STATION_ID_PER_PRODUCER = 10000;
		final int DURATION = 60; //s
		
		List<PrometheusService> services = new ArrayList<>();
		try {				
			for (int i = 0; i < MAX_SERVICES; i++) {
				final PrometheusService service = new PrometheusService(8089+i, 10);
				services.add(service);
			}
									
			final List<Producer> producers = new ArrayList<>();
			for (int i = 0; i < MAX_PRODUCERS; i++) {
				final PrometheusService service = services.get(i%MAX_SERVICES);
				final Producer p = service.new Producer(MAX_STATION_ID_PER_PRODUCER * i, MAX_STATION_ID_PER_PRODUCER, service);
				producers.add(p);
				new Thread(p).start();
			}
			
			Thread.sleep(DURATION * 1000);
			
			for(Producer p : producers) {
				p.stopRequested = true;
			}
			
			Thread.sleep(5000);
			
			long sum = 0;
			for(Producer p : producers) {
				sum += p.count;					
			}
			System.out.println("Throuput: " + (sum/DURATION) + " msg/s");									
			
			throw new Exception("Terminating!");
		} catch (Exception e) {
			//e.printStackTrace();
			for(PrometheusService service : services) {
				service.stop();
			}
		}
	}

}
