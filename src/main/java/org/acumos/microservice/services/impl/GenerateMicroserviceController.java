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
import org.acumos.cds.domain.MLPUser;
import org.acumos.microservice.component.docker.DockerizeModel;
import org.acumos.microservice.services.DockerService;
import org.acumos.onboarding.common.exception.AcumosServiceException;
import org.acumos.onboarding.common.models.OnboardingNotification;
import org.acumos.onboarding.common.models.ServiceResponse;
import org.acumos.onboarding.common.utils.EELFLoggerDelegate;
import org.acumos.onboarding.common.utils.JsonResponse;
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
			@ApiResponse(code = 400, message = "Invalid request", response = ServiceResponse.class) })
	@RequestMapping(value = "/generateMicroservice", method = RequestMethod.POST, produces = "application/json")
	public ResponseEntity<ServiceResponse> generateMicroservice(HttpServletRequest request,
			@RequestParam(required = true) String solutioId, String revisionId,
			@RequestParam(required = false) String modName,
			@RequestParam(required = false) Integer deployment_env,
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestHeader(value = "tracking_id", required = false) String trackingID,
			@RequestHeader(value = "provider", required = false) String provider)
			throws AcumosServiceException {
		

		String artifactName = null;
		List<String> artifactNameList = new ArrayList<String>();
		File files = null;
		Metadata mData = null;
		OnboardingNotification onboardingStatus = null;
		MLPSolution mlpSolution = new MLPSolution();
		
		if (deployment_env == null){
			deployment_env = 1;
		}
		
		// If trackingID is provided in the header create a
		// OnboardingNotification object that will be used to update
		// status
		// against that trackingID
		onboardingStatus = new OnboardingNotification(cmnDataSvcEndPoinURL, cmnDataSvcUser, cmnDataSvcPwd);
		if (trackingID != null) {
			logger.debug(EELFLoggerDelegate.debugLogger, "Tracking ID: {}", trackingID);
			onboardingStatus.setTrackingId(trackingID);
		} else {
			trackingID = UUID.randomUUID().toString();
			onboardingStatus.setTrackingId(trackingID);
			logger.debug(EELFLoggerDelegate.debugLogger, "Tracking ID: {}", trackingID);
		}	
		
		String fileName = "MSGen_"+ trackingID + ".log";
		// setting log filename in ThreadLocal
		LogBean logBean = new LogBean();
		logBean.setFileName(fileName);
		logBean.setLogPath(logPath);

		LogThreadLocal logThread = new LogThreadLocal();
		logThread.set(logBean);
		// create log file to capture logs as artifact
		createLogFile(logPath);

		logger.debug(EELFLoggerDelegate.debugLogger, "Fetching model from Nexus...!");
		
		try {

			// Nexus Integration....!

			DownloadModelArtifacts download = new DownloadModelArtifacts();
			logger.debug(EELFLoggerDelegate.debugLogger, "solutioId: {}", solutioId, "revisionId: {}", revisionId);
			artifactNameList = download.getModelArtifacts(solutioId, revisionId, cmnDataSvcUser, cmnDataSvcPwd,
					nexusEndPointURL, nexusUserName, nexusPassword, cmnDataSvcEndPoinURL);
			
			logger.debug(EELFLoggerDelegate.debugLogger, "Number of artifacts: ", artifactNameList.size());
			
			logger.debug(EELFLoggerDelegate.debugLogger,"Starting Microservice Generation");

			files = new File("model");

			MultipartFile model = null, meta = null, proto = null;
			
			File modelFile = null, MetaFile = null, protoFile = null;
			
			for (String name : artifactNameList) {
				if (name.contains(".json")) {
					logger.debug(EELFLoggerDelegate.debugLogger, "MetaFile: {}", name);
					MetaFile = new File(files, name);
				} else if (name.contains(".proto")) {
					logger.debug(EELFLoggerDelegate.debugLogger, "ProtoFile: {}", name);
					protoFile = new File(files, name);
				} else {
					logger.debug(EELFLoggerDelegate.debugLogger, "ModelFile: {}", name);
					modelFile = new File(files, name);
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
				
				FileInputStream fisModel = new FileInputStream(modelFile);
				model = new MockMultipartFile("Model", modelFile.getName(), "", fisModel);

				FileInputStream fisMeta = new FileInputStream(MetaFile);
				meta = new MockMultipartFile("Metadata", MetaFile.getName(), "", fisMeta);

				FileInputStream fisProto = new FileInputStream(protoFile);
				proto = new MockMultipartFile("Proto", protoFile.getName(), "", fisProto);
				
				if(solutioId != null && revisionId != null){
					mData.setSolutionId(solutioId);
					mData.setRevisionId(revisionId);
					mlpSolution.setSolutionId(solutioId);
					mlpSolution.setName(mData.getSolutionName());
					mlpSolution.setDescription(mData.getSolutionName());
					mlpSolution.setUserId(mData.getOwnerId());
				}
				
				String version = mData.getVersion();

				if (version == null) {
					version = getModelVersion(mData.getSolutionId(), mData.getRevisionId());
					mData.setVersion(version);
				}

				MLPUser shareUser = null;

				// try {
				// 'authorization' represents JWT token here...!
				if (authorization == null) {
					logger.error(EELFLoggerDelegate.errorLogger, "Token Not Available...!");
					throw new AcumosServiceException(AcumosServiceException.ErrorCode.OBJECT_NOT_FOUND,
							"Token Not Available...!");
				}

				// Call to validate JWT Token.....!
				logger.debug(EELFLoggerDelegate.debugLogger, "Started JWT token validation");
				String ownerId = commonOnboarding.validate(authorization, provider);

				String imageUri = null;

				if (ownerId != null && !ownerId.isEmpty()) {
					logger.debug(EELFLoggerDelegate.debugLogger, "Token validation successful");
				
					// update userId in onboardingStatus
					if (onboardingStatus != null)
						onboardingStatus.setUserId(ownerId);

					logger.debug(EELFLoggerDelegate.debugLogger,
							"Dockerization request recieved with " + model.getOriginalFilename());

					modelOriginalName = model.getOriginalFilename();
					String modelId = UtilityFunction.getGUID();
					File outputFolder = new File("tmp", modelId);
					outputFolder.mkdirs();
					boolean isSuccess = false;
					
					try {
						mData.setOwnerId(ownerId);

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
							onboardingStatus.notifyOnboardingStatus("Dockerize", "ST",
									"Create Docker Image Started for solution " + mData.getSolutionId());
						}

						try {
							imageUri = dockerizeFile(metadataParser, modelFile, mlpSolution.getSolutionId(), deployment_env);
						} catch (Exception e) {
							// Notify Create docker image failed
							if (onboardingStatus != null) {
								onboardingStatus.notifyOnboardingStatus("Dockerize", "FA", e.getMessage());
							}
							
							MDC.put(OnboardingLogConstants.MDCs.RESPONSE_STATUS_CODE,OnboardingLogConstants.ResponseStatus.ERROR.name());
							MDC.put(OnboardingLogConstants.MDCs.RESPONSE_DESCRIPTION,e.getMessage());
							logger.error(EELFLoggerDelegate.errorLogger, "Error {}", e);
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
						commonOnboarding.addArtifact(mData, imageUri, getArtifactTypeCode("Docker Image"), onboardingStatus);
						
						if (deployment_env == 2) {
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
									revertbackOnboarding(metadataParser.getMetadata(), mlpSolution.getSolutionId(), imageUri);
								}
							}

							// push docker build log into nexus
							File file = new java.io.File(logPath + File.separator + fileName);
							logger.debug(EELFLoggerDelegate.debugLogger, "Log file length " + file.length());
							logger.debug(EELFLoggerDelegate.debugLogger, "Log file Path " + file.getPath() +" Absolute Path : "+file.getAbsolutePath() + " Canonical Path: "+ file.getCanonicalFile());

							if (metadataParser != null && mData != null) {
								logger.debug(EELFLoggerDelegate.debugLogger,
										"Adding of log artifacts into nexus started " + fileName);

								commonOnboarding.addArtifact(mData, file, "LG",
										fileName, onboardingStatus);
								MDC.put(OnboardingLogConstants.MDCs.RESPONSE_STATUS_CODE,OnboardingLogConstants.ResponseStatus.COMPLETED.name());
								logger.debug(EELFLoggerDelegate.debugLogger,
										"Artifacts log pushed to nexus successfully" + fileName);
							}

							// delete log file
							UtilityFunction.deleteDirectory(file);
							logThread.unset();
							mData = null;
						} catch (AcumosServiceException e) {
							mData = null;
							MDC.put(OnboardingLogConstants.MDCs.RESPONSE_STATUS_CODE,OnboardingLogConstants.ResponseStatus.ERROR.name());
							MDC.put(OnboardingLogConstants.MDCs.RESPONSE_DESCRIPTION,e.getMessage());
							logger.error(EELFLoggerDelegate.errorLogger, "RevertbackOnboarding Failed");
							HttpStatus httpCode = HttpStatus.INTERNAL_SERVER_ERROR;
							return new ResponseEntity<ServiceResponse>(
									ServiceResponse.errorResponse(e.getErrorCode(), e.getMessage()), httpCode);
						}
					}
				} else {
					try {
						MDC.put(OnboardingLogConstants.MDCs.RESPONSE_STATUS_CODE,OnboardingLogConstants.ResponseStatus.ERROR.name());
						MDC.put(OnboardingLogConstants.MDCs.RESPONSE_DESCRIPTION,"Either Username/Password is invalid");
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
			if (HttpStatus.UNAUTHORIZED == e.getStatusCode()) {
				logger.debug(EELFLoggerDelegate.debugLogger,
						"Unauthorized User - Either Username/Password is invalid.");
				return new ResponseEntity<ServiceResponse>(
						ServiceResponse.errorResponse("" + e.getStatusCode(), "Unauthorized User"),
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
			commonOnboarding.addArtifact(mData, anoIn, getArtifactTypeCode("Metadata"), solutionID, onboardingStatus);
			commonOnboarding.addArtifact(mData, anoOut, getArtifactTypeCode("Metadata"), solutionID, onboardingStatus);
			commonOnboarding.addArtifact(mData, compo, getArtifactTypeCode("Metadata"), solutionID, onboardingStatus);
			commonOnboarding.addArtifact(mData, ons, getArtifactTypeCode("Metadata"), solutionID, onboardingStatus);
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
