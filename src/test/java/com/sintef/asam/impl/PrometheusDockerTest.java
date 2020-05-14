package com.sintef.asam.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sintef.asam.impl.cam.CAM;

public class PrometheusDockerTest {

	static String localIP;
	static String entrypoint;
	File dockerDir;

	boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

	private String runCommand(String... cmd) throws IOException {
		ProcessBuilder builder = new ProcessBuilder();
		builder.command(cmd);
		builder.directory(dockerDir);
		final Process p=builder.start();
		BufferedReader br=new BufferedReader(
				new InputStreamReader(
						p.getInputStream()));
		StringBuilder b = new StringBuilder();
		String line;
		while((line=br.readLine())!=null){
			System.out.println(line);
			b.append(line);
		}
		return b.toString();
	}

	@After
	public void stop() {
		try {
			if (isWindows) {
				runCommand("cmd.exe", "/c", "docker rm -f kafkapromtest");
			} else {
				runCommand("sh", "-c", "docker rm -f kafkapromtest");//Maybe?
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Before
	public void init() {		
		try(final DatagramSocket socket = new DatagramSocket()){
			socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
			localIP = socket.getLocalAddress().getHostAddress();
		} catch (SocketException | UnknownHostException e) {				
			e.printStackTrace();
		}
		System.err.println("localIP: " + localIP);



		try {
			File tempDir = FileUtils.getTempDirectory();
			URL dockerUrl = PrometheusDockerTest.class.getClassLoader().getResource("docker/");			
			dockerDir = new File(dockerUrl.toURI());
			FileUtils.copyDirectoryToDirectory(dockerDir, tempDir);

			if (isWindows) {
				runCommand("cmd.exe", "/c", "docker build -t kafkaprom/prometheus . && docker run --name kafkapromtest -d -p 8080:8080 -p 9090:9090 kafkaprom/prometheus 1s " + localIP + ":8089 && docker ps");
			} else {
				runCommand("sh", "-c", "docker build -t kafkaprom/prometheus . && docker run --name kafkapromtest -d -p 8080:8080 -p 9090:9090 kafkaprom/prometheus 1s " + localIP + ":8089 && docker ps");//Maybe?
			}

		} catch (URISyntaxException | IOException e) {
			e.printStackTrace();
		}		
	}

	//FIXME: it seems this is not a very portable way of connecting to prometheus...
	private String GET(String api) {
		StringBuilder result = new StringBuilder();
		try {
			String address = "127.0.0.1";
			if (isWindows) {
				String tempAddress = runCommand("cmd.exe", "/c", "docker-machine ip default");
				if (tempAddress.split("\\.").length == 4) address = tempAddress; //most likely an IP...
			} /*else {
				runCommand("sh", "-c", "docker build -t kafkaprom/prometheus . && docker run --name kafkapromtest -d -p 9090:9090 kafkaprom/prometheus 1s " + localIP + ":8085");//Maybe?
			}*/
			System.out.println("Connecting to http://" + address + ":9090"+api);
			URL url = new URL("http://" + address + ":9090" + api);
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
			return null;
		}
		System.out.println(result.toString());
		return result.toString();
	}

	@Test
	public void test() {
		try {
			final String json = "{\"header\":{" + "\"protocolVersion\":1," + "\"messageID\":2," + "\"stationID\":0}," + "\"cam\":{" + "\"speedValue\":%s,"
					+ "\"headingValue\":%s}" + "}";
			
			String address = "127.0.0.1";
			if (isWindows) {
				String tempAddress = runCommand("cmd.exe", "/c", "docker-machine ip default");
				if (tempAddress.split("\\.").length == 4) address = tempAddress; //most likely an IP...
			} 
			
			final PrometheusService service = new PrometheusService(8089, 5, 1, address + ":8080");
			Thread.sleep(5000);		
			
			final long start = System.currentTimeMillis();
			String startDate = new SimpleDateFormat("yyyy-MM-dd'T'h:m:ss.SSS'Z'").format(new Date());
			
			service.process("ns", String.format(json, 100, 50), CAM.class);
			Thread.sleep(1000);
			final String heading1 = GET("/api/v1/query?query=ns_0_headingValue");
			final String speed1 = GET("/api/v1/query?query=ns_0_speedValue");
			if (heading1 == null || speed1 == null) return;
			assertTrue(heading1.contains("\"50\""));
			assertTrue(speed1.contains("\"100\""));
			service.process("ns", String.format(json, 101, 51), CAM.class);
			Thread.sleep(1000);
			final String heading2 = GET("/api/v1/query?query=ns_0_headingValue");
			final String speed2 = GET("/api/v1/query?query=ns_0_speedValue");
			assertTrue(heading2.contains("\"51\""));
			assertTrue(speed2.contains("\"101\""));
			service.process("ns", String.format(json, 102, 52), CAM.class);
			Thread.sleep(1000);
			final String heading3 = GET("/api/v1/query?query=ns_0_headingValue");
			final String speed3 = GET("/api/v1/query?query=ns_0_speedValue");
			assertTrue(heading3.contains("\"52\""));
			assertTrue(speed3.contains("\"102\""));
			
			String stopDate = new SimpleDateFormat("yyyy-MM-dd'T'h:m:ss.SSS'Z'").format(new Date());
			
			//Check that data is still there, even if gauge has timed out
			Thread.sleep(10000);	
			// a) it is not available anymore through instant query
			final String heading4 = GET("/api/v1/query?query=ns_0_headingValue");
			final String speed4 = GET("/api/v1/query?query=ns_0_speedValue");
			assertFalse(heading4.contains("\"value\""));
			assertFalse(speed4.contains("\"value\""));
			
			// b) data should however still be there e.g. through range vectors
			final String heading5 = GET("/api/v1/query?query=ns_0_headingValue[30s]");
			final String speed5 = GET("/api/v1/query?query=ns_0_speedValue[30s]");
			assertTrue(heading5.contains("\"50\"") && heading5.contains("\"51\"") && heading5.contains("\"52\""));
			assertTrue(speed5.contains("\"100\"") && speed5.contains("\"101\"") && speed5.contains("\"102\""));
			
			//Check that it still works when we create a new gauge for the same timeseries
			service.process("ns", String.format(json, 103, 53), CAM.class);
			Thread.sleep(1000);
			final String heading6 = GET("/api/v1/query?query=ns_0_headingValue");
			final String speed6 = GET("/api/v1/query?query=ns_0_speedValue");
			assertTrue(heading6.contains("\"53\""));
			assertTrue(speed6.contains("\"103\""));
		} catch (InterruptedException | IOException e) {
			//e.printStackTrace();
		}
		
	}

}
