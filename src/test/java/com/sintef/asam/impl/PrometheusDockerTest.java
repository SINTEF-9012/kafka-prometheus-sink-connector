package com.sintef.asam.impl;

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
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
				runCommand("cmd.exe", "/c", "docker build -t kafkaprom/prometheus . && docker run --name kafkapromtest -d -p 9090:9090 kafkaprom/prometheus 1s " + localIP + ":8085 && docker ps");
			} else {
				runCommand("sh", "-c", "docker build -t kafkaprom/prometheus . && docker run --name kafkapromtest -d -p 9090:9090 kafkaprom/prometheus 1s " + localIP + ":8085");//Maybe?
			}

		} catch (URISyntaxException | IOException e) {
			e.printStackTrace();
		}		
	}

	private String GET() {
		StringBuilder result = new StringBuilder();
		try {
			String address = localIP;
			if (isWindows) {
				address = runCommand("cmd.exe", "/c", "docker-machine ip default");
			} /*else {
				runCommand("sh", "-c", "docker build -t kafkaprom/prometheus . && docker run --name kafkapromtest -d -p 9090:9090 kafkaprom/prometheus 1s " + localIP + ":8085");//Maybe?
			}*/
			System.out.println("Connecting to http://" + address + ":9090");
			URL url = new URL("http://" + address + ":9090");
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
		GET();
	}

}
