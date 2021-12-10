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

package org.acumos.microservice.services.impl;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;

import org.acumos.cds.CodeNameType;
import org.acumos.cds.client.CommonDataServiceRestClientImpl;
import org.acumos.cds.domain.MLPCodeNamePair;
import org.acumos.cds.domain.MLPSiteConfig;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.cds.domain.MLPTask;
import org.acumos.cds.domain.MLPUser;
import org.acumos.microservice.component.docker.DockerizeModel;
import org.acumos.microservice.services.DockerService;
import org.acumos.onboarding.common.exception.AcumosServiceException;
import org.acumos.onboarding.common.models.OnboardingNotification;
import org.acumos.onboarding.common.models.ServiceResponse;
import org.acumos.onboarding.common.utils.LogBean;
import org.acumos.onboarding.common.utils.LogThreadLocal;
import org.acumos.onboarding.common.utils.LoggerDelegate;
import org.acumos.onboarding.common.utils.OnboardingConstants;
import org.acumos.onboarding.common.utils.UtilityFunction;
import org.acumos.onboarding.component.docker.preparation.Metadata;
import org.acumos.onboarding.component.docker.preparation.MetadataParser;
import org.acumos.onboarding.logging.OnboardingLogConstants;
import org.acumos.onboarding.services.impl.CommonOnboarding;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController
@RequestMapping(value = "/v2")
@Api(value = "Operation to to onboard a ML model", tags = "Onboarding Service APIs")

/**
 *
 * @author *****
 *
 */
public class GenerateMicroserviceController extends DockerizeModel implements DockerService {
	private static Logger log = LoggerFactory.getLogger(GenerateMicroserviceController.class);
	LoggerDelegate logger = new LoggerDelegate(log);
	Map<String, String> artifactsDetails = new HashMap<>();

	@Autowired
	CommonOnboarding commonOnboarding;
	public static final String logPath = "/maven/logs/microservice-generation/applog";

	public GenerateMicroserviceController() {
		// Property values are injected after the constructor finishes
	}

	/************************************************
	 * End of Authentication
	 *****************************************************/

	@Override
	@Consumes(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Dockerize the model artifact by solution and revision ID", response = ServiceResponse.class)
	@ApiResponses(value = {
			@ApiResponse(code = 500, message = "Something bad happened", response = ServiceResponse.class),
			@ApiResponse(code = 400, message = "Invalid request", response = ServiceResponse.class),
			@ApiResponse(code = 401, message = "Unauthorized User", response = ServiceResponse.class) })
	@RequestMapping(value = "/generateMicroservice", method = RequestMethod.POST, produces = "application/json")
	public ResponseEntity<ServiceResponse> generateMicroservice(HttpServletRequest request,
			@RequestParam(required = true) String solutioId, String revisionId,
			@RequestParam(required = false) String modName,
			@RequestParam(value = "deployment_env", required = false) String deploy_env,
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestHeader(value = "tracking_id", required = false) String trackingID,
			@RequestHeader(value = "provider", required = false) String provider,
			@RequestHeader(value = "Request-ID", required = false) String request_id,
			@RequestHeader(value = "deploy", required = false) String deployStr) throws AcumosServiceException {

		// If trackingID is provided in the header create a
		// OnboardingNotification object that will be used to update
		// status
		// against that trackingID
		if (trackingID != null) {
			logger.debug("Tracking ID: " + trackingID);
		} else {
			trackingID = UUID.randomUUID().toString();
			logger.debug("Tracking ID: " + trackingID);
		}

		boolean deploy = false;
		
		if(deployStr != null) {
			if (deployStr.equals("true")) {
				deploy = true;
			}
		}

		String fileName = "MicroserviceGenerationLog.txt";
		// setting log filename in ThreadLocal
		LogBean logBean = new LogBean();
		logBean.setFileName(fileName);
		logBean.setLogPath(logPath + File.separator + trackingID);

		LogThreadLocal logThread = new LogThreadLocal();
		logThread.set(logBean);
		// create log file to capture logs as artifact
		createLogFile(logBean.getLogPath());

		String buildVersion = UtilityFunction.getProjectVersion();
		logger.debug("Microservice-Generation version : " + buildVersion);

		String deployment_env = null;
		OnboardingNotification onboardingStatus = null;

		if (deploy_env == null || deploy_env.isEmpty()) {
			deployment_env = "1";
		} else {
			deployment_env = deploy_env.trim();
		}

		logger.debug("deployment_env: " + deployment_env);

		if (request_id != null) {
			logger.debug("Request ID: " + request_id);
		} else {
			request_id = UUID.randomUUID().toString();
			logger.debug("Request ID Created: " + request_id);
		}

		onboardingStatus = new OnboardingNotification(cmnDataSvcEndPoinURL, cmnDataSvcUser, cmnDataSvcPwd, request_id);
		onboardingStatus.setRequestId(request_id);
		MDC.put(OnboardingLogConstants.MDCs.REQUEST_ID, request_id);
		logger.debug("MicroService Async Flag: " + microServiceAsyncFlag, logBean);

		if (microServiceAsyncFlag) {

			return generateMicroserviceAsyncDef(onboardingStatus, solutioId, revisionId, modName, deployment_env,
					authorization, trackingID, provider, deploy);

		} else {

			String artifactName = null;
			File files = null;
			List<String> artifactNameList = new ArrayList<String>();
			Metadata mData = null;
			MLPSolution mlpSolution = new MLPSolution();
			MLPSolutionRevision revision;
			MLPTask task = null;

			try {

				// Call to validate Token.....!
				String ownerId = commonOnboarding.validate(authorization, provider);
				if (ownerId != null && !ownerId.isEmpty()) {

					logger.debug("Token validation successful");
					MDC.put(OnboardingLogConstants.MDCs.USER, ownerId);

				} else {

					logger.error("Either Username/Password is invalid.");
					MDC.put(OnboardingLogConstants.MDCs.RESPONSE_STATUS_CODE,
							OnboardingLogConstants.ResponseStatus.ERROR.name());
					MDC.put(OnboardingLogConstants.MDCs.RESPONSE_DESCRIPTION, "Either Username/Password is invalid.");
					// throw new
					// AcumosServiceException(AcumosServiceException.ErrorCode.INVALID_TOKEN,
					// "Either Username/Password is invalid.");
					return new ResponseEntity<ServiceResponse>(
							ServiceResponse.errorResponse("" + HttpStatus.UNAUTHORIZED, "Unauthorized User"),
							HttpStatus.UNAUTHORIZED);
				}

				String modelName = null;

				logger.debug("Fetching model from Nexus...!");

				// Nexus Integration....!

				DownloadModelArtifacts download = new DownloadModelArtifacts();
				logger.debug("solutioId: " + solutioId, "revisionId: " + revisionId);
				artifactNameList = download.getModelArtifacts(solutioId, revisionId, cmnDataSvcUser, cmnDataSvcPwd,
						nexusEndPointURL, nexusUserName, nexusPassword, cmnDataSvcEndPoinURL);

				logger.debug("Number of artifacts: " + artifactNameList.size());

				logger.debug("Starting Microservice Generation", logBean);

				String modelId = UtilityFunction.getGUID();
				File outputFolder = new File("tmp", modelId);
				outputFolder.mkdirs();

				files = new File("model");

				MultipartFile model = null, meta = null, proto = null, license = null;

				File modelFile = null, MetaFile = null, protoFile = null, licenseFile = null;

				for (String name : artifactNameList) {
					if (!name.toLowerCase().contains("license") && name.toLowerCase().contains(".json")) {
						logger.debug("MetaFile: " + name);
						MetaFile = new File(files, name);
						UtilityFunction.copyFile(MetaFile, new File(outputFolder, name));
					} else if (name.toLowerCase().contains(".proto")) {
						logger.debug("ProtoFile: " + name);
						protoFile = new File(files, name);
						UtilityFunction.copyFile(protoFile, new File(outputFolder, name));
					} else if (name.toLowerCase().contains("license") && name.toLowerCase().contains(".json")) {
						logger.debug("license: " + name);
						licenseFile = new File(files, name);
						UtilityFunction.copyFile(licenseFile, new File(outputFolder, name));
					} else {
						logger.debug("ModelFile: " + name);
						modelFile = new File(files, name);
						UtilityFunction.copyFile(modelFile, new File(outputFolder, name));
					}
				}

				if (modName != null) {
					Object obj = new JSONParser().parse(new FileReader(MetaFile));
					JSONObject jo = (JSONObject) obj;
					jo.put("name", modName);
					String jsonFile = jo.toString();
					FileOutputStream fout = new FileOutputStream(MetaFile);
					fout.write(jsonFile.getBytes());
					fout.close();
				}

				if ((modelFile.exists()) && (MetaFile.exists()) && (protoFile.exists())) {
					metadataParser = new MetadataParser(MetaFile);
					mData = metadataParser.getMetadata();
					mData.setOwnerId(ownerId);

					FileInputStream fisModel = new FileInputStream(modelFile);
					model = new MockMultipartFile("Model", modelFile.getName(), "", fisModel);

					FileInputStream fisMeta = new FileInputStream(MetaFile);
					meta = new MockMultipartFile("Metadata", MetaFile.getName(), "", fisMeta);

					FileInputStream fisProto = new FileInputStream(protoFile);
					proto = new MockMultipartFile("Proto", protoFile.getName(), "", fisProto);

					if (deployment_env.equalsIgnoreCase("2")) {

						List<MLPSolution> solList = commonOnboarding.getExistingSolution(mData);

						boolean isListEmpty = solList.isEmpty();

						if (isListEmpty) {

							mlpSolution = commonOnboarding.createSolution(mData, onboardingStatus);
							mData.setSolutionId(mlpSolution.getSolutionId());
							logger.debug("New solution created Successfully for ONAP" + mlpSolution.getSolutionId());
						} else {
							logger.debug("Existing solution found for ONAP model name " + solList.get(0).getName());
							mlpSolution = solList.get(0);
							mData.setSolutionId(mlpSolution.getSolutionId());
							mlpSolution.setName(mData.getSolutionName());
							// mlpSolution.setDescription(mData.getSolutionName());
							mlpSolution.setUserId(mData.getOwnerId());
						}

						revision = commonOnboarding.createSolutionRevision(mData);
						logger.debug("Revision created Successfully  for ONAP" + revision.getRevisionId());
						mData.setRevisionId(revision.getRevisionId());

						modelName = mData.getModelName() + "_" + mData.getSolutionId();
					} else if (solutioId != null && revisionId != null) {
						mData.setSolutionId(solutioId);
						mData.setRevisionId(revisionId);
						mlpSolution.setSolutionId(solutioId);
						mlpSolution.setName(mData.getSolutionName());
						// mlpSolution.setDescription(mData.getSolutionName());
						mlpSolution.setUserId(mData.getOwnerId());
					} else {
						logger.error("Invalid Request................");
						throw new AcumosServiceException(AcumosServiceException.ErrorCode.INVALID_PARAMETER,
								"Invalid Request...............");
					}

					String version = mData.getVersion();

					if (version == null) {
						version = getModelVersion(mData.getSolutionId(), mData.getRevisionId());
						mData.setVersion(version);
					}

					MLPUser shareUser = null;

					/*
					 * // try { // 'authorization' represents JWT token here...! if (authorization
					 * == null) { logger.error( "Token Not Available...!"); throw new
					 * AcumosServiceException(AcumosServiceException.ErrorCode.OBJECT_NOT_FOUND,
					 * "Token Not Available...!"); }
					 */

					String imageUri = null;
					if (ownerId != null && !ownerId.isEmpty()) {

						logger.debug("Dockerization request recieved with " + model.getOriginalFilename());

						modelOriginalName = model.getOriginalFilename();
						boolean isSuccess = false;

						try {

							// Solution id creation completed
							// Notify Creation of solution ID is successful
							if (onboardingStatus != null) {
								// set solution Id
								if (mlpSolution.getSolutionId() != null) {
									onboardingStatus.setSolutionId(mlpSolution.getSolutionId());
								}
								// set revision id
								if (mData.getRevisionId() != null) {
									onboardingStatus.setRevisionId(mData.getRevisionId());
								}
							}
							// Notify Create docker image has started
							if (onboardingStatus != null) {

								logger.debug("Setting values in Task object");

								task = new MLPTask();
								task.setTaskCode("MS");
								task.setStatusCode("ST");
								task.setName("MicroserviceGeneration");
								task.setUserId(ownerId);
								task.setCreated(Instant.now());
								task.setModified(Instant.now());
								task.setTrackingId(trackingID);
								task.setSolutionId(mlpSolution.getSolutionId());
								task.setRevisionId(mData.getRevisionId());

								onboardingStatus.setTrackingId(trackingID);
								onboardingStatus.setUserId(ownerId);
								logger.debug("Task Details: " + task.toString());

								task = cdmsClient.createTask(task);

								logger.debug("TaskID: " + task.getTaskId());

								onboardingStatus.setTaskId(task.getTaskId());

								onboardingStatus.notifyOnboardingStatus("Dockerize", "ST",
										"Create Docker Image Started for solution " + mData.getSolutionId());
							}

							try { 
								imageUri = dockerizeFile(metadataParser, modelFile, mlpSolution.getSolutionId(),
										deployment_env, outputFolder, task.getTaskId(), mData.getSolutionId(),
										trackingID, logBean);

							} catch (Exception e) {
								// Notify Create docker image failed
								if (onboardingStatus != null) {
									onboardingStatus.notifyOnboardingStatus("Dockerize", "FA", e.getMessage());
								}

								MDC.put(OnboardingLogConstants.MDCs.RESPONSE_STATUS_CODE,
										OnboardingLogConstants.ResponseStatus.ERROR.name());
								MDC.put(OnboardingLogConstants.MDCs.RESPONSE_DESCRIPTION, e.getMessage());
								logger.error("Error " + e);
								throw e;
							}
							if (!createImageViaJenkins) {
								// Notify Create docker image is successful
								if (onboardingStatus != null) {
									onboardingStatus.notifyOnboardingStatus("Dockerize", "SU",
											"Created Docker Image Successfully for solution " + mData.getSolutionId());
								}

								// Add artifacts started. Notification will be handed by
								// addArtifact method itself for started/success/failure
								artifactsDetails = getArtifactsDetails();
								commonOnboarding.addArtifact(mData, imageUri, getArtifactTypeCode("Docker Image"),
										onboardingStatus);

								if (deployment_env.equalsIgnoreCase("2")) {
									logger.debug("OutputFolderPath: " + outputFolder);
									logger.debug("AbsolutePath OutputFolderPath: " + outputFolder.getAbsolutePath());
									addDCAEArrtifacts(mData, outputFolder, mlpSolution.getSolutionId(),
											onboardingStatus);
								}

								isSuccess = true;
								MDC.put(OnboardingLogConstants.MDCs.RESPONSE_CODE, HttpStatus.CREATED.toString());
							}

							return new ResponseEntity<ServiceResponse>(ServiceResponse.successResponse(mlpSolution,
									task.getTaskId(), trackingID, imageUri), HttpStatus.CREATED);

						} finally {

							try {
								UtilityFunction.deleteDirectory(outputFolder);
								task.setModified(Instant.now());
								logger.debug(
										"createImageViaJenkins in finally of Async process = " + createImageViaJenkins);
								if (!createImageViaJenkins) {
									if (isSuccess == false) {
										logger.debug("Onboarding Failed, Reverting failed solutions and artifacts.");
										task.setStatusCode("FA");
										logger.debug("MLP task updating with the values =" + task.toString());
										cdmsClient.updateTask(task);
										if (metadataParser != null && mData != null) {
											revertbackOnboarding(metadataParser.getMetadata(),
													mlpSolution.getSolutionId(), imageUri);
										}
									}
									if (isSuccess == true) {
										task.setStatusCode("SU");
										logger.debug("MLP task updating with the values =" + task.toString());
										cdmsClient.updateTask(task);
									}
									// push docker build log into nexus
									File file = new java.io.File(
											logPath + File.separator + trackingID + File.separator + fileName);
									logger.debug("Log file length " + file.length());
									logger.debug("Log file Path " + file.getPath() + " Absolute Path : "
											+ file.getAbsolutePath() + " Canonical Path: " + file.getCanonicalFile());

									if (metadataParser != null && mData != null) {
										logger.debug("Adding of log artifacts into nexus started " + fileName);

										String nexusArtifactID = "MicroserviceGenerationLog";

										commonOnboarding.addArtifact(mData, file, "LG", nexusArtifactID,
												onboardingStatus);
										MDC.put(OnboardingLogConstants.MDCs.RESPONSE_STATUS_CODE,
												OnboardingLogConstants.ResponseStatus.COMPLETED.name());
										logger.debug("Artifacts log pushed to nexus successfully" + fileName);
									}

									// deploy the model
									if (deploy) {
										// configKey=deployment_jenkins_config. Hard Coding it for now. Can be fetched
										// from deployment yaml
										ResponseEntity<ServiceResponse> responseEntity = deployModel("deployment_jenkins_config", cdmsClient, imageUri_test);
										logger.debug("Response Code of Model Deployment = "+responseEntity.getStatusCode());
									}

									// delete the Docker image
									/*
									 * logger.debug(EELFLoggerDelegate.
									 * debugLogger,"Docker image Deletion started -> image = "+imageUri+", tag = "
									 * +mData.getVersion()); DeleteImageCommand deleteCMD = new
									 * DeleteImageCommand(imageUri, mData.getVersion(), null); deleteCMD.execute();
									 * logger.debug("Docker image Deletion Done");
									 */

									// delete log file
									UtilityFunction.deleteDirectory(file);
								}
								logThread.unset();
								mData = null;
							} catch (AcumosServiceException e) {
								mData = null;
								HttpStatus httpCode = HttpStatus.INTERNAL_SERVER_ERROR;
								MDC.put(OnboardingLogConstants.MDCs.RESPONSE_STATUS_CODE,
										OnboardingLogConstants.ResponseStatus.ERROR.name());
								MDC.put(OnboardingLogConstants.MDCs.RESPONSE_DESCRIPTION, httpCode.toString());
								logger.error("RevertbackOnboarding Failed");
								return new ResponseEntity<ServiceResponse>(
										ServiceResponse.errorResponse(e.getErrorCode(), e.getMessage()), httpCode);
							}
						}
					} else {
						try {
							MDC.put(OnboardingLogConstants.MDCs.RESPONSE_STATUS_CODE,
									OnboardingLogConstants.ResponseStatus.ERROR.name());
							MDC.put(OnboardingLogConstants.MDCs.RESPONSE_DESCRIPTION,
									"Either Username/Password is invalid");
							logger.error("Either Username/Password is invalid.");
							throw new AcumosServiceException(AcumosServiceException.ErrorCode.INVALID_TOKEN,
									"Either Username/Password is invalid.");
						} catch (AcumosServiceException e) {
							return new ResponseEntity<ServiceResponse>(
									ServiceResponse.errorResponse(e.getErrorCode(), e.getMessage()),
									HttpStatus.UNAUTHORIZED);
						}
					}
					// }
				} else {
					logger.error("Model artifacts not available..!");
					throw new AcumosServiceException("Model artifacts not available..!");
				}

			} catch (AcumosServiceException e) {

				HttpStatus httpCode = HttpStatus.INTERNAL_SERVER_ERROR;
				MDC.put(OnboardingLogConstants.MDCs.RESPONSE_STATUS_CODE,
						OnboardingLogConstants.ResponseStatus.ERROR.name());
				logger.error(e.getErrorCode() + "  " + e.getMessage());
				if (e.getErrorCode().equalsIgnoreCase(OnboardingConstants.INVALID_PARAMETER)) {
					httpCode = HttpStatus.BAD_REQUEST;
				}
				MDC.put(OnboardingLogConstants.MDCs.RESPONSE_DESCRIPTION, httpCode.toString());
				return new ResponseEntity<ServiceResponse>(
						ServiceResponse.errorResponse(e.getErrorCode(), e.getMessage()), httpCode);
			} catch (HttpClientErrorException e) {
				MDC.put(OnboardingLogConstants.MDCs.RESPONSE_STATUS_CODE,
						OnboardingLogConstants.ResponseStatus.ERROR.name());
				MDC.put(OnboardingLogConstants.MDCs.RESPONSE_DESCRIPTION, e.getMessage());
				// Handling #401
				if (HttpStatus.UNAUTHORIZED == e.getStatusCode() || HttpStatus.BAD_REQUEST == e.getStatusCode()) {
					logger.debug("Unauthorized User - Either Username/Password is invalid.");
					return new ResponseEntity<ServiceResponse>(
							ServiceResponse.errorResponse("" + HttpStatus.UNAUTHORIZED, "Unauthorized User"),
							HttpStatus.UNAUTHORIZED);
				} else {
					logger.error(e.getMessage());
					e.printStackTrace();
					return new ResponseEntity<ServiceResponse>(
							ServiceResponse.errorResponse("" + e.getStatusCode(), e.getMessage()), e.getStatusCode());
				}
			} catch (Exception e) {
				MDC.put(OnboardingLogConstants.MDCs.RESPONSE_STATUS_CODE,
						OnboardingLogConstants.ResponseStatus.ERROR.name());
				MDC.put(OnboardingLogConstants.MDCs.RESPONSE_DESCRIPTION, e.getMessage());
				logger.error(e.getMessage());
				e.printStackTrace();
				if (e instanceof AcumosServiceException) {
					return new ResponseEntity<ServiceResponse>(
							ServiceResponse.errorResponse(((AcumosServiceException) e).getErrorCode(), e.getMessage()),
							HttpStatus.INTERNAL_SERVER_ERROR);
				} else {
					return new ResponseEntity<ServiceResponse>(ServiceResponse
							.errorResponse(AcumosServiceException.ErrorCode.UNKNOWN.name(), e.getMessage()),
							HttpStatus.INTERNAL_SERVER_ERROR);
				}
			}
		}
	}

	public ResponseEntity<ServiceResponse> deployModel(String configKey, CommonDataServiceRestClientImpl cdmsClient, String imageUri) {

		MLPSiteConfig mlpSiteConfig = null;
		String jsr = null;
		String jjb = null;
		String param = null;
		String jlog = null;
		String jst = null;
		String paramValue = imageUri;
		try {   
			log.debug("deployModel Method started ... ");
			if(cdmsClient == null) {
				cdmsClient = new CommonDataServiceRestClientImpl(cmnDataSvcEndPoinURL, cmnDataSvcUser, cmnDataSvcPwd,
							null);
				log.debug("cdmsClient = "+cdmsClient);
			}
			log.debug("cdmsClient = "+cdmsClient+", configKey = "+configKey);
			mlpSiteConfig = cdmsClient.getSiteConfig(configKey);
			log.debug("MLPSiteConfig = "+mlpSiteConfig);

			if (mlpSiteConfig != null) {

				log.debug("MLPSiteConfig ConfigKey = "+mlpSiteConfig.getConfigKey()+"\nMLPSiteConfig ConfigValue = "+mlpSiteConfig.getConfigValue());
				ObjectMapper mapper = new ObjectMapper();
				@SuppressWarnings("unchecked")
				Map<String, Object> slidesConfigJson = mapper.readValue(mlpSiteConfig.getConfigValue(), Map.class);
				for (Map.Entry<String, Object> entry : slidesConfigJson.entrySet()) {

					switch (entry.getKey()) {

					case "jsr":
						jsr = entry.getValue().toString();
						log.debug("jsr = " + jsr);
						break;

					case "jjb":
						jjb = entry.getValue().toString();
						log.debug("jjb = " + jjb);
						break;

					case "param":
						param = entry.getValue().toString();
						log.debug("param = " + param);
						break;

					case "jlog":
						jlog = entry.getValue().toString();
						log.debug("jlog = " + jlog);
						break;

					case "jst":
						jst = entry.getValue().toString();
						log.debug("jst = " + jst + "\n\n");
						break;

					default:
						break;

					}
				}
			} else {
				log.error("ERROR: MLPSiteConfig is NULL. Nothing is fetched from CDS.");
				throw new AcumosServiceException(AcumosServiceException.ErrorCode.OBJECT_NOT_FOUND,
						"Exception occurred while fetching siteConfig configKey from CDS.");
			}

			// call the Jenkins Job for Deploying the model
			log.debug("paramValue :" +paramValue);
			callDeploymentJenkinsJob(jsr, jjb, param, paramValue, jlog, jst);
			return new ResponseEntity<ServiceResponse>(ServiceResponse.successResponse(), HttpStatus.CREATED);

		} catch (AcumosServiceException e) {
			log.error("Exception Occurred Fetching Site Configuration Details : ", e.getMessage());
			HttpStatus httpCode = HttpStatus.INTERNAL_SERVER_ERROR;
			MDC.put(OnboardingLogConstants.MDCs.RESPONSE_STATUS_CODE,
					OnboardingLogConstants.ResponseStatus.ERROR.name());
			MDC.put(OnboardingLogConstants.MDCs.RESPONSE_DESCRIPTION, httpCode.toString());
			log.error("Deployment of the model Failed");

			return new ResponseEntity<ServiceResponse>(ServiceResponse.errorResponse(e.getErrorCode(), e.getMessage()),
					httpCode);

		} catch (Exception ex) {
			log.error("Exception while Deploying Model : ", ex.getMessage());
			ex.printStackTrace();
			return new ResponseEntity<ServiceResponse>(
					ServiceResponse.errorResponse(AcumosServiceException.ErrorCode.UNKNOWN.name(), ex.getMessage()),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private void callDeploymentJenkinsJob(String jsr, String jjb, String param, String paramValue, String jlog,
			String jst) throws AcumosServiceException {

		try {

			log.debug("Calling the Deployment Jenkins Job");
			String jsrUri = jsr + "/job/" + jjb + "/buildWithParameters";
			log.debug("jsrUri = "+jsrUri);
			URL jsrURL = new URL(jsrUri); // Jenkins URL
			log.debug("jsrURL = "+jsrURL);
			String authStr = jlog + ":" + jst;
			log.debug("authStr = "+authStr);
			String encoding = Base64.getEncoder().encodeToString(authStr.getBytes("utf-8"));

			HttpURLConnection connection = (HttpURLConnection) jsrURL.openConnection();
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			connection.setRequestProperty("Authorization", "Basic " + encoding);

			log.debug("jsrUri = " + jsrUri + "\nparam = " + param + "\nparamValue = " + paramValue);
			
			//Setting the parameters. There may be a need to handle them in jenkins job
			String urlParams = param + "=" + paramValue;

			byte[] postData = urlParams.getBytes("utf-8");
			try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
				wr.write(postData);
			}

			InputStream content = connection.getInputStream();
			BufferedReader in = new BufferedReader(new InputStreamReader(content));
			String line;
			while ((line = in.readLine()) != null) {
				log.debug(line);
				System.out.println(line);
			}
			log.debug("Connection Response Code - " + connection.getResponseCode());
			System.out.println("Done - " + connection.getResponseCode());

		} catch (Exception e) {
			e.printStackTrace();
			log.error("Exception occurred while executing callDeploymentJenkinsJob method : ", e.getMessage());
			throw new AcumosServiceException(AcumosServiceException.ErrorCode.CONNECTION_ISSUE,
					"Exception occurred while connecting to deployment Jenkins job.");
		}
	}

	private Map<String, String> getArtifactsDetails() {
		List<MLPCodeNamePair> typeCodeList = cdmsClient.getCodeNamePairs(CodeNameType.ARTIFACT_TYPE);
		Map<String, String> artifactsDetails = new HashMap<>();
		if (!typeCodeList.isEmpty()) {
			for (MLPCodeNamePair codeNamePair : typeCodeList) {
				artifactsDetails.put(codeNamePair.getName(), codeNamePair.getCode());
			}
		}
		return artifactsDetails;
	}

	private String getArtifactTypeCode(String artifactTypeName) {
		String typeCode = artifactsDetails.get(artifactTypeName);
		return typeCode;
	}

	private void addDCAEArrtifacts(Metadata mData, File outputFolder, String solutionID,
			OnboardingNotification onboardingStatus) throws AcumosServiceException {

		File filePathoutputF = new File(outputFolder, "app");

		File anoIn = new File(filePathoutputF, "anomaly-in.json");
		File anoOut = new File(filePathoutputF, "anomaly-out.json");
		File compo = new File(filePathoutputF, "component.json");
		File ons = new File(filePathoutputF, "onsdemo1.yaml");

		try {

			// call microservice
			logger.debug("DCAE ADD Artifact Started ");
			commonOnboarding.addArtifact(mData, anoIn, getArtifactTypeCode("Metadata"), "anomaly-in", onboardingStatus);
			commonOnboarding.addArtifact(mData, anoOut, getArtifactTypeCode("Metadata"), "anomaly-out",
					onboardingStatus);
			commonOnboarding.addArtifact(mData, compo, getArtifactTypeCode("Metadata"), "component", onboardingStatus);
			commonOnboarding.addArtifact(mData, ons, getArtifactTypeCode("Metadata"), "onsdemo1", onboardingStatus);
			logger.debug("DCAE ADD Artifact End ");
		}

		catch (AcumosServiceException e) {
			logger.error("Exception occured while adding DCAE Artifacts " + e);
			throw e;
		} catch (Exception e) {
			logger.error("Exception occured while adding DCAE Artifacts " + e);
			throw e;
		}
	}

	public String getCmnDataSvcEndPoinURL() {
		return cmnDataSvcEndPoinURL;
	}

	public String getCmnDataSvcUser() {
		return cmnDataSvcUser;
	}

	public String getCmnDataSvcPwd() {
		return cmnDataSvcPwd;
	}
}
