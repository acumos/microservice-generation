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

package org.acumos.microservice.component.docker.preparation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.acumos.onboarding.common.exception.AcumosServiceException;
import org.acumos.onboarding.common.utils.UtilityFunction;
import org.acumos.onboarding.component.docker.preparation.Metadata;
import org.acumos.onboarding.component.docker.preparation.MetadataParser;
import org.acumos.onboarding.component.docker.preparation.Requirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.acumos.onboarding.common.utils.LogBean;
import org.acumos.onboarding.common.utils.LoggerDelegate;

public class RDockerPreparator {
	
	private static Logger log = LoggerFactory.getLogger(RDockerPreparator.class);
	static LoggerDelegate logger = new LoggerDelegate(log);

	
	private Metadata metadata;

	private String rVersion;

	private String rhttpProxy;
	
	private LogBean logBean;
	
	private String rimageName;

	
	
	public RDockerPreparator(MetadataParser metadataParser, String httpProxy, LogBean logBean, String rimageName) throws AcumosServiceException {
		this.rhttpProxy = httpProxy;
		this.metadata = metadataParser.getMetadata();
		this.rimageName = rimageName;
		int[] runtimeVersion = versionAsArray(metadata.getRuntimeVersion());
		this.logBean = logBean;

		if (runtimeVersion.length > 0) {
			String version = Arrays.toString(runtimeVersion);
			log.debug("Version Log Debug: " + version);
			version = version.replaceAll(", ", ".").replace("[", "").replace("]", "");
			this.rVersion = version;
			log.debug("Version Log Debug: " + version);
		} else {
			throw new AcumosServiceException(AcumosServiceException.ErrorCode.INVALID_PARAMETER,
					"Unspported r version " + metadata.getRuntimeVersion());
		}

	}

	public void prepareDockerApp(File outputFolder) throws AcumosServiceException {
		this.createDockerFile(new File(outputFolder, "Dockerfile"), new File(outputFolder, "Dockerfile"));
		this.createPackageR(new File(outputFolder, "packages.R"), new File(outputFolder, "packages.R"));
	}

	private void createPackageR(File inPackageRFile, File outPackageRFile) throws AcumosServiceException {
		try {
			List<Requirement> requirements = this.metadata.getRequirements();
			List<String> reqAsLists = new ArrayList();
			
			for (Requirement requirement : requirements) {
				reqAsLists.add(requirement.name);
			}
			
			logger.debug("metadatajson R package: " + reqAsLists,logBean);
						
			String packageRFileAsString= new String(UtilityFunction.toBytes(inPackageRFile));
			
			logger.debug("requriment Package.R package: " + packageRFileAsString,logBean);
			
			if(reqAsLists != null && !reqAsLists.isEmpty()) {
				String packageAsString = "";
				for(String reqAsList : reqAsLists) {
					packageAsString = "install.packages(\""+reqAsList+"\",repos=\"http://cloud.r-project.org\",dependencies=T)";
				if(!packageRFileAsString.contains(packageAsString))
					packageRFileAsString += "\n"+packageAsString;
				}
			}
			
			logger.debug("Merged R packages: " + packageRFileAsString, logBean);
						
			FileWriter writer = new FileWriter(outPackageRFile);
			try {
				writer.write(packageRFileAsString.trim());
			} finally {
				writer.close();
			}
		} catch (IOException e) {
			throw new AcumosServiceException(AcumosServiceException.ErrorCode.INTERNAL_SERVER_ERROR,
					"Fail to create dockerFile for input model", e);
		}
	}

	private void createDockerFile(File inDockerFile, File outDockerFile) throws AcumosServiceException {
		try {
			String dockerFileAsString = new String(UtilityFunction.toBytes(inDockerFile));
			dockerFileAsString = MessageFormat.format(dockerFileAsString,
					new Object[] { this.rhttpProxy, this.rVersion, this.rimageName });
			FileWriter writer = new FileWriter(outDockerFile);
			try {
				writer.write(dockerFileAsString.trim());
			} finally {
				writer.close();
			}
		} catch (IOException e) {
			throw new AcumosServiceException(AcumosServiceException.ErrorCode.INTERNAL_SERVER_ERROR,
					"Fail to create dockerFile for input model", e);
		}
	}

	public static int compareVersion(int[] baseVersion, int[] currentVersion) {
		int result = 0;
		for (int i = 0; i < baseVersion.length; i++) {
			if (currentVersion.length < i + 1 || baseVersion[i] > currentVersion[i]) {
				result = 1;
				break;
			} else if (baseVersion[i] < currentVersion[i]) {
				result = -1;
				break;
			}
		}
		return result;
	}

	public static int[] versionAsArray(String version) {
		String[] versionArr = version.split("\\.");
		int[] versionIntArr = new int[versionArr.length];
		for (int i = 0; i < versionArr.length; i++) {
			versionIntArr[i] = Integer.parseInt(versionArr[i]);
		}
		return versionIntArr;
	}

}
