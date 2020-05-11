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
		this(props.getPrometheusPort()+portOffset, props.getPrometheusTimeout(), props.getPrometheusBuffer());
		portOffset++;		
	}

	public PrometheusService(int port, int timeout, int buffer) throws IOException {
		registry = new CollectorRegistry();
		factory = new PrometheusFactory(registry, timeout, buffer);		
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
}
