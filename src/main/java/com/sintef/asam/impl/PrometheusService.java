package com.sintef.asam.impl;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.sintef.asam.PrometheusSinkConnectorConfig;
import com.sintef.asam.impl.cam.CAM;

import io.prometheus.client.exporter.HTTPServer;

public class PrometheusService {
	private static final Logger logger = LogManager.getLogger(PrometheusService.class);

	private HTTPServer server;
	private PrometheusFactory factory;

	public PrometheusFactory getFactory() {
		return factory;
	}

	public PrometheusService(PrometheusSinkConnectorConfig props) throws IOException {
		this(props.getPrometheusPort());
	}

	private PrometheusService(int port) throws IOException {
		factory = new PrometheusFactory();
		server = new HTTPServer(port);
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

	class Producer implements Runnable {

		int baseID = 0;
		PrometheusService service;

		public Producer(int baseID, PrometheusService service) {
			this.baseID = baseID;
			this.service = service;
		}

		@Override
		public void run() {
			int i = 0;
			while (!Thread.interrupted()) {
				final String json = "{\"header\":{" + "\"protocolVersion\":1," + "\"messageID\":2," + "\"stationID\":"
						+ (baseID + (i % 100)) + "}," + "\"cam\":{" + "\"speedValue\":" + (i % 80) + ","
						+ "\"headingValue\":" + (i % 50) + "}" + "}";
				System.out.println("data " + i + ": " + json);
				service.process("cam", json, CAM.class);
				i++;
				try { Thread.sleep(25); } catch (InterruptedException e) { e.printStackTrace(); }
				 
			}

		}

	}

	// FIXME: write a test
	public static void main(String args[]) {
		PrometheusService service = null;
		try {
			service = new PrometheusService(8088);
			final int MAX_THREAD = 50;
			for (int i = 0; i < MAX_THREAD; i++) {
				final Producer p = service.new Producer(100 * i, service);
				new Thread(p).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
			if (service != null)
				service.stop();
		}
	}

}
