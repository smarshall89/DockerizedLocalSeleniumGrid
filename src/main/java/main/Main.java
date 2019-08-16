package main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.remote.BrowserType;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerClient.ListContainersParam;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.PortBinding;

public class Main {
	
	static int MaxSecondsToWaitBeforeKill = 1;
	static DockerClient docker;
	static String hubName = "selenium-hub";
	static String hubImage = "selenium/hub";
	static String nodeImagePrefix = "selenium/node-";
	
	public static void main(String[] args) throws IOException, DockerCertificateException, DockerException, InterruptedException {
		createDockerObject();
		String hub = getSeleniumHubId();
		String chrome = getBrowserContainerId(BrowserType.CHROME);
		
		startContainer(hub);
		startContainer(chrome);

		stopContainer(chrome);
		stopContainer(hub);

		removeContainer(chrome);
		removeContainer(hub);

	}
	
	private static void startContainer(String containerId) throws DockerException, InterruptedException {
		try {
			docker.startContainer(containerId);	
		}catch(DockerException e) {
			if(e.getMessage().contains("304")) {
				System.out.println("ContainerId '" + containerId + "' already started");
			}
		}
	}
	
	private static void stopContainer(String containerId) throws DockerException, InterruptedException {
		try {
			docker.stopContainer(containerId,MaxSecondsToWaitBeforeKill);	
		}catch(DockerException e) {
			
		}
	}
	
	private static void removeContainer(String containerId) throws DockerException, InterruptedException {
		try {
			docker.removeContainer(containerId);	
		}catch(DockerException e) {
			
		}
	}

	private static String getBrowserContainerId(String browser) throws DockerException, InterruptedException {
		String containerId = getContainerId(nodeImagePrefix+ browser);
		if(containerId == null) {
			HostConfig hostConfig = HostConfig.builder()
					.links("selenium-hub:hub")
					.build();
			
			ContainerConfig containerConfig = ContainerConfig.builder()
					.hostConfig(hostConfig)
				    .image(nodeImagePrefix+ browser)
				    .build();
			
			return docker.createContainer(containerConfig).id();	
		}else {
			return containerId;
		}
	}

	private static void createDockerObject() throws DockerCertificateException, DockerException, InterruptedException {
		docker = DefaultDockerClient.fromEnv().build();
	}
	
	private static String getSeleniumHubId() throws DockerException, InterruptedException {
		String containerId = getContainerId(hubImage);
		
		if(containerId == null) {
			final String[] ports = {"4444"};
			final Map<String, List<PortBinding>> portBindings = new HashMap<>();
			for (String port : ports) {
			    List<PortBinding> hostPorts = new ArrayList<>();
			    hostPorts.add(PortBinding.of("0.0.0.0", port));
			    portBindings.put(port, hostPorts);
			}
			
			List<PortBinding> randomPort = new ArrayList<>();
			randomPort.add(PortBinding.randomPort
					("0.0.0.0"));
			
			final HostConfig hostConfig = HostConfig.builder().portBindings(portBindings).build();
			portBindings.put("443", randomPort);
			final ContainerConfig containerConfig = ContainerConfig.builder()
				    .hostConfig(hostConfig)
				    .image(hubImage)
				    .exposedPorts(ports)
				    .build();
			
			return docker.createContainer(containerConfig,hubName).id();	
		}else {
			return containerId;
		}
	}
	
	private static String getContainerId(String containerName) throws DockerException, InterruptedException {
		final List<Container> containers = docker.listContainers(ListContainersParam.allContainers());
		for(Container c : containers) {
			if(c.image().equalsIgnoreCase(containerName)) {
				return c.id();
			}
		}
		return null;
	}
}
