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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;

import org.acumos.microservice.component.docker.DockerizeModel;
import org.acumos.microservice.services.DockerService;
import org.acumos.onboarding.common.exception.AcumosServiceException;
import org.acumos.onboarding.common.models.OnboardingNotification;
import org.acumos.onboarding.common.models.ServiceResponse;
import org.acumos.onboarding.common.utils.EELFLoggerDelegate;
import org.acumos.onboarding.logging.OnboardingLogConstants;
import org.acumos.onboarding.services.impl.CommonOnboarding;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
		OnboardingNotification onboardingStatus = null;
		
		if (deploy_env == null || deploy_env.isEmpty()){
			deployment_env = "1";
		} else {
			deployment_env = deploy_env.trim();
		}
		
		logger.debug(EELFLoggerDelegate.debugLogger, "deployment_env: {}", deployment_env);
		
		if (request_id != null) {
			logger.debug(EELFLoggerDelegate.debugLogger, "Request ID: {}", request_id);
		} else {
			request_id = UUID.randomUUID().toString();
			logger.debug(EELFLoggerDelegate.debugLogger, "Request ID Created: {}", request_id);
		}
		
		// If trackingID is provided in the header create a
		// OnboardingNotification object that will be used to update
		// status
		// against that trackingID
		if (trackingID != null) {
			logger.debug(EELFLoggerDelegate.debugLogger, "Tracking ID: {}", trackingID);
		} else {
			trackingID = UUID.randomUUID().toString();
			logger.debug(EELFLoggerDelegate.debugLogger, "Tracking ID: {}", trackingID);
		}	
		
		onboardingStatus = new OnboardingNotification(cmnDataSvcEndPoinURL, cmnDataSvcUser, cmnDataSvcPwd, request_id);
		onboardingStatus.setRequestId(request_id);
		MDC.put(OnboardingLogConstants.MDCs.REQUEST_ID, request_id);
		
		return generateMicroserviceDef(onboardingStatus, solutioId, revisionId, modName, deployment_env, authorization,
				trackingID, provider);

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
