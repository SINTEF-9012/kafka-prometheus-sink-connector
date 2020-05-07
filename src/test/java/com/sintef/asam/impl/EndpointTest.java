package com.sintef.asam.impl;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.Test;

import com.sintef.asam.impl.cam.CAM;

public class EndpointTest {

	final int timeout = 3;
	final int speedValue = 100;
	final int headingValue = 50;
	final String json = "{\"header\":{" + "\"protocolVersion\":1," + "\"messageID\":2," + "\"stationID\":0}," + "\"cam\":{" + "\"speedValue\":%s,"
			+ "\"headingValue\":%s}" + "}";


	@Test
	public void process() {	
		PrometheusService service = null;
		try {
			service = new PrometheusService(8089, timeout);
			service.process("ns", String.format(json, speedValue, headingValue), CAM.class);

			StringBuilder result = new StringBuilder();
			URL url = new URL("http://localhost:" + service.port);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;
			while ((line = rd.readLine()) != null) {
				result.append(line+"\n");
			}
			rd.close();

			System.out.println(result.toString());

			assertTrue(result.toString().contains(String.format("ns_0_speedValue %s", speedValue)));
			assertTrue(result.toString().contains(String.format("ns_0_headingValue %s", headingValue)));
		} catch (IOException e) {
			fail("Service should find an alternative port if prefered specified port is busy.");			
		} finally {
			if(service != null) service.stop();
		}
	}

	@Test
	public void Timeout() {
		PrometheusService service = null;
		try {
			service = new PrometheusService(8089, timeout);
			service.process("ns", String.format(json, speedValue, headingValue), CAM.class);

			Thread.sleep((timeout+1)*1000);
			
			StringBuilder result = new StringBuilder();
			URL url = new URL("http://localhost:" + service.port);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;
			while ((line = rd.readLine()) != null) {
				result.append(line+"\n");
			}
			rd.close();

			System.out.println(result.toString());
			
			assertTrue(result.toString().isEmpty());
		} catch (IOException e) {
			fail("Service should find an alternative port if prefered specified port is busy.");			
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			if(service != null) service.stop();
		}
	}

}
