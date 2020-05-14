package com.sintef.asam.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.Charset;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.sintef.asam.PrometheusSinkConnectorConfig;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;

public class PrometheusService {
	private static final Logger logger = LogManager.getLogger(PrometheusService.class);

	private HTTPServer server;
	private CollectorRegistry registry;
	private PrometheusFactory factory;

	private static int portOffset = 0;
	int port;
	String prometheus;

	public PrometheusFactory getFactory() {
		return factory;
	}

	public PrometheusService(PrometheusSinkConnectorConfig props) throws IOException {
		this(props.getPort()+portOffset, props.getTimeout(), props.getBuffer(), props.getPrometheus());
		portOffset++;		
	}

	public PrometheusService(int port, int timeout, int buffer, String prometheus) throws IOException {
		registry = new CollectorRegistry();
		factory = new PrometheusFactory(registry, timeout, buffer);
		this.prometheus = prometheus;
		try {
			server = new HTTPServer(new InetSocketAddress(port), registry, false);
			logger.info("Starting Prometheus service with HTTP endpoint available on port '{}'", port);
		} catch (IOException e) {//we try on a port chosen by Java
			server = new HTTPServer(new InetSocketAddress(0), registry, false);
			logger.info("Starting Prometheus service with HTTP endpoint available on port '{}'", server.getPort());
			System.out.println("Starting Prometheus service with HTTP endpoint available on port " + server.getPort());
		}
		this.port = server.getPort();
		register(false);
	}

	public void process(String namespace, String json, Class<?> messageType) {
		if (server == null || factory == null)
			return;
		final Message m = (Message) JSON.parseObject(json, messageType);
		m.process(namespace, this);
	}

	public void stop() {
		//factory.clean();
		//Schedulers.shutdown();
		//factory = null;
		register(true);
		if (server != null) {
			logger.warn("STOPPING prometheus HTTP endpoint on port '{}'", server.getPort());
			System.err.println("STOPPING prometheus HTTP endpoint on port " + server.getPort());
			server.stop();
			//server = null;
		}
	}
	
	public void register(boolean delete) {
		if (this.prometheus != null) {
			try(final DatagramSocket socket = new DatagramSocket()){
				socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
				final String localIP = socket.getLocalAddress().getHostAddress();
				//System.out.println("localIP: " + localIP);
				URL url = new URL("http://" + prometheus + "/targets");
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				if (delete)
					conn.setRequestMethod("DELETE");
				else
					conn.setRequestMethod("POST");
				//conn.setConnectTimeout(5000);
				conn.setDoOutput( true );
				final String data = localIP + ":" + server.getPort() + " custom";
				conn.getOutputStream().write(data.getBytes(Charset.forName("UTF-8")));					
				BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				String line;
				StringBuilder result = new StringBuilder();
				while ((line = rd.readLine()) != null) {
					result.append(line+"\n");
				}
				rd.close();
				//System.out.println(result.toString());
			} catch (Exception e) {				
				//e.printStackTrace();
			}
		}
	}
}
