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
				runCommand("cmd.exe", "/c", "docker build -t kafkaprom/prometheus . && docker run --name kafkapromtest -d -p 9090:9090 kafkaprom/prometheus 1s " + localIP + ":8089 && docker ps");
			} else {
				runCommand("sh", "-c", "docker build -t kafkaprom/prometheus . && docker run --name kafkapromtest -d -p 9090:9090 kafkaprom/prometheus 1s " + localIP + ":8089 && docker ps");//Maybe?
			}

		} catch (URISyntaxException | IOException e) {
			e.printStackTrace();
		}		
	}

	private String GET(String api) {
		StringBuilder result = new StringBuilder();
		try {
			String address = localIP;
			if (isWindows) {
				address = runCommand("cmd.exe", "/c", "docker-machine ip default");
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
			e.printStackTrace();			
		}
		System.out.println(result.toString());
		return result.toString();
	}

	@Test
	public void test() {
		try {
			final String json = "{\"header\":{" + "\"protocolVersion\":1," + "\"messageID\":2," + "\"stationID\":0}," + "\"cam\":{" + "\"speedValue\":%s,"
					+ "\"headingValue\":%s}" + "}";
			final PrometheusService service = new PrometheusService(8089, 10);
			Thread.sleep(5000);		
			
			final long start = System.currentTimeMillis();
			String startDate = new SimpleDateFormat("yyyy-MM-dd'T'h:m:ss.SSS'Z'").format(new Date());
			
			service.process("ns", String.format(json, 100, 50), CAM.class);
			Thread.sleep(1000);
			final String heading1 = GET("/api/v1/query?query=ns_0_headingValue");
			final String speed1 = GET("/api/v1/query?query=ns_0_speedValue");
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
			
						
			//GET("/api/v1/query_range?query=ns_0_headingValue&start=" + startDate + "&end=" + stopDate + "&step=1s");
			//GET("/api/v1/query_range?query=ns_0_speedValue&start=" + startDate + "&end=" + stopDate + "&step=1s");
			
		} catch (InterruptedException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
