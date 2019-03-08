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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;

import org.acumos.cds.CodeNameType;
import org.acumos.cds.domain.MLPCodeNamePair;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.cds.domain.MLPTask;
import org.acumos.cds.domain.MLPUser;
import org.acumos.microservice.component.docker.DockerizeModel;
import org.acumos.microservice.component.docker.cmd.DeleteImageCommand;
import org.acumos.microservice.services.DockerService;
import org.acumos.onboarding.common.exception.AcumosServiceException;
import org.acumos.onboarding.common.models.OnboardingNotification;
import org.acumos.onboarding.common.models.ServiceResponse;
import org.acumos.onboarding.common.utils.EELFLoggerDelegate;
import org.acumos.onboarding.common.utils.LogBean;
import org.acumos.onboarding.common.utils.LogThreadLocal;
import org.acumos.onboarding.common.utils.OnboardingConstants;
import org.acumos.onboarding.common.utils.UtilityFunction;
import org.acumos.onboarding.component.docker.preparation.Metadata;
import org.acumos.onboarding.component.docker.preparation.MetadataParser;
import org.acumos.onboarding.logging.OnboardingLogConstants;
import org.acumos.onboarding.services.impl.CommonOnboarding;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController 
@RequestMapping(value="/v2")
@Api(value="Operation to to onboard a ML model",tags="Onboarding Service APIs")

/**
 * 
 * @author *****
 *
 */
public class GenerateMicroserviceController extends DockerizeModel implements DockerService {
	private static EELFLoggerDelegate logger = EELFLoggerDelegate.getLogger(GenerateMicroserviceController.class);
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
			@RequestHeader(value = "Request-ID", required = false) String request_id)
			throws AcumosServiceException {
		
		String deployment_env = null;
		String artifactName = null;
		File files = null;
		List<String> artifactNameList = new ArrayList<String>();
		Metadata mData = null;
		OnboardingNotification onboardingStatus = null;
		MLPSolution mlpSolution = new MLPSolution();
		MLPSolutionRevision revision;
		
		if (deploy_env == null || deploy_env.isEmpty()){
			deployment_env = "1";
		} else {
			deployment_env = deploy_env.trim();
		}
		
		logger.debug(EELFLoggerDelegate.debugLogger, "deployment_env: "+ deployment_env);
		
		if (request_id != null) {
			logger.debug(EELFLoggerDelegate.debugLogger, "Request ID: "+ request_id);
		} else {
			request_id = UUID.randomUUID().toString();
			logger.debug(EELFLoggerDelegate.debugLogger, "Request ID Created: "+ request_id);
		}
		
		// If trackingID is provided in the header create a
		// OnboardingNotification object that will be used to update
		// status
		// against that trackingID
		if (trackingID != null) {
			logger.debug(EELFLoggerDelegate.debugLogger, "Tracking ID: "+ trackingID);
		} else {
			trackingID = UUID.randomUUID().toString();
			logger.debug(EELFLoggerDelegate.debugLogger, "Tracking ID: "+ trackingID);
		}	
		
		onboardingStatus = new OnboardingNotification(cmnDataSvcEndPoinURL, cmnDataSvcUser, cmnDataSvcPwd, request_id);
		onboardingStatus.setRequestId(request_id);
		MDC.put(OnboardingLogConstants.MDCs.REQUEST_ID, request_id);
		
		try {

			// Call to validate Token.....!
			String ownerId = commonOnboarding.validate(authorization, provider);
			if (ownerId != null && !ownerId.isEmpty()) {

				logger.debug(EELFLoggerDelegate.debugLogger, "Token validation successful");

			} else {

				logger.error(EELFLoggerDelegate.errorLogger, "Either Username/Password is invalid.");
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
			logger.debug(EELFLoggerDelegate.debugLogger, "Microservice-Generation version : " + buildVersion);

			String modelName = null;

			logger.debug(EELFLoggerDelegate.debugLogger, "Fetching model from Nexus...!");

			// Nexus Integration....!

			DownloadModelArtifacts download = new DownloadModelArtifacts();
			logger.debug(EELFLoggerDelegate.debugLogger, "solutioId: "+ solutioId, "revisionId: "+ revisionId);
			artifactNameList = download.getModelArtifacts(solutioId, revisionId, cmnDataSvcUser, cmnDataSvcPwd,
					nexusEndPointURL, nexusUserName, nexusPassword, cmnDataSvcEndPoinURL);

			logger.debug(EELFLoggerDelegate.debugLogger, "Number of artifacts: ", artifactNameList.size());

			logger.debug(EELFLoggerDelegate.debugLogger, "Starting Microservice Generation");

			String modelId = UtilityFunction.getGUID();
			File outputFolder = new File("tmp", modelId);
			outputFolder.mkdirs();

			files = new File("model");

			MultipartFile model = null, meta = null, proto = null, licence = null;

			File modelFile = null, MetaFile = null, protoFile = null, licenceFile = null;

			for (String name : artifactNameList) {
				if (name.contains(".json")) {
					logger.debug(EELFLoggerDelegate.debugLogger, "MetaFile: "+ name);
					MetaFile = new File(files, name);
					UtilityFunction.copyFile(MetaFile, new File(outputFolder, name));
				} else if (name.contains(".proto")) {
					logger.debug(EELFLoggerDelegate.debugLogger, "ProtoFile: "+ name);
					protoFile = new File(files, name);
					UtilityFunction.copyFile(protoFile, new File(outputFolder, name));
				} else if (name.equalsIgnoreCase("licence.txt")) {
					logger.debug(EELFLoggerDelegate.debugLogger, "Licence: "+ name);
					licenceFile = new File(files, name);
					UtilityFunction.copyFile(licenceFile, new File(outputFolder, name));
				} else {
					logger.debug(EELFLoggerDelegate.debugLogger, "ModelFile: "+ name);
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
						logger.debug(EELFLoggerDelegate.debugLogger,
								"New solution created Successfully for ONAP" + mlpSolution.getSolutionId());
					} else {
						logger.debug(EELFLoggerDelegate.debugLogger,
								"Existing solution found for ONAP model name " + solList.get(0).getName());
						mlpSolution = solList.get(0);
						mData.setSolutionId(mlpSolution.getSolutionId());
						mlpSolution.setName(mData.getSolutionName());
						// mlpSolution.setDescription(mData.getSolutionName());
						mlpSolution.setUserId(mData.getOwnerId());
					}

					revision = commonOnboarding.createSolutionRevision(mData);
					logger.debug(EELFLoggerDelegate.debugLogger,
							"Revision created Successfully  for ONAP" + revision.getRevisionId());
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
					logger.error(EELFLoggerDelegate.errorLogger, "Invalid Request................");
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
				 * == null) { logger.error(EELFLoggerDelegate.errorLogger,
				 * "Token Not Available...!"); throw new
				 * AcumosServiceException(AcumosServiceException.ErrorCode.OBJECT_NOT_FOUND,
				 * "Token Not Available...!"); }
				 */

				String imageUri = null;

				if (ownerId != null && !ownerId.isEmpty()) {

					logger.debug(EELFLoggerDelegate.debugLogger,
							"Dockerization request recieved with " + model.getOriginalFilename());

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
							
							logger.debug(EELFLoggerDelegate.debugLogger, "Setting values in Task object");

							MLPTask task = new MLPTask();
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
							logger.debug(EELFLoggerDelegate.debugLogger, "Task Details: " + task.toString());
							
							task = cdmsClient.createTask(task);

							logger.debug(EELFLoggerDelegate.debugLogger, "TaskID: " + task.getTaskId());

							onboardingStatus.setTaskId(task.getTaskId());

							onboardingStatus.notifyOnboardingStatus("Dockerize", "ST",
									"Create Docker Image Started for solution " + mData.getSolutionId());
						}

						try {
							imageUri = dockerizeFile(metadataParser, modelFile, mlpSolution.getSolutionId(),
									deployment_env, outputFolder);
						} catch (Exception e) {
							// Notify Create docker image failed
							if (onboardingStatus != null) {
								onboardingStatus.notifyOnboardingStatus("Dockerize", "FA", e.getMessage());
							}

							MDC.put(OnboardingLogConstants.MDCs.RESPONSE_STATUS_CODE,
									OnboardingLogConstants.ResponseStatus.ERROR.name());
							MDC.put(OnboardingLogConstants.MDCs.RESPONSE_DESCRIPTION, e.getMessage());
							logger.error(EELFLoggerDelegate.errorLogger, "Error "+ e);
							throw e;
						}

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
							logger.debug(EELFLoggerDelegate.debugLogger, "OutputFolderPath: " + outputFolder);
							logger.debug(EELFLoggerDelegate.debugLogger,
									"AbsolutePath OutputFolderPath: " + outputFolder.getAbsolutePath());
							addDCAEArrtifacts(mData, outputFolder, mlpSolution.getSolutionId(), onboardingStatus);
						}

						isSuccess = true;

						return new ResponseEntity<ServiceResponse>(ServiceResponse.successResponse(mlpSolution),
								HttpStatus.CREATED);
					} finally {

						try {
							UtilityFunction.deleteDirectory(outputFolder);
							if (isSuccess == false) {
								logger.debug(EELFLoggerDelegate.debugLogger,
										"Onboarding Failed, Reverting failed solutions and artifacts.");
								if (metadataParser != null && mData != null) {
									revertbackOnboarding(metadataParser.getMetadata(), mlpSolution.getSolutionId(),
											imageUri);
								}
							}

							// push docker build log into nexus
							File file = new java.io.File(
									logPath + File.separator + trackingID + File.separator + fileName);
							logger.debug(EELFLoggerDelegate.debugLogger, "Log file length " + file.length());
							logger.debug(EELFLoggerDelegate.debugLogger,
									"Log file Path " + file.getPath() + " Absolute Path : " + file.getAbsolutePath()
											+ " Canonical Path: " + file.getCanonicalFile());

							if (metadataParser != null && mData != null) {
								logger.debug(EELFLoggerDelegate.debugLogger,
										"Adding of log artifacts into nexus started " + fileName);

								String nexusArtifactID = "MicroserviceGenerationLog";

								commonOnboarding.addArtifact(mData, file, "LG", nexusArtifactID, onboardingStatus);
								MDC.put(OnboardingLogConstants.MDCs.RESPONSE_STATUS_CODE,
										OnboardingLogConstants.ResponseStatus.COMPLETED.name());
								logger.debug(EELFLoggerDelegate.debugLogger,
										"Artifacts log pushed to nexus successfully" + fileName);
							}

							// delete the Docker image
							/*
							 * logger.debug(EELFLoggerDelegate.
							 * debugLogger,"Docker image Deletion started -> image = "+imageUri+", tag = "
							 * +mData.getVersion()); DeleteImageCommand deleteCMD = new
							 * DeleteImageCommand(imageUri, mData.getVersion(), null); deleteCMD.execute();
							 * logger.debug(EELFLoggerDelegate.debugLogger,"Docker image Deletion Done");
							 */

							// delete log file
							UtilityFunction.deleteDirectory(file);
							logThread.unset();
							mData = null;
						} catch (AcumosServiceException e) {
							mData = null;
							MDC.put(OnboardingLogConstants.MDCs.RESPONSE_STATUS_CODE,
									OnboardingLogConstants.ResponseStatus.ERROR.name());
							MDC.put(OnboardingLogConstants.MDCs.RESPONSE_DESCRIPTION, e.getMessage());
							logger.error(EELFLoggerDelegate.errorLogger, "RevertbackOnboarding Failed");
							HttpStatus httpCode = HttpStatus.INTERNAL_SERVER_ERROR;
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
						logger.error(EELFLoggerDelegate.errorLogger, "Either Username/Password is invalid.");
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
				logger.error(EELFLoggerDelegate.errorLogger, "Model artifacts not available..!");
				throw new AcumosServiceException("Model artifacts not available..!");
			}

		} catch (AcumosServiceException e) {
			
			HttpStatus httpCode = HttpStatus.INTERNAL_SERVER_ERROR;
			MDC.put(OnboardingLogConstants.MDCs.RESPONSE_STATUS_CODE,OnboardingLogConstants.ResponseStatus.ERROR.name());
			MDC.put(OnboardingLogConstants.MDCs.RESPONSE_DESCRIPTION,e.getMessage());
			logger.error(EELFLoggerDelegate.errorLogger, e.getErrorCode() + "  " + e.getMessage());
			if (e.getErrorCode().equalsIgnoreCase(OnboardingConstants.INVALID_PARAMETER)) {
				httpCode = HttpStatus.BAD_REQUEST;
			}
			return new ResponseEntity<ServiceResponse>(ServiceResponse.errorResponse(e.getErrorCode(), e.getMessage()),
					httpCode);
		} catch (HttpClientErrorException e) {
			  MDC.put(OnboardingLogConstants.MDCs.RESPONSE_STATUS_CODE,OnboardingLogConstants.ResponseStatus.ERROR.name());
			  MDC.put(OnboardingLogConstants.MDCs.RESPONSE_DESCRIPTION,e.getMessage());
			// Handling #401
			if (HttpStatus.UNAUTHORIZED == e.getStatusCode() || HttpStatus.BAD_REQUEST == e.getStatusCode()) {
				logger.debug(EELFLoggerDelegate.debugLogger,
						"Unauthorized User - Either Username/Password is invalid.");
				return new ResponseEntity<ServiceResponse>(
						ServiceResponse.errorResponse("" + HttpStatus.UNAUTHORIZED, "Unauthorized User"),
						HttpStatus.UNAUTHORIZED);
			} else {
				logger.error(EELFLoggerDelegate.errorLogger, e.getMessage());
				e.printStackTrace();
				return new ResponseEntity<ServiceResponse>(
						ServiceResponse.errorResponse("" + e.getStatusCode(), e.getMessage()), e.getStatusCode());
			}
		} catch (Exception e) {
			MDC.put(OnboardingLogConstants.MDCs.RESPONSE_STATUS_CODE,OnboardingLogConstants.ResponseStatus.ERROR.name());
			MDC.put(OnboardingLogConstants.MDCs.RESPONSE_DESCRIPTION,e.getMessage());
			logger.error(EELFLoggerDelegate.errorLogger, e.getMessage());
			e.printStackTrace();
			if (e instanceof AcumosServiceException) {
				return new ResponseEntity<ServiceResponse>(
						ServiceResponse.errorResponse(((AcumosServiceException) e).getErrorCode(), e.getMessage()),
						HttpStatus.INTERNAL_SERVER_ERROR);
			} else {
				return new ResponseEntity<ServiceResponse>(
						ServiceResponse.errorResponse(AcumosServiceException.ErrorCode.UNKNOWN.name(), e.getMessage()),
						HttpStatus.INTERNAL_SERVER_ERROR);
			}
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
			
			//call microservice
			logger.debug(EELFLoggerDelegate.debugLogger,"DCAE ADD Artifact Started ");
			commonOnboarding.addArtifact(mData, anoIn, getArtifactTypeCode("Metadata"), "anomaly-in", onboardingStatus);
			commonOnboarding.addArtifact(mData, anoOut, getArtifactTypeCode("Metadata"), "anomaly-out", onboardingStatus);
			commonOnboarding.addArtifact(mData, compo, getArtifactTypeCode("Metadata"), "component", onboardingStatus);
			commonOnboarding.addArtifact(mData, ons, getArtifactTypeCode("Metadata"), "onsdemo1", onboardingStatus);
			logger.debug(EELFLoggerDelegate.debugLogger,"DCAE ADD Artifact End ");
		}

		catch (AcumosServiceException e) {
			logger.error(EELFLoggerDelegate.errorLogger,"Exception occured while adding DCAE Artifacts " +e);
			throw e;
		} catch(Exception e){
			logger.error(EELFLoggerDelegate.errorLogger,"Exception occured while adding DCAE Artifacts " +e);
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
