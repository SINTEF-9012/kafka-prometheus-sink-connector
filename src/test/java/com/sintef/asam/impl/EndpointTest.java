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

	final int timeout = 2;
	final int speedValue = 100;
	final int headingValue = 50;
	final String json = "{\"header\":{" + "\"protocolVersion\":1," + "\"messageID\":2," + "\"stationID\":0}," + "\"cam\":{" + "\"speedValue\":%s,"
			+ "\"headingValue\":%s}" + "}";


	private String GET(int port) {
		StringBuilder result = new StringBuilder();
		try {
			URL url = new URL("http://localhost:" + port);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;
			while ((line = rd.readLine()) != null) {
				result.append(line+"\n");
			}
			rd.close();
		} catch (IOException e) {
			e.printStackTrace();			
		}
		//System.out.println(result.toString());
		return result.toString();
	}

	@Test
	public void processOne() {	
		PrometheusService service = null;
		try {
			service = new PrometheusService(8089, timeout, 1, null);
			service.process("ns", String.format(json, speedValue, headingValue), CAM.class);

			final String result = GET(service.port);

			assertTrue(result.contains(String.format("ns_0_speedValue %s", speedValue)));
			assertTrue(result.contains(String.format("ns_0_headingValue %s", headingValue)));
		} catch (IOException e) {
			fail("Service should find an alternative port if prefered specified port is busy.");			
		} finally {
			if(service != null) service.stop();
		}
	}
	
	@Test
	public void processThree() {	
		PrometheusService service = null;
		try {
			service = new PrometheusService(8089, timeout, 3, null);
			service.process("ns", String.format(json, speedValue, headingValue), CAM.class);
			service.process("ns", String.format(json, speedValue+1, headingValue+1), CAM.class);
			service.process("ns", String.format(json, speedValue+2, headingValue+2), CAM.class);

			final String result = GET(service.port);

			assertTrue(result.contains(String.format("ns_0_speedValue %s", speedValue+1)));
			assertTrue(result.contains(String.format("ns_0_headingValue %s", headingValue+1)));
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
			service = new PrometheusService(8089, timeout, 1, null);
			service.process("ns", String.format(json, speedValue, headingValue), CAM.class);

			Thread.sleep((timeout+1)*1000);

			final String result = GET(service.port);

			assertTrue(result.isEmpty());
		} catch (IOException e) {
			fail("Service should find an alternative port if prefered specified port is busy.");			
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			if(service != null) service.stop();
		}
	}

}
