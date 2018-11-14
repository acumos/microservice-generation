package org.acumos.microservice;

import org.acumos.cds.client.CommonDataServiceRestClientImpl;
import org.acumos.cds.domain.MLPArtifact;
import org.acumos.microservice.component.docker.DockerClientFactory;
import org.acumos.microservice.component.docker.DockerConfiguration;
import org.acumos.microservice.component.docker.DockerizeModel;
import org.acumos.microservice.component.docker.cmd.CreateImageCommand;
import org.acumos.microservice.component.docker.cmd.DeleteImageCommand;
import org.acumos.microservice.component.docker.cmd.PushImageCommand;
import org.acumos.microservice.component.docker.cmd.TagImageCommand;
import org.acumos.microservice.component.docker.preparation.H2ODockerPreparator;
import org.acumos.microservice.component.docker.preparation.JavaGenericDockerPreparator;
import org.acumos.microservice.component.docker.preparation.PythonDockerPreprator;
import org.acumos.microservice.component.docker.preparation.RDockerPreparator;
import org.acumos.microservice.services.impl.DownloadModelArtifacts;
import org.acumos.nexus.client.NexusArtifactClient;
import org.acumos.nexus.client.RepositoryLocation;
//import org.acumos.onboarding.FilePathTest;
import org.acumos.onboarding.common.exception.AcumosServiceException;
import org.acumos.onboarding.common.models.OnboardingNotification;
import org.acumos.onboarding.common.utils.LogBean;
import org.acumos.onboarding.common.utils.LogThreadLocal;
import org.acumos.onboarding.common.utils.ResourceUtils;
import org.acumos.onboarding.common.utils.UtilityFunction;
import org.acumos.onboarding.component.docker.preparation.Metadata;
import org.acumos.onboarding.component.docker.preparation.MetadataParser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.github.dockerjava.api.DockerClient;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
//import static org.powermock.api.easymock.PowerMock.createMock;
import org.acumos.onboarding.common.utils.UtilityFunction;
//import static org.easmock


@RunWith(PowerMockRunner.class)
@PrepareForTest({DockerizeModel.class,UtilityFunction.class,DockerClientFactory.class})
public class DockerizeModelTest  implements ResourceLoaderAware {
	
	@InjectMocks
	@Spy  //Spy is added to support static mock 
	DockerizeModel dockerizeModel = new DockerizeModel();
	
	ResourceLoader resourceLoader;
	
	@Mock
	ResourceUtils resourceUtils;

	@Mock
	CommonDataServiceRestClientImpl cmnDataService;
	
	@Mock
	OnboardingNotification onboardingNotification;
	
	@Mock
	NexusArtifactClient artifactClient;
	
	@Mock
	UtilityFunction utilityFunction;
	
	@Mock
	PythonDockerPreprator pythonDockerPreprator;
	
	@Mock
	RDockerPreparator rDockerPreparator;
	
	@Mock
	JavaGenericDockerPreparator javaGenericDockerPreparator;
	
	@Mock
	H2ODockerPreparator h2oDockerPreparator;
	
	@Mock
	DockerConfiguration dockerConfiguration;
	
	@Mock
	DockerClient dockerClient;
	
	@Mock
	CreateImageCommand createImageCommand;
	
	@Mock
	TagImageCommand tagImageCommand;
	
	@Mock
	PushImageCommand pushImageCmd;
	
	@Mock
	DeleteImageCommand deleteImageCommand;
	


	@Test
	public void dockerizePythonFileTest() {
		
	try {

		File files = new File("model");
		File MetaFile = new File(files, "metadata.json");
		String ownerId = "testuser";
		
		String filePath = FilePathTest.filePath();
		File validJsonFile = new File(filePath + "metadata.json");
		
		ResourceUtils resourceUtilsMock = mock(ResourceUtils.class);
		mockStatic(UtilityFunction.class);
		mockStatic(DockerClientFactory.class);
		Resource rc = mock(Resource.class);
		File file = mock(File.class);
		PythonDockerPreprator pythonDockerPreprator = mock(PythonDockerPreprator.class);
		//CreateImageCommand createImageCommand = mock(CreateImageCommand.class);
		
		DockerizeModel dockerizeModelMock = mock(DockerizeModel.class);
		DockerClient dockerClient = mock(DockerClient.class);
		
		MetadataParser metadataParser = new MetadataParser(validJsonFile);
		
		Metadata mData = null;
		
		mData = metadataParser.getMetadata();
		mData.setModelName("acumosmodel");
		mData.setVersion("1.0");
		
		
		String modelId = "12345";
		File outputFolder = new File(filePath,modelId);
		
		File modelFile = new File("model.zip");
		
		ResourceUtils resourceUtils1 = new ResourceUtils(resourceLoader);
		Resource[] resources = resourceUtils1.loadResources("classpath*:templates/dcae_python/*");
		
		PowerMockito.when(resourceUtils.loadResources(Mockito.anyString())).thenReturn(resources);
		
		PowerMockito.doNothing().when(UtilityFunction.class);
		UtilityFunction.copyFile(rc, file);
		
		PowerMockito.doReturn(dockerClient).when(DockerClientFactory.class);
		DockerClientFactory.getDockerClient(Mockito.anyObject());
		
		PowerMockito.whenNew(PythonDockerPreprator.class)
			.withArguments(Mockito.anyObject(), Mockito.anyString(), Mockito.anyString(),Mockito.anyString()).thenReturn(pythonDockerPreprator);
	
		doNothing().when(pythonDockerPreprator).prepareDockerAppV2(Mockito.anyObject());
		
		doNothing().when(dockerizeModel).listFilesAndFilesSubDirectories(Mockito.anyObject());
		PowerMockito.whenNew(CreateImageCommand.class).withArguments(Mockito.anyObject(), Mockito.anyString(), Mockito.anyString(),Mockito.anyString(),Mockito.anyBoolean(),Mockito.anyBoolean()).thenReturn(createImageCommand);
		doNothing().when(createImageCommand).execute();
		
		PowerMockito.whenNew(CreateImageCommand.class).withArguments(Mockito.anyObject(), Mockito.anyString(), Mockito.anyString(),Mockito.anyString(),Mockito.anyBoolean(),Mockito.anyBoolean()).thenReturn(createImageCommand);
		doNothing().when(createImageCommand).execute();
		

		PowerMockito.whenNew(TagImageCommand.class).withArguments(Mockito.anyString(), Mockito.anyString(),Mockito.anyString(),Mockito.anyBoolean(),Mockito.anyBoolean()).thenReturn(tagImageCommand);

		PowerMockito.whenNew(PushImageCommand.class).withArguments(Mockito.anyString(), Mockito.anyString(),Mockito.anyString()).thenReturn(pushImageCmd);
	
		
		String imageURI = dockerizeModel.dockerizeFile(metadataParser, modelFile, "solid1234", "2",outputFolder);
		assertNotNull(imageURI);	
		
		
	
	
		} catch (AcumosServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	
	@Test
	public void dockerizeJavagenericArgusH2ORFileTest() {
		
	try {

		File files = new File("model");
		File MetaFile = new File(files, "metadata.json");
		String ownerId = "testuser";
		
		String filePath = FilePathTest.filePath();
		File validJsonFile = new File(filePath + "metadata.json");
		
		ResourceUtils resourceUtilsMock = mock(ResourceUtils.class);
		mockStatic(UtilityFunction.class);
		mockStatic(DockerClientFactory.class);
		Resource rc = mock(Resource.class);
		File file = mock(File.class);
		JavaGenericDockerPreparator javaGenericDockerPreparator = mock(JavaGenericDockerPreparator.class);
		
		RDockerPreparator rDockerPreparator = mock(RDockerPreparator.class);
		//CreateImageCommand createImageCommand = mock(CreateImageCommand.class);
		H2ODockerPreparator h2ODockerPreparator = mock(H2ODockerPreparator.class);
		
		
		DockerizeModel dockerizeModelMock = mock(DockerizeModel.class);
		DockerClient dockerClient = mock(DockerClient.class);
		
		MetadataParser metadataParser = new MetadataParser(validJsonFile);
		
		Metadata mData = null;
		
		mData = metadataParser.getMetadata();
		mData.setModelName("acumosmodel");
		mData.setVersion("1.0");
		
		
		String modelId = "12345";
		File outputFolder = new File(filePath,modelId);
		
		File modelFile = new File("model.zip");
		
		ResourceUtils resourceUtils1 = new ResourceUtils(resourceLoader);
		Resource[] resources = resourceUtils1.loadResources("classpath*:templates/java_argus/*");
		
		PowerMockito.when(resourceUtils.loadResources(Mockito.anyString())).thenReturn(resources);
		
		PowerMockito.doNothing().when(UtilityFunction.class);
		UtilityFunction.copyFile(rc, file);
		
		PowerMockito.doReturn(dockerClient).when(DockerClientFactory.class);
		DockerClientFactory.getDockerClient(Mockito.anyObject());
		
		PowerMockito.whenNew(JavaGenericDockerPreparator.class)
			.withArguments(Mockito.anyObject()).thenReturn(javaGenericDockerPreparator);
	
		PowerMockito.whenNew(RDockerPreparator.class)
		.withArguments(Mockito.anyObject() , Mockito.anyString()).thenReturn(rDockerPreparator);
		
		PowerMockito.whenNew(H2ODockerPreparator.class)
		.withArguments(Mockito.anyObject()).thenReturn(h2ODockerPreparator);

		
		doNothing().when(javaGenericDockerPreparator).prepareDockerApp(Mockito.anyObject());
		doNothing().when(rDockerPreparator).prepareDockerApp(Mockito.anyObject());
		doNothing().when(h2ODockerPreparator).prepareDockerApp(Mockito.anyObject());
		
		
		doNothing().when(dockerizeModel).listFilesAndFilesSubDirectories(Mockito.anyObject());
		PowerMockito.whenNew(CreateImageCommand.class).withArguments(Mockito.anyObject(), Mockito.anyString(), Mockito.anyString(),Mockito.anyString(),Mockito.anyBoolean(),Mockito.anyBoolean()).thenReturn(createImageCommand);
		doNothing().when(createImageCommand).execute();
		
		PowerMockito.whenNew(CreateImageCommand.class).withArguments(Mockito.anyObject(), Mockito.anyString(), Mockito.anyString(),Mockito.anyString(),Mockito.anyBoolean(),Mockito.anyBoolean()).thenReturn(createImageCommand);
		doNothing().when(createImageCommand).execute();
		

		PowerMockito.whenNew(TagImageCommand.class).withArguments(Mockito.anyString(), Mockito.anyString(),Mockito.anyString(),Mockito.anyBoolean(),Mockito.anyBoolean()).thenReturn(tagImageCommand);

		PowerMockito.whenNew(PushImageCommand.class).withArguments(Mockito.anyString(), Mockito.anyString(),Mockito.anyString()).thenReturn(pushImageCmd);
		
		
		PowerMockito.when(UtilityFunction.getFileName(Mockito.anyObject(),Mockito.anyString())).thenReturn("testfile");
		UtilityFunction.deCompressGZipFile(Mockito.anyObject(),Mockito.anyObject());
		UtilityFunction.unTarFile(Mockito.anyObject(),Mockito.anyObject());
		
		mData.setRuntimeName("javageneric");
		mData.setRuntimeVersion("0");
		dockerizeModel.setModelOriginalName("acumosavageneric");
		String imageURI = dockerizeModel.dockerizeFile(metadataParser, modelFile, "solid1234", "2",outputFolder);
		assertNotNull(imageURI);	
		
		mData.setRuntimeName("r");
		dockerizeModel.setModelOriginalName("acumosr");
		imageURI = dockerizeModel.dockerizeFile(metadataParser, modelFile, "solid1234", "2",outputFolder);
		assertNotNull(imageURI);	
		
		mData.setRuntimeName("h2o");
		dockerizeModel.setModelOriginalName("h2o");
		imageURI = dockerizeModel.dockerizeFile(metadataParser, modelFile, "solid1234", "2",outputFolder);
		assertNotNull(imageURI);	
		
		mData.setRuntimeName("javaargus");
		dockerizeModel.setModelOriginalName("javaargus");
		imageURI = dockerizeModel.dockerizeFile(metadataParser, modelFile, "solid1234", "2",outputFolder);
		assertNotNull(imageURI);
		
		
		
	
	
		} catch (AcumosServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(Exception e) {
			e.printStackTrace();
		}
	
	}
	
	
	@Test
	public void revertbackOnboardingTest() {
		
	try {

		
		File files = new File("model");
		File MetaFile = new File(files, "metadata.json");
		String ownerId = "testuser";
		
		String filePath = FilePathTest.filePath();
		String modelId = "12345";
		File outputFolder = new File(filePath,modelId);
		
		File validJsonFile = new File(filePath + "metadata.json");
		
		mockStatic(DockerClientFactory.class);
		CommonDataServiceRestClientImpl cmdDataSvc = mock(CommonDataServiceRestClientImpl.class);
		NexusArtifactClient artifactClient = mock(NexusArtifactClient.class);
		
		DockerClient dockerClient = mock(DockerClient.class);
		
		MetadataParser metadataParser = new MetadataParser(validJsonFile);
		
		Metadata mData = null;
		
		mData = metadataParser.getMetadata();
		mData.setModelName("acumosmodel");
		mData.setVersion("1.0");
		
		PowerMockito.doReturn(dockerClient).when(DockerClientFactory.class);
		DockerClientFactory.getDockerClient(Mockito.anyObject());
		
		PowerMockito.whenNew(NexusArtifactClient.class).withArguments(Mockito.anyObject()).thenReturn(artifactClient);
		
		MLPArtifact mLPArtifact  = new MLPArtifact();
		mLPArtifact.setArtifactTypeCode("MI");
		mLPArtifact.setUri("org/acumos/hello-world/2/hello-world-2.json");
		
		List<MLPArtifact> mlpArtifactList = new ArrayList();
		mlpArtifactList.add(mLPArtifact);		
		
		// Remember-  to mock method on mock object it should on mock() method not on object created by @Mock  . e.g.  below won't work 
		// on cmnDataService created @Mock where it should work on cmdDataSvc created by mock(....)
		PowerMockito.when(cmdDataSvc.getSolutionRevisionArtifacts(Mockito.anyString(), Mockito.anyString())).thenReturn(mlpArtifactList);
		
		//DeleteImageCommand deleteImageCommand = new DeleteImageCommand(imageTagName, metadata.getVersion(), "");
		PowerMockito.whenNew(DeleteImageCommand.class).withArguments(Mockito.anyString(), Mockito.anyString(),Mockito.anyString()).thenReturn(deleteImageCommand);
		
		mData.setRuntimeName("javageneric");
		mData.setRuntimeVersion("0");
		mData.setSolutionId("12dgcb63f45sd");
		dockerizeModel.setModelOriginalName("acumosavageneric");
		
	/*	LogBean logBean = new LogBean();
		logBean.setFileName("fileName");
		//logBean.setLogPath(logPath+File.separator+trackingID);

		LogThreadLocal logThread = new LogThreadLocal();
		logThread.set(logBean);*/
		
		//dockerizeModel.createLogFile("test");
//		dockerizeModel.listFilesAndFilesSubDirectories(outputFolder);
		dockerizeModel.revertbackOnboarding(mData, "solutionid", "imagclassifier");
		
		
		
		assert(true);	
		
		} catch (AcumosServiceException e) {
			assert(false);
			e.printStackTrace();
		} catch(Exception e) {
			e.printStackTrace();
		}
	
	
	}



	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		// TODO Auto-generated method stub
		 this.resourceLoader = resourceLoader;
	}
	
	
 		
	}
	


