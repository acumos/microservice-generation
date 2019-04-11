package org.acumos.microservice.component.docker.cmd;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.core.command.PullImageResultCallback;

public class PullImageCommand extends DockerCommand{
	private static final Logger logger = LoggerFactory.getLogger(PullImageCommand.class);
	
	String repository;

	public PullImageCommand(String repository) {
		this.repository = repository;
	} 
	
	public String getRepository() {
		return repository;
	}

	@Override
	public void execute() throws DockerException {
		AuthConfig authConfig = new AuthConfig()
			       .withUsername("docker")
			       .withPassword("docker")
			       .withEmail("ben@me.com")
			       .withRegistryAddress("nexus3.acumos.org");
		String imageFullName = "nexus3.acumos.org:10004/onboarding-base-r";
		logger.debug("Full Image Name: " + imageFullName);
		final DockerClient client = getClient();
		//PullImageCmd pullImageCmd = client.pullImageCmd(imageFullName).withTag("1.0").withAuthConfig(authConfig);
		/*PullImageResultCallback callback = new PullImageResultCallback() {
			@Override
			public void onNext(PullResponseItem item) {
				super.onNext(item);
			}

			@Override
			public void onError(Throwable throwable) {
				logger.error(EELFLoggerDelegate.errorLogger,"Failed to pull image:" + throwable.getMessage());
				super.onError(throwable);
			}
		};
		pullImageCmd.exec(callback).awaitSuccess();*/
		logger.debug("Auth Config started: " + authConfig.toString());
		client.authCmd().withAuthConfig(authConfig).exec(); // WORKS

		logger.debug("Pull Command started");
		client.pullImageCmd(imageFullName) // FAILS
		        .withTag("1.0")
		        .withAuthConfig(authConfig)
		        .exec(new PullImageResultCallback()).awaitSuccess();
		logger.debug("Pull Command end");
		
	}

	@Override
	public String getDisplayName() {
		// TODO Auto-generated method stub
		return "Pull image";
	}
	

}
