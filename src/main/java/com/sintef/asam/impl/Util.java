package com.sintef.asam.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.sintef.asam.impl.cam.CAM;

public class Util {
	
	@Parameter(names = {"--buffer", "-b"}, description = "Size of the averaging buffer")
	int BUFFER = 5;
	
	@Parameter(names = {"--timeout", "-t"}, description = "Time after which metrics should be (temporarily) discarded")
	int TIMEOUT = 10;
	
	@Parameter(names = {"--consumer", "-c"}, description = "How many consumers to run in the simulation")
	int MAX_SERVICES = 10;
	
	@Parameter(names = {"--producer", "-p"}, description = "How many producers to run in the simulation")
	int MAX_PRODUCERS = 30;
	
	@Parameter(names = {"--station-id", "-sid"}, description = "How many station IDs per producer")
	long MAX_STATION_ID_PER_PRODUCER = 5000;
	
	@Parameter(names = {"--duration", "-d"}, description = "Duration (in s) of the simulation")
	int DURATION = 60; //s
	
	@Parameter(names = {"--port"}, description = "Port of the (first) endpoint")
	int PORT = 8089; //s

	@Parameter(names = {"--help", "-h"}, help = true)
	private boolean help = false;

	
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
			System.out.println("#scraps: " + sizes.size());
			System.out.println("Average size of an endpoint per scrap: "  + FileUtils.byteCountToDisplaySize(sum/sizes.size()));
			
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
						+ (baseID + (count % range)) + "}," + "\"cam\":{" + "\"speedValue\":" + (80 - (count % 10) + Math.random()*10) + ","
						+ "\"headingValue\":" + (50 - (count % 10) + Math.random()*10) + "}" + "}";
				service.process("cam", json, CAM.class);
				count++;
				if (count % range == 0) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

		}

	}

	// FIXME: write a test
	public static void main(String args[]) {
		Util cli = new Util();
		
		final JCommander jcom = JCommander.newBuilder().addObject(cli).build();
		jcom.parse(args);
		
		if (cli.help) {
			//printUsage(jcom);
			System.exit(0);
		}
		
		final int BUFFER = cli.BUFFER;
		final int TIMEOUT = cli.TIMEOUT;
		final int MAX_SERVICES = cli.MAX_SERVICES;		
		final int MAX_PRODUCERS = cli.MAX_PRODUCERS;
		final long MAX_STATION_ID_PER_PRODUCER = cli.MAX_STATION_ID_PER_PRODUCER;
		final int DURATION = cli.DURATION; //s
		final int PORT = cli.PORT;
		
		List<PrometheusService> services = new ArrayList<>();
		try {	
			List<String> endpoints = new ArrayList<String>();
			for (int i = 0; i < MAX_SERVICES; i++) {
				final PrometheusService service = new PrometheusService(PORT+i, TIMEOUT, BUFFER);
				services.add(service);
				endpoints.add("localhost:" + service.port);
			}

			final PrometheusMockUp prom = cli.new PrometheusMockUp(endpoints, BUFFER);
			final Thread promThread = new Thread(prom);
			promThread.start();

			final List<Producer> producers = new ArrayList<>();
			final List<Thread> producerThreads = new ArrayList<Thread>();
			for (int i = 0; i < MAX_PRODUCERS; i++) {
				final PrometheusService service = services.get(i%MAX_SERVICES);
				final Producer p = cli.new Producer(MAX_STATION_ID_PER_PRODUCER * i, MAX_STATION_ID_PER_PRODUCER, service);
				producers.add(p);
				final Thread t = new Thread(p);
				producerThreads.add(t);
				t.start();
				Thread.sleep(25);
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
