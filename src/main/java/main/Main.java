package main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.remote.BrowserType;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.PortBinding;

public class Main {
	public static void main(String[] args) throws IOException, DockerCertificateException, DockerException, InterruptedException {
		DockerClient docker = createDockerObject();
		ContainerCreation hub = createSeleniumHub(docker);
		ContainerCreation chrome = createBrowserNode(docker,BrowserType.CHROME);
		
		docker.startContainer(hub.id());
		docker.startContainer(chrome.id());

		
		docker.stopContainer(chrome.id(),1);
		docker.stopContainer(hub.id(),1);

		
		docker.removeContainer(chrome.id());
		docker.removeContainer(hub.id());

	}
	
	private static ContainerCreation createBrowserNode(DockerClient docker , String browser) throws DockerException, InterruptedException {
		HostConfig hostConfig = HostConfig.builder()
				.links("selenium-hub:hub")
				.build();
		
		ContainerConfig containerConfig = ContainerConfig.builder()
				.hostConfig(hostConfig)
			    .image("selenium/node-" + browser)
			    .cmd("-e","NODE_MAX_SESSION=10")
			    .cmd("-e","NODE_MAX_INSTANCES=10")
			    .build();
		
		return docker.createContainer(containerConfig);
	}

	private static DockerClient createDockerObject() throws DockerCertificateException {
		return DefaultDockerClient.fromEnv().build();
	}
	
	private static ContainerCreation createSeleniumHub(DockerClient docker) throws DockerException, InterruptedException {
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
			    .image("selenium/hub")
			    .exposedPorts(ports)
			    .build();
		
		return docker.createContainer(containerConfig,"selenium-hub");
	}
}
