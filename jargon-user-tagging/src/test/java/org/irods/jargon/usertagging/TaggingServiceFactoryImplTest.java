package org.irods.jargon.usertagging;

import java.util.Properties;

import junit.framework.TestCase;

import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.testutils.TestingPropertiesHelper;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;


public class TaggingServiceFactoryImplTest {
	
	private static Properties testingProperties = new Properties();
	private static TestingPropertiesHelper testingPropertiesHelper = new TestingPropertiesHelper();
	private static IRODSAccount irodsAccount;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		TestingPropertiesHelper testingPropertiesLoader = new TestingPropertiesHelper();
		testingProperties = testingPropertiesLoader.getTestProperties();
		irodsAccount = testingPropertiesHelper.buildIRODSAccountFromTestProperties(testingProperties);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testInstanceNullAccessObjectFactory() throws Exception {
		new TaggingServiceFactoryImpl(null);
	}
	
	public void testGetFreeTaggingService() throws Exception {
		IRODSAccessObjectFactory irodsAccessObjectFactory = Mockito.mock(IRODSAccessObjectFactory.class);
		TaggingServiceFactory taggingServiceFactory = new TaggingServiceFactoryImpl(irodsAccessObjectFactory);
		FreeTaggingService actual = taggingServiceFactory.instanceFreeTaggingService(irodsAccount);
		TestCase.assertNotNull("did not get free tagging service", actual);
	}
	
	public void testIRODSTaggingService() throws Exception {
		IRODSAccessObjectFactory irodsAccessObjectFactory = Mockito.mock(IRODSAccessObjectFactory.class);
		TaggingServiceFactory taggingServiceFactory = new TaggingServiceFactoryImpl(irodsAccessObjectFactory);
		IRODSTaggingService actual = taggingServiceFactory.instanceIrodsTaggingService(irodsAccount);
		TestCase.assertNotNull("did not get irods tagging service", actual);
	}
	
	public void testUserTagCloudService() throws Exception {
		IRODSAccessObjectFactory irodsAccessObjectFactory = Mockito.mock(IRODSAccessObjectFactory.class);
		TaggingServiceFactory taggingServiceFactory = new TaggingServiceFactoryImpl(irodsAccessObjectFactory);
		UserTagCloudService actual = taggingServiceFactory.instanceUserTagCloudService(irodsAccount);
		TestCase.assertNotNull("did not get user tag cloud  service", actual);
	}


}