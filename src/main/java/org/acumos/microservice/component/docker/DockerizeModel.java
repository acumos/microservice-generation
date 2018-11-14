/*-
 * ===============LICENSE_START=======================================================
 * Acumos
 * ===================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property & Tech Mahindra. All rights reserved.
 * ===================================================================================
 * This Acumos software file is distributed by AT&T and Tech Mahindra
 * under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ===============LICENSE_END=========================================================
 */

package org.acumos.microservice.component.docker;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.annotation.PostConstruct;

import org.acumos.cds.client.CommonDataServiceRestClientImpl;
import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.microservice.component.docker.cmd.CreateImageCommand;
import org.acumos.microservice.component.docker.cmd.DeleteImageCommand;
import org.acumos.microservice.component.docker.cmd.PushImageCommand;
import org.acumos.microservice.component.docker.cmd.TagImageCommand;
import org.acumos.microservice.component.docker.preparation.H2ODockerPreparator;
import org.acumos.microservice.component.docker.preparation.JavaGenericDockerPreparator;
import org.acumos.microservice.component.docker.preparation.PythonDockerPreprator;
import org.acumos.microservice.component.docker.preparation.RDockerPreparator;
import org.acumos.nexus.client.NexusArtifactClient;
import org.acumos.nexus.client.RepositoryLocation;
import org.acumos.onboarding.common.exception.AcumosServiceException;
import org.acumos.onboarding.common.utils.EELFLoggerDelegate;
import org.acumos.onboarding.common.utils.LogBean;
import org.acumos.onboarding.common.utils.LogThreadLocal;
import org.acumos.onboarding.common.utils.ResourceUtils;
import org.acumos.onboarding.common.utils.UtilityFunction;
import org.acumos.onboarding.component.docker.preparation.Metadata;
import org.acumos.onboarding.component.docker.preparation.MetadataParser;
import org.acumos.onboarding.services.impl.PortalRestClientImpl;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.github.dockerjava.api.DockerClient;

public class DockerizeModel {
	
	private static final EELFLoggerDelegate logger = EELFLoggerDelegate.getLogger(DockerizeModel.class);
	
	@Value("${nexus.nexusEndPointURL}")
	protected String nexusEndPointURL;

	@Value("${nexus.nexusUserName}")
	protected String nexusUserName;

	@Value("${nexus.nexusPassword}")
	protected String nexusPassword;

	@Value("${nexus.nexusGroupId}")
	protected String nexusGroupId;

	@Value("${cmndatasvc.cmnDataSvcEndPoinURL}")
	protected String cmnDataSvcEndPoinURL;

	@Value("${cmndatasvc.cmnDataSvcUser}")
	protected String cmnDataSvcUser;

	@Value("${cmndatasvc.cmnDataSvcPwd}")
	protected String cmnDataSvcPwd;

	@Value("${http_proxy}")
	protected String http_proxy;

	@Value("${requirements.extraIndexURL}")
	protected String extraIndexURL;

	@Value("${requirements.trustedHost}")
	protected String trustedHost;

	@Value("${mktPlace.mktPlaceEndPointURL}")
	protected String portalURL;
	
	protected String modelOriginalName = null;
	
	

	@Autowired
	ResourceLoader resourceLoader;

	@Autowired
	protected DockerConfiguration dockerConfiguration;
	
	protected MetadataParser metadataParser;
	
	protected CommonDataServiceRestClientImpl cdmsClient;

	protected PortalRestClientImpl portalClient;

	ResourceUtils resourceUtils;
	
	@PostConstruct
	public void init() {
		logger.debug(EELFLoggerDelegate.debugLogger,"Creating docker service instance");
		this.cdmsClient = new CommonDataServiceRestClientImpl(cmnDataSvcEndPoinURL, cmnDataSvcUser, cmnDataSvcPwd, null);
		this.portalClient = new PortalRestClientImpl(portalURL);
		this.resourceUtils = new ResourceUtils(resourceLoader);
	}
	
	/*
	 * @Method Name : dockerizeFile Performs complete dockerization process.
	 */
	public String dockerizeFile(MetadataParser metadataParser, File localmodelFile, String solutionID, String deployment_env, File tempFolder) throws AcumosServiceException {
		File outputFolder = tempFolder;
		Metadata metadata = metadataParser.getMetadata();
		logger.debug(EELFLoggerDelegate.debugLogger,"Preparing app in: {}", tempFolder);
		if (metadata.getRuntimeName().equals("python")) {
			outputFolder = new File(tempFolder, "app");
			outputFolder.mkdir();
			
			Resource[] resources = null;
			
			if(deployment_env.equalsIgnoreCase("2"))
			{
				resources = resourceUtils.loadResources("classpath*:templates/dcae_python/*");
			}
			else
			{
				resources = this.resourceUtils.loadResources("classpath*:templates/python/*");
			}

			PythonDockerPreprator dockerPreprator = new PythonDockerPreprator(metadataParser, extraIndexURL,
					trustedHost,http_proxy);
			
			for (Resource resource : resources) {
				UtilityFunction.copyFile(resource, new File(outputFolder, resource.getFilename()));
			}
			try {
				File modelFolder = new File(outputFolder, "model");
				UtilityFunction.unzip(localmodelFile, modelFolder.getAbsolutePath());
			} catch (IOException e) {
				logger.error(EELFLoggerDelegate.errorLogger,"Python templatization failed: {}", e);
			}
			dockerPreprator.prepareDockerAppV2(outputFolder);
		} else if (metadata.getRuntimeName().equals("r")) {			
			RDockerPreparator dockerPreprator = new RDockerPreparator(metadataParser, http_proxy);
			Resource[] resources = this.resourceUtils.loadResources("classpath*:templates/r/*");
			for (Resource resource : resources) {
				UtilityFunction.copyFile(resource, new File(outputFolder, resource.getFilename()));
			}
			dockerPreprator.prepareDockerApp(outputFolder);
		} else if (metadata.getRuntimeName().equals("javaargus")) {
			try {
				String outputFile = UtilityFunction.getFileName(localmodelFile, outputFolder.toString());
				File tarFile = new File(outputFile);
				tarFile = UtilityFunction.deCompressGZipFile(localmodelFile, tarFile);
				UtilityFunction.unTarFile(tarFile, outputFolder);
			} catch (IOException e) {
				logger.error(EELFLoggerDelegate.errorLogger,"Java Argus templatization failed: {}", e);
			}
		} else if (metadata.getRuntimeName().equals("h2o")) {
			File plugin_root = new File(outputFolder, "plugin_root");
			plugin_root.mkdirs(); 
			File plugin_src = new File(plugin_root, "src");
			plugin_src.mkdirs();
			File plugin_classes = new File(plugin_root, "classes");
			plugin_classes.mkdirs();

			H2ODockerPreparator dockerPreprator = new H2ODockerPreparator(metadataParser);

			Resource[] resources = this.resourceUtils.loadResources("classpath*:templates/h2o/*");
			for (Resource resource : resources) {
				UtilityFunction.copyFile(resource, new File(outputFolder, resource.getFilename()));
			}
			try {
				UtilityFunction.unzip(localmodelFile, outputFolder.getAbsolutePath());

				String mm[] = modelOriginalName.split("\\.");

				File fd = new File(outputFolder.getAbsolutePath() + "/" + mm[0]);

				File ff[] = fd.listFiles();

				if (ff != null) {
					for (File f : ff) {
						FileUtils.copyFileToDirectory(f, outputFolder);
					}
					UtilityFunction.deleteDirectory(new File(outputFolder.getAbsolutePath() + "/" + modelOriginalName));
					UtilityFunction.deleteDirectory(new File(outputFolder.getAbsolutePath() + "/" + mm[0]));
				}

				// Creat solution id - success
			} catch (IOException e) {
				logger.error(EELFLoggerDelegate.errorLogger,"H2O templatization failed", e);
			}
			dockerPreprator.prepareDockerApp(outputFolder);

		} else if (metadata.getRuntimeName().equals("javageneric")) {
			File plugin_root = new File(outputFolder, "plugin_root");
			plugin_root.mkdirs();
			File plugin_src = new File(plugin_root, "src");
			plugin_src.mkdirs();
			File plugin_classes = new File(plugin_root, "classes");
			plugin_classes.mkdirs();

			JavaGenericDockerPreparator dockerPreprator = new JavaGenericDockerPreparator(metadataParser);
			Resource[] resources = this.resourceUtils.loadResources("classpath*:templates/javaGeneric/*");
			for (Resource resource : resources) {
				UtilityFunction.copyFile(resource, new File(outputFolder, resource.getFilename()));
			}

			try {

				UtilityFunction.unzip(localmodelFile, outputFolder.getAbsolutePath());

				String mm[] = modelOriginalName.split("\\.");

				File fd = new File(outputFolder.getAbsolutePath() + "/" + mm[0]);

				File ff[] = fd.listFiles();

				if (ff != null) {
					for (File f : ff) {
						FileUtils.copyFileToDirectory(f, outputFolder);
					}
					UtilityFunction.deleteDirectory(new File(outputFolder.getAbsolutePath() + "/" + modelOriginalName));
					UtilityFunction.deleteDirectory(new File(outputFolder.getAbsolutePath() + "/" + mm[0]));
				}

			} catch (IOException e) {
				logger.error(EELFLoggerDelegate.errorLogger,"Java-Generic templatization failed", e);
			}

			dockerPreprator.prepareDockerApp(outputFolder);

		} else {
			logger.error(EELFLoggerDelegate.errorLogger,"Unspported runtime {}", metadata.getRuntimeName());
			throw new AcumosServiceException(AcumosServiceException.ErrorCode.INVALID_PARAMETER,
					"Unspported runtime " + metadata.getRuntimeName());
		}
		logger.debug(EELFLoggerDelegate.debugLogger,"Resource List");
		listFilesAndFilesSubDirectories(outputFolder);
		logger.debug(EELFLoggerDelegate.debugLogger,"End of Resource List");
		logger.debug(EELFLoggerDelegate.debugLogger,"Started docker client");
		DockerClient dockerClient = DockerClientFactory.getDockerClient(dockerConfiguration);
		logger.debug(EELFLoggerDelegate.debugLogger,"Docker client created successfully");
		try {			
			logger.debug("Docker image creation started");
			String actualModelName = getActualModelName(metadata, solutionID);  
			CreateImageCommand createCMD = new CreateImageCommand(outputFolder, actualModelName,metadata.getVersion(), null, false, true);
			createCMD.setClient(dockerClient);
			createCMD.execute();
			logger.debug(EELFLoggerDelegate.debugLogger,"Docker image creation done");
			// put catch here
			// /Microservice/Docker image nexus creation -success

			// in catch /Microservice/Docker image nexus creation -failure

			// TODO: remove local image

			logger.debug(EELFLoggerDelegate.debugLogger,"Starting docker image tagging");
			String imageTagName = dockerConfiguration.getImagetagPrefix() + File.separator + actualModelName;
			
			String dockerImageURI = imageTagName + ":" + metadata.getVersion();
			
			TagImageCommand tagImageCommand = new TagImageCommand(actualModelName+ ":" + metadata.getVersion(),
					imageTagName, metadata.getVersion(), true, false);
			tagImageCommand.setClient(dockerClient);
			tagImageCommand.execute();
			logger.debug(EELFLoggerDelegate.debugLogger,"Docker image tagging completed successfully");

			logger.debug(EELFLoggerDelegate.debugLogger,"Starting pushing with Imagename:" + imageTagName + " and version : " + metadata.getVersion()
					+ " in nexus");
			PushImageCommand pushImageCmd = new PushImageCommand(imageTagName, metadata.getVersion(), "");
			pushImageCmd.setClient(dockerClient);
			pushImageCmd.execute();

			logger.debug(EELFLoggerDelegate.debugLogger,"Docker image URI : " + dockerImageURI);

			logger.debug(EELFLoggerDelegate.debugLogger,"Docker image pushed in nexus successfully");

			// Microservice/Docker image pushed to nexus -success

			return dockerImageURI;

		} finally {
			try {
				dockerClient.close();
			} catch (IOException e) {
				logger.error(EELFLoggerDelegate.errorLogger,"Fail to close docker client gracefully", e);
			}
		}
	}
	
	public void listFilesAndFilesSubDirectories(File directory) {

		File[] fList = directory.listFiles();

		for (File file : fList) {
			if (file.isFile()) {
				logger.debug(EELFLoggerDelegate.debugLogger,file.getName());
			} else if (file.isDirectory()) {
				listFilesAndFilesSubDirectories(file);
			}
		}
	}
	
	public String getActualModelName(Metadata metadata, String solutionID) {

		return metadata.getModelName() + "_" + solutionID;
	}
	
	public void revertbackOnboarding(Metadata metadata,String solutionID, String imageUri) throws AcumosServiceException {

		try {

			logger.debug(EELFLoggerDelegate.debugLogger,"In RevertbackOnboarding method");
			RepositoryLocation repositoryLocation = new RepositoryLocation();
			repositoryLocation.setId("1");
			repositoryLocation.setUrl(nexusEndPointURL);
			repositoryLocation.setUsername(nexusUserName);
			repositoryLocation.setPassword(nexusPassword);
			NexusArtifactClient nexusClient = new NexusArtifactClient(repositoryLocation);
			DockerClient dockerClient = DockerClientFactory.getDockerClient(dockerConfiguration);

			// Remove the image from docker registry
			// Check the value of imageUri, if it is null then do not delete the
			// image
			logger.debug(EELFLoggerDelegate.debugLogger,"Image Name from dockerize file method: " + imageUri);

			if (StringUtils.isNotBlank(imageUri)) {
				String imageTagName = dockerConfiguration.getImagetagPrefix() + "/" + getActualModelName(metadata, solutionID);
				
				logger.debug(EELFLoggerDelegate.debugLogger,"Image Name: " + imageTagName);
				DeleteImageCommand deleteImageCommand = new DeleteImageCommand(imageTagName, metadata.getVersion(), "");
				deleteImageCommand.setClient(dockerClient);
				deleteImageCommand.execute();
				logger.debug(EELFLoggerDelegate.debugLogger,"Successfully Deleted the image from Docker Registry");
			}

			if (metadata.getSolutionId() != null) {
				logger.debug(EELFLoggerDelegate.debugLogger,"Solution id: " + metadata.getSolutionId() + "  Revision id: " + metadata.getRevisionId());

				// get the Artifact IDs for given solution
				List<MLPArtifact> artifactids = cdmsClient.getSolutionRevisionArtifacts(metadata.getSolutionId(),
						metadata.getRevisionId());

				// check if artifactids is empty
				// Delete all the artifacts for given solution

				/*for (MLPArtifact mlpArtifact : artifactids) {
					String artifactId = mlpArtifact.getArtifactId();

					// Delete SolutionRevisionArtifact
					logger.debug(EELFLoggerDelegate.debugLogger,"Deleting Artifact: " + artifactId);
					cdmsClient.dropSolutionRevisionArtifact(metadata.getSolutionId(), metadata.getRevisionId(),
							artifactId);
					logger.debug(EELFLoggerDelegate.debugLogger,"Successfully Deleted the SolutionRevisionArtifact");

					// Delete Artifact
					cdmsClient.deleteArtifact(artifactId);
					logger.debug(EELFLoggerDelegate.debugLogger,"Successfully Deleted the Artifact");

					// Delete the file from the Nexus
					if (!(mlpArtifact.getArtifactTypeCode().equals("DI"))) {
						nexusClient.deleteArtifact(mlpArtifact.getUri());
						logger.debug(EELFLoggerDelegate.debugLogger,"Successfully Deleted the Artifact from Nexus");
					}
				}*/

			}
		} catch (Exception e) {
			logger.error(EELFLoggerDelegate.errorLogger,"Onboarding failed");
			logger.error(EELFLoggerDelegate.errorLogger,e.getMessage(), e);
			throw new AcumosServiceException(AcumosServiceException.ErrorCode.INTERNAL_SERVER_ERROR,
					"Fail to revert back onboarding changes : " + e.getMessage());
		}
	}
	
	public String getModelVersion(String solutionId, String revisionId) {
		MLPSolutionRevision revData;
		revData = cdmsClient.getSolutionRevision(solutionId, revisionId);

		return revData.getVersion();
	}
	
	public static void createLogFile(String logPath) {
		LogBean logBean = LogThreadLocal.get();
		String fileName = logBean.getFileName();

		File file = new java.io.File(logPath);
		file.mkdirs();
		try {
			File f1 = new File(file.getPath() + File.separator + fileName);
			if (!f1.exists()) {
				f1.createNewFile();
			}
			logger.debug(EELFLoggerDelegate.debugLogger,
					"Log file created successfully " + f1.getAbsolutePath());
		} catch (Exception e) {
			//info to avoid infinite loop.logger.debug call again calls addlog method
			logger.info("Failed while creating log file " + e.getMessage());
		}

	}
	
	public String getModelOriginalName() {
		return modelOriginalName;
	}

	public void setModelOriginalName(String modelOriginalName) {
		this.modelOriginalName = modelOriginalName;
	}
}
