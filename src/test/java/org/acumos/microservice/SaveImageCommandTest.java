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

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.acumos.microservice.component.docker.DockerClientFactory;
import org.acumos.microservice.component.docker.DockerizeModel;
import org.acumos.microservice.component.docker.cmd.SaveImageCommand;
import org.acumos.microservice.component.docker.preparation.JavaGenericDockerPreparator;
import org.acumos.microservice.component.docker.preparation.RDockerPreparator;
import org.acumos.onboarding.common.utils.UtilityFunction;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.github.dockerjava.api.DockerClient;

/**
 * 
 * @author ****
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SaveImageCommand.class,IOUtils.class})
//@PrepareForTest({SaveImageCommand.class,IOUtils.class})
public class SaveImageCommandTest {
    
	String filePath = FilePathTest.filePath();
	
	@InjectMocks
	SaveImageCommand saveImageCommand = new SaveImageCommand("H2O", "1.0.0", filePath + "metadata.json", "h20", true);
	
	@Mock
	IOUtils iOUtils;

	@Test
	public void getDisplayName() {
		try {
			Assert.assertNotNull(saveImageCommand.getDisplayName());
		} catch (Exception e) {
			Assert.fail("getDisplayName failed : " + e.getMessage());
		}
	}
	
	@Test
	public void executeTest() {
		
		//final OutputStream output = new FileOutputStream(new File(destination, filename));
		FileOutputStream fileOutputStream = mock(FileOutputStream.class);
		OutputStream outputStream = mock(OutputStream.class);
		InputStream inputStream = mock(InputStream.class);
		SaveImageCommand SaveImageCommandMock = mock(SaveImageCommand.class);
		
		File file = new File("file");
		

		
		try {
			PowerMockito.whenNew(FileOutputStream.class).withArguments(file).thenReturn(fileOutputStream);
			
			//PowerMockito.doNothing().when(IOUtils.class);
			mockStatic(IOUtils.class);
			//.getClass()expect(IOUtils.copy(inputStream,outputStream)).andReturn(1);
			//when(IOUtils.copy(inputStream,outputStream)).thenReturn(1);
			//PowerMockito.when(IOUtils.class).thenReturn(1);
			//when(IOUtils.copy(inputStream,outputStream))
			mockStatic(IOUtils.class);
			PowerMockito.when(IOUtils.copy(inputStream,outputStream)).thenReturn(1);
			
			PowerMockito.doNothing().when(IOUtils.class);
			//doNothing().when(IOUtils.closeQuietly(outputStream));
			IOUtils.closeQuietly(outputStream);
			
			//doNothing().when(iOUtils).copy(inputStream,outputStream);
			//UtilityFunction.copyFile(rc, file);
			//IOUtils.copy(client.saveImageCmd(imageName + ":" + imageTag).exec(), output);
			DockerClient dockerClient = mock(DockerClient.class);
			
			
			PowerMockito.when(SaveImageCommandMock.getClient()).thenReturn(dockerClient);
			//DockerClient client
			
			
			saveImageCommand.execute();

		} catch (Exception e) {
			e.printStackTrace();
		}

		
	
		
	}
	
	@Test
	public void getImageName(){
		saveImageCommand.getImageName();
	}
	@Test
	public void getImageTag(){
		saveImageCommand.getImageTag();
	}
	@Test
	public void getDestination(){
		saveImageCommand.getDestination();
	}
	@Test
	public void getFilename(){
		saveImageCommand.getFilename();
	}
	@Test
	public void getIgnoreIfNotFound(){
		saveImageCommand.getIgnoreIfNotFound();
	}
	
	
}
