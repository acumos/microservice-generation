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

package org.acumos.microservice;

import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.acumos.microservice.component.docker.preparation.CPPDockerPreparator;
import org.acumos.onboarding.common.exception.AcumosServiceException;
import org.acumos.onboarding.component.docker.preparation.MetadataParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CPPDockerPreparatorTest {

	 
	String filePath = FilePathTest.filePath(); 
	File outFolder = new File(filePath);
	File jsonFile = new File(filePath+"cpp.json");
	File reqtxt = new File(filePath+"requirements.txt");
	File srcFile = new File(filePath+"Dockerfile");
	
	public CPPDockerPreparatorTest() throws AcumosServiceException {
		new MetadataParser(jsonFile);
	}
	
	MetadataParser metadataParser = new MetadataParser(jsonFile);
	
	@InjectMocks
	CPPDockerPreparator cPPDockerPreparator = new CPPDockerPreparator(metadataParser, "http_proxy");

	@Test
	public void compareVersionTest() {

		int[] baseVersion = { 1, 2, 3 };
		int[] currentVersion = { 4, 5, 6 };
		int result = CPPDockerPreparator.compareVersion(baseVersion, currentVersion);
		assertNotNull(result);
	}

	@Test
	public void versionAsArrayTest() {

		int[] baseVersion = CPPDockerPreparator.versionAsArray("1234");
		assertNotNull(baseVersion);
	}
	
	@Test
	public void prepareDockerAppTest() throws AcumosServiceException {
		
	try {	
		cPPDockerPreparator.prepareDockerApp(outFolder);
	} catch(Exception e) {
		org.junit.Assert.fail("prepareDockerApp failed : " + e.getMessage());
	}

	}
	
	
		
}