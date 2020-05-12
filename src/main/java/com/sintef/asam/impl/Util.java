package com.sintef.asam.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.sintef.asam.impl.cam.CAM;

public class Util {
	static final Util UTIL = new Util();

	private class PrometheusMockUp implements Runnable {
		volatile boolean stopRequested = false;
		boolean isStopped = true;

		List<String> endpoints = new ArrayList<String>();
		int period = 0;
		long amountData = 0;
		
		List<Integer> sizes = new ArrayList<>();

		public PrometheusMockUp(List<String> endpoints, int period) {
			this.endpoints = endpoints;
			this.period = period;
		}

		@Override
		public void run() {
			while (!stopRequested) {
				for(String endpoint : endpoints) {
					final String result = GET(endpoint);
					if(result.length()>0) sizes.add(result.length());
					amountData += result.length();
				}
				try {
					Thread.sleep(period);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			System.out.println("Stop requested...");
			long amountDataSinceStopRequested = 0;
			do {
				amountDataSinceStopRequested = 0;
				for(String endpoint : endpoints) {
					final String result = GET(endpoint);
					if(result.length()>0) sizes.add(result.length());
					amountData += result.length();
					amountDataSinceStopRequested += result.length();										
				}
				try {
					Thread.sleep(period);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if(amountDataSinceStopRequested != 0) 
					System.out.println("Still scraping data: " + FileUtils.byteCountToDisplaySize(amountDataSinceStopRequested));
			} while (amountDataSinceStopRequested != 0);

			isStopped = true;

			System.out.println("Total amount of data scrapped by Prometheus: " + FileUtils.byteCountToDisplaySize(amountData));
			long sum = 0;
			for(int s : sizes) {
				sum += s;
			}
			System.out.println("#scraps: " + sizes.size() + "Average size of an endpoint per scrap: "  + FileUtils.byteCountToDisplaySize(sum/sizes.size()));
			
		}

		private String GET(String endpoint) {
			StringBuilder result = new StringBuilder();
			try {
				//System.out.println("Connecting to http://" + endpoint);
				URL url = new URL("http://" + endpoint);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("GET");
				BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				String line;
				while ((line = rd.readLine()) != null) {
					result.append(line+"\n");
				}
				rd.close();
			} catch (IOException e) {
				//e.printStackTrace();			
			}
			//System.out.println(result.toString());
			return result.toString();
		}
	}

	private class Producer implements Runnable {

		volatile boolean stopRequested = false;
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
		final int BUFFER = 5;
		final int TIMEOUT = 10;
		final int MAX_SERVICES = 10;		
		final int MAX_PRODUCERS = 30;
		final long MAX_STATION_ID_PER_PRODUCER = 5000;
		final int DURATION = 60; //s

		List<PrometheusService> services = new ArrayList<>();
		try {	
			List<String> endpoints = new ArrayList<String>();
			for (int i = 0; i < MAX_SERVICES; i++) {
				final PrometheusService service = new PrometheusService(8089+i, TIMEOUT, BUFFER);
				services.add(service);
				endpoints.add("localhost:" + service.port);
			}

			final PrometheusMockUp prom = UTIL.new PrometheusMockUp(endpoints, BUFFER);
			final Thread promThread = new Thread(prom);
			promThread.start();

			final List<Producer> producers = new ArrayList<>();
			final List<Thread> producerThreads = new ArrayList<Thread>();
			for (int i = 0; i < MAX_PRODUCERS; i++) {
				final PrometheusService service = services.get(i%MAX_SERVICES);
				final Producer p = UTIL.new Producer(MAX_STATION_ID_PER_PRODUCER * i, MAX_STATION_ID_PER_PRODUCER, service);
				producers.add(p);
				final Thread t = new Thread(p);
				producerThreads.add(t);
				t.start();
			}

			Thread.sleep(DURATION * 1000);

			for(Producer p : producers) {
				p.stopRequested = true;
			}

			Thread.sleep(5000);
			
			for(Thread prod : producerThreads) {
				prod.join(100);
			}

			long sum = 0;
			for(Producer p : producers) {
				sum += p.count;					
			}
			System.out.println("Throuput: " + (sum/DURATION) + " msg/s");	

			prom.stopRequested = true;

			while(!prom.isStopped) {
				System.out.println("Waiting...");
				Thread.sleep(1000);
			}
			
			promThread.join(100);			

			throw new Exception("Terminating!");
		} catch (Exception e) {
			//e.printStackTrace();
			for(PrometheusService service : services) {
				service.stop();
			}
		}
	}

}
