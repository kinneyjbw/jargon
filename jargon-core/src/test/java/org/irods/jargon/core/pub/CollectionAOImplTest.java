package org.irods.jargon.core.pub;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import junit.framework.Assert;

import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.connection.IRODSServerProperties;
import org.irods.jargon.core.exception.DataNotFoundException;
import org.irods.jargon.core.exception.DuplicateDataException;
import org.irods.jargon.core.exception.FileNotFoundException;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.protovalues.FilePermissionEnum;
import org.irods.jargon.core.pub.domain.AvuData;
import org.irods.jargon.core.pub.domain.Collection;
import org.irods.jargon.core.pub.domain.ObjStat.SpecColType;
import org.irods.jargon.core.pub.domain.UserFilePermission;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.pub.io.IRODSFileFactory;
import org.irods.jargon.core.query.AVUQueryElement;
import org.irods.jargon.core.query.AVUQueryOperatorEnum;
import org.irods.jargon.core.query.MetaDataAndDomainData;
import org.irods.jargon.testutils.TestingPropertiesHelper;
import org.irods.jargon.testutils.filemanip.FileGenerator;
import org.irods.jargon.testutils.icommandinvoke.IcommandInvoker;
import org.irods.jargon.testutils.icommandinvoke.IrodsInvocationContext;
import org.irods.jargon.testutils.icommandinvoke.icommands.ImetaAddCommand;
import org.irods.jargon.testutils.icommandinvoke.icommands.ImetaCommand.MetaObjectType;
import org.irods.jargon.testutils.icommandinvoke.icommands.ImetaListCommand;
import org.irods.jargon.testutils.icommandinvoke.icommands.ImetaRemoveCommand;
import org.irods.jargon.testutils.icommandinvoke.icommands.ImkdirCommand;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class CollectionAOImplTest {

	private static Properties testingProperties = new Properties();
	private static org.irods.jargon.testutils.TestingPropertiesHelper testingPropertiesHelper = new TestingPropertiesHelper();
	private static org.irods.jargon.testutils.filemanip.ScratchFileUtils scratchFileUtils = null;
	public static final String IRODS_TEST_SUBDIR_PATH = "CollectionAOImplTest";
	private static org.irods.jargon.testutils.IRODSTestSetupUtilities irodsTestSetupUtilities = null;
	private static IRODSFileSystem irodsFileSystem = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		org.irods.jargon.testutils.TestingPropertiesHelper testingPropertiesLoader = new TestingPropertiesHelper();
		testingProperties = testingPropertiesLoader.getTestProperties();
		scratchFileUtils = new org.irods.jargon.testutils.filemanip.ScratchFileUtils(
				testingProperties);
		irodsTestSetupUtilities = new org.irods.jargon.testutils.IRODSTestSetupUtilities();
		irodsTestSetupUtilities.initializeIrodsScratchDirectory();
		irodsTestSetupUtilities
				.initializeDirectoryForTest(IRODS_TEST_SUBDIR_PATH);
		irodsFileSystem = IRODSFileSystem.instance();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		irodsFileSystem.closeAndEatExceptions();
	}

	@Test
	public void testCollectionAOImpl() throws Exception {

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();
		CollectionAO collectionAO = accessObjectFactory
				.getCollectionAO(irodsAccount);
		Assert.assertNotNull(collectionAO);
	}

	@Test
	public void testInstanceIRODSFileForCollectionPath() throws Exception {
		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH);

		// now get an irods file and see if it is readable, it should be

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();

		CollectionAO collectionAO = accessObjectFactory
				.getCollectionAO(irodsAccount);
		IRODSFile irodsFile = collectionAO
				.instanceIRODSFileForCollectionPath(targetIrodsCollection);
		Assert.assertNotNull(irodsFile);
		Assert.assertTrue(irodsFile.isDirectory());
	}

	@Test
	public void testInstanceIRODSFileForCollectionPathNotExists()
			throws Exception {
		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();
		CollectionAO collectionAO = accessObjectFactory
				.getCollectionAO(irodsAccount);
		IRODSFile irodsFile = collectionAO
				.instanceIRODSFileForCollectionPath(targetIrodsCollection
						+ "/idontexistshere");
		Assert.assertFalse("this should not exist", irodsFile.exists());
	}

	@Test
	public void testFindDomainByMetadataQuery() throws Exception {

		String testDirName = "testFindDomainByMetadataQuery";
		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + '/'
								+ testDirName);

		// initialize the AVU data
		String expectedAttribName = "testattrib1";
		String expectedAttribValue = "testvalue1";
		String expectedAttribUnits = "test1units";

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();
		CollectionAO collectionAO = accessObjectFactory
				.getCollectionAO(irodsAccount);

		IRODSFile testFile = irodsFileSystem.getIRODSFileFactory(irodsAccount)
				.instanceIRODSFile(targetIrodsCollection);
		testFile.deleteWithForceOption();
		testFile.mkdirs();

		AvuData avuData = AvuData.instance(expectedAttribName,
				expectedAttribValue, expectedAttribUnits);

		collectionAO.deleteAVUMetadata(targetIrodsCollection, avuData);
		collectionAO.addAVUMetadata(targetIrodsCollection, avuData);

		expectedAttribValue = expectedAttribValue.toUpperCase();

		List<AVUQueryElement> queryElements = new ArrayList<AVUQueryElement>();

		queryElements.add(AVUQueryElement.instanceForValueQuery(
				AVUQueryElement.AVUQueryPart.ATTRIBUTE,
				AVUQueryOperatorEnum.EQUAL, expectedAttribName));

		List<Collection> result = collectionAO
				.findDomainByMetadataQuery(queryElements);
		Assert.assertFalse("no query result returned", result.isEmpty());
		Assert.assertEquals(targetIrodsCollection, result.get(0)
				.getCollectionName());
	}

	@Test
	public final void testFindMetadataValuesByMetadataQuery() throws Exception {
		String testDirName = "testFindMetadataValuesByMetadataQuery";
		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + '/'
								+ testDirName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();

		IRODSFile irodsFile = accessObjectFactory.getIRODSFileFactory(
				irodsAccount).instanceIRODSFile(targetIrodsCollection);
		irodsFile.mkdirs();

		CollectionAO collectionAO = accessObjectFactory
				.getCollectionAO(irodsAccount);

		// initialize the AVU data
		String expectedAttribName = "testmdattrib1";
		String expectedAttribValue = "testmdvalue1";
		String expectedAttribUnits = "test1mdunits";

		AvuData avuData = AvuData.instance(expectedAttribName,
				expectedAttribValue, expectedAttribUnits);
		collectionAO.deleteAVUMetadata(targetIrodsCollection, avuData);

		collectionAO.addAVUMetadata(targetIrodsCollection, avuData);

		List<AVUQueryElement> queryElements = new ArrayList<AVUQueryElement>();

		queryElements.add(AVUQueryElement.instanceForValueQuery(
				AVUQueryElement.AVUQueryPart.ATTRIBUTE,
				AVUQueryOperatorEnum.EQUAL, expectedAttribName));

		List<MetaDataAndDomainData> result = collectionAO
				.findMetadataValuesByMetadataQuery(queryElements);
		Assert.assertFalse("no query result returned", result.isEmpty());
	}

	@Test
	public final void testFindMetadataValuesByMetadataQueryCaseInsensitive()
			throws Exception {
		String testDirName = "testFindMetadataValuesByMetadataQuery";
		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + '/'
								+ testDirName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();

		IRODSFile irodsFile = accessObjectFactory.getIRODSFileFactory(
				irodsAccount).instanceIRODSFile(targetIrodsCollection);
		irodsFile.mkdirs();

		CollectionAO collectionAO = accessObjectFactory
				.getCollectionAO(irodsAccount);

		if (!collectionAO.getIRODSServerProperties()
				.isSupportsCaseInsensitiveQueries()) {
			return;
		}

		// initialize the AVU data
		String expectedAttribName = "testmdattrib1".toUpperCase();
		String expectedAttribValue = "testmdvalue1";
		String expectedAttribUnits = "test1mdunits";

		AvuData avuData = AvuData.instance(expectedAttribName,
				expectedAttribValue, expectedAttribUnits);
		collectionAO.deleteAVUMetadata(targetIrodsCollection, avuData);

		collectionAO.addAVUMetadata(targetIrodsCollection, avuData);

		List<AVUQueryElement> queryElements = new ArrayList<AVUQueryElement>();

		queryElements.add(AVUQueryElement.instanceForValueQuery(
				AVUQueryElement.AVUQueryPart.ATTRIBUTE,
				AVUQueryOperatorEnum.EQUAL, expectedAttribName.toLowerCase()));

		List<MetaDataAndDomainData> result = collectionAO
				.findMetadataValuesByMetadataQuery(queryElements, true);
		Assert.assertFalse("no query result returned", result.isEmpty());
	}

	@Test
	public final void testFindMetadataValuesByMetadataQueryForCollection()
			throws Exception {
		String testDirName = "testFindMetadataValuesByMetadataQueryForCollection";
		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + '/'
								+ testDirName);

		// initialize the AVU data
		String expectedAttribName = "testmdattrib1".toUpperCase();
		String expectedAttribValue = "testmdvalue1";
		String expectedAttribUnits = "test1mdunits";

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();

		IRODSFile irodsFile = accessObjectFactory.getIRODSFileFactory(
				irodsAccount).instanceIRODSFile(targetIrodsCollection);
		irodsFile.mkdirs();

		CollectionAO collectionAO = accessObjectFactory
				.getCollectionAO(irodsAccount);

		AvuData avuData = AvuData.instance(expectedAttribName,
				expectedAttribValue, expectedAttribUnits);
		collectionAO.deleteAVUMetadata(targetIrodsCollection, avuData);

		collectionAO.addAVUMetadata(targetIrodsCollection, avuData);

		List<AVUQueryElement> queryElements = new ArrayList<AVUQueryElement>();

		queryElements.add(AVUQueryElement.instanceForValueQuery(
				AVUQueryElement.AVUQueryPart.ATTRIBUTE,
				AVUQueryOperatorEnum.EQUAL, expectedAttribName));

		List<MetaDataAndDomainData> result = collectionAO
				.findMetadataValuesByMetadataQueryForCollection(queryElements,
						targetIrodsCollection);
		Assert.assertFalse("no query result returned", result.isEmpty());
	}

	@Test
	public final void testFindMetadataValuesByMetadataQueryForCollectionCaseInsensitive()
			throws Exception {
		String testDirName = "testFindMetadataValuesByMetadataQueryForCollectionCaseInsensitive";
		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + '/'
								+ testDirName);

		// initialize the AVU data
		String expectedAttribName = "testFindMetadataValuesByMetadataQueryForCollectionCaseInsensitive"
				.toUpperCase();
		String expectedAttribValue = "testFindMetadataValuesByMetadataQueryForCollectionCaseInsensitive";
		String expectedAttribUnits = "testFindMetadataValuesByMetadataQueryForCollectionCaseInsensitive";

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();

		IRODSFile irodsFile = accessObjectFactory.getIRODSFileFactory(
				irodsAccount).instanceIRODSFile(targetIrodsCollection);
		irodsFile.mkdirs();

		CollectionAO collectionAO = accessObjectFactory
				.getCollectionAO(irodsAccount);

		if (!collectionAO.getIRODSServerProperties()
				.isSupportsCaseInsensitiveQueries()) {
			return;
		}

		AvuData avuData = AvuData.instance(expectedAttribName,
				expectedAttribValue, expectedAttribUnits);
		collectionAO.deleteAVUMetadata(targetIrodsCollection, avuData);

		collectionAO.addAVUMetadata(targetIrodsCollection, avuData);

		List<AVUQueryElement> queryElements = new ArrayList<AVUQueryElement>();

		queryElements.add(AVUQueryElement.instanceForValueQuery(
				AVUQueryElement.AVUQueryPart.ATTRIBUTE,
				AVUQueryOperatorEnum.EQUAL, expectedAttribName.toLowerCase()));

		List<MetaDataAndDomainData> result = collectionAO
				.findMetadataValuesByMetadataQueryForCollection(queryElements,
						targetIrodsCollection, 0, true);
		Assert.assertFalse("no query result returned", result.isEmpty());
	}

	@Test
	public final void testFindMetadataValuesByMetadataQueryTwoConditions()
			throws Exception {
		String testDirName = "testFindMetadataValuesByMetadataQueryTwoConditions";
		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + '/'
								+ testDirName);

		// put scratch collection into irods in the right place
		IrodsInvocationContext invocationContext = testingPropertiesHelper
				.buildIRODSInvocationContextFromTestProperties(testingProperties);
		ImkdirCommand imkdirCommand = new ImkdirCommand();
		imkdirCommand.setCollectionName(targetIrodsCollection);

		IcommandInvoker invoker = new IcommandInvoker(invocationContext);
		invoker.invokeCommandAndGetResultAsString(imkdirCommand);

		// initialize the AVU data
		String expectedAttribName = "testmdtwocondattrib1";
		String expectedAttribValue = "testmdtwocondvalue1";
		String expectedAttribUnits = "test1mdtwocondunits";

		ImetaRemoveCommand imetaRemoveCommand = new ImetaRemoveCommand();
		imetaRemoveCommand.setAttribName(expectedAttribName);
		imetaRemoveCommand.setAttribValue(expectedAttribValue);
		imetaRemoveCommand.setAttribUnits(expectedAttribUnits);
		imetaRemoveCommand.setMetaObjectType(MetaObjectType.COLLECTION_META);
		imetaRemoveCommand.setObjectPath(targetIrodsCollection);
		invoker.invokeCommandAndGetResultAsString(imetaRemoveCommand);

		ImetaAddCommand imetaAddCommand = new ImetaAddCommand();
		imetaAddCommand.setMetaObjectType(MetaObjectType.COLLECTION_META);
		imetaAddCommand.setAttribName(expectedAttribName);
		imetaAddCommand.setAttribValue(expectedAttribValue);
		imetaAddCommand.setAttribUnits(expectedAttribUnits);
		imetaAddCommand.setObjectPath(targetIrodsCollection);
		invoker.invokeCommandAndGetResultAsString(imetaAddCommand);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();
		CollectionAO collectionAO = accessObjectFactory
				.getCollectionAO(irodsAccount);

		List<AVUQueryElement> queryElements = new ArrayList<AVUQueryElement>();

		queryElements.add(AVUQueryElement.instanceForValueQuery(
				AVUQueryElement.AVUQueryPart.ATTRIBUTE,
				AVUQueryOperatorEnum.EQUAL, expectedAttribName));

		queryElements.add(AVUQueryElement.instanceForValueQuery(
				AVUQueryElement.AVUQueryPart.VALUE, AVUQueryOperatorEnum.EQUAL,
				expectedAttribValue));

		List<MetaDataAndDomainData> result = collectionAO
				.findMetadataValuesByMetadataQuery(queryElements);
		Assert.assertFalse("no query result returned", result.isEmpty());
	}

	@Test
	public void testAddAvuMetadata() throws Exception {
		String testDirName = "testAddAvuMetadataDir";
		String expectedAttribName = "testattrib1";
		String expectedAttribValue = "testvalue1";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + '/'
								+ testDirName);

		// put scratch collection into irods in the right place
		IrodsInvocationContext invocationContext = testingPropertiesHelper
				.buildIRODSInvocationContextFromTestProperties(testingProperties);
		ImkdirCommand imkdirCommand = new ImkdirCommand();
		imkdirCommand.setCollectionName(targetIrodsCollection);

		IcommandInvoker invoker = new IcommandInvoker(invocationContext);
		invoker.invokeCommandAndGetResultAsString(imkdirCommand);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();
		CollectionAO collectionAO = accessObjectFactory
				.getCollectionAO(irodsAccount);

		AvuData dataToAdd = AvuData.instance(expectedAttribName,
				expectedAttribValue, "");
		collectionAO.addAVUMetadata(targetIrodsCollection, dataToAdd);

		// verify the metadata was added
		// now get back the avu data and make sure it's there
		ImetaListCommand imetaList = new ImetaListCommand();
		imetaList.setAttribName(expectedAttribName);
		imetaList.setMetaObjectType(MetaObjectType.COLLECTION_META);
		imetaList.setObjectPath(targetIrodsCollection);
		String metaValues = invoker
				.invokeCommandAndGetResultAsString(imetaList);
		Assert.assertTrue("did not find expected attrib name",
				metaValues.indexOf(expectedAttribName) > -1);
		Assert.assertTrue("did not find expected attrib value",
				metaValues.indexOf(expectedAttribValue) > -1);

	}

	@Test(expected = DataNotFoundException.class)
	public void testAddAvuMetadataMissingCollection() throws Exception {
		String testDirName = "testAddAvuMetadataMissingCollection";
		String expectedAttribName = "testattrib1";
		String expectedAttribValue = "testvalue1";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + '/'
								+ testDirName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();
		CollectionAO collectionAO = accessObjectFactory
				.getCollectionAO(irodsAccount);

		AvuData dataToAdd = AvuData.instance(expectedAttribName,
				expectedAttribValue, "");
		collectionAO.addAVUMetadata(targetIrodsCollection, dataToAdd);
	}

	@Test
	public void testAddDuplicateAvuMetadata() throws Exception {
		String testDirName = "testAddDuplicateAvuMetadata";
		String expectedAttribName = "testattrib1";
		String expectedAttribValue = "testvalue1";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + '/'
								+ testDirName);

		// put scratch collection into irods in the right place
		IrodsInvocationContext invocationContext = testingPropertiesHelper
				.buildIRODSInvocationContextFromTestProperties(testingProperties);
		ImkdirCommand imkdirCommand = new ImkdirCommand();
		imkdirCommand.setCollectionName(targetIrodsCollection);

		IcommandInvoker invoker = new IcommandInvoker(invocationContext);
		invoker.invokeCommandAndGetResultAsString(imkdirCommand);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();
		CollectionAO collectionAO = accessObjectFactory
				.getCollectionAO(irodsAccount);

		AvuData dataToAdd = AvuData.instance(expectedAttribName,
				expectedAttribValue, "");
		collectionAO.addAVUMetadata(targetIrodsCollection, dataToAdd);
		try {
			collectionAO.addAVUMetadata(targetIrodsCollection, dataToAdd);
		} catch (DuplicateDataException dde) {
			// expected post 3.1, not a great test anymore...
		}

	}

	@Test(expected = DuplicateDataException.class)
	public void testAddDuplicateAvuMetadataWithUnits() throws Exception {
		String testDirName = "testAddDuplicateAvuMetadata";
		String expectedAttribName = "testattrib1";
		String expectedAttribValue = "testvalue1";
		String expectedUnitsValue = "testunits1";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + '/'
								+ testDirName);

		// put scratch collection into irods in the right place
		IrodsInvocationContext invocationContext = testingPropertiesHelper
				.buildIRODSInvocationContextFromTestProperties(testingProperties);
		ImkdirCommand imkdirCommand = new ImkdirCommand();
		imkdirCommand.setCollectionName(targetIrodsCollection);

		IcommandInvoker invoker = new IcommandInvoker(invocationContext);
		invoker.invokeCommandAndGetResultAsString(imkdirCommand);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();
		CollectionAO collectionAO = accessObjectFactory
				.getCollectionAO(irodsAccount);

		AvuData dataToAdd = AvuData.instance(expectedAttribName,
				expectedAttribValue, expectedUnitsValue);
		collectionAO.addAVUMetadata(targetIrodsCollection, dataToAdd);
		collectionAO.addAVUMetadata(targetIrodsCollection, dataToAdd);

	}

	@Test
	public void testAddAvuMetadataWithBarInVals() throws Exception {
		String testDirName = "testAddAvuMetadataWithBarInVals";
		String expectedAttribName = "testattrib1";
		String expectedAttribValue = "testvalue1|somemore";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + '/'
								+ testDirName);

		// put scratch collection into irods in the right place
		IrodsInvocationContext invocationContext = testingPropertiesHelper
				.buildIRODSInvocationContextFromTestProperties(testingProperties);
		ImkdirCommand imkdirCommand = new ImkdirCommand();
		imkdirCommand.setCollectionName(targetIrodsCollection);

		IcommandInvoker invoker = new IcommandInvoker(invocationContext);
		invoker.invokeCommandAndGetResultAsString(imkdirCommand);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();
		CollectionAO collectionAO = accessObjectFactory
				.getCollectionAO(irodsAccount);

		AvuData dataToAdd = AvuData.instance(expectedAttribName,
				expectedAttribValue, "");
		collectionAO.addAVUMetadata(targetIrodsCollection, dataToAdd);

		// verify the metadata was added
		// now get back the avu data and make sure it's there
		ImetaListCommand imetaList = new ImetaListCommand();
		imetaList.setAttribName(expectedAttribName);
		imetaList.setMetaObjectType(MetaObjectType.COLLECTION_META);
		imetaList.setObjectPath(targetIrodsCollection);
		String metaValues = invoker
				.invokeCommandAndGetResultAsString(imetaList);
		Assert.assertTrue("did not find expected attrib name",
				metaValues.indexOf(expectedAttribName) > -1);
		Assert.assertTrue("did not find expected attrib value",
				metaValues.indexOf(expectedAttribValue) > -1);

	}

	@Test
	public void testAddAvuMetadataWithColonInArg() throws Exception {
		String testDirName = "testAddAvuMetadataWithColonInArg";
		String expectedAttribName = "testattrib1:testAttribAnother";
		String expectedAttribValue = "testvalue1|somemore";
		String expectedUnit = "test:thisHasAColon";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + '/'
								+ testDirName);

		// put scratch collection into irods in the right place
		IrodsInvocationContext invocationContext = testingPropertiesHelper
				.buildIRODSInvocationContextFromTestProperties(testingProperties);
		ImkdirCommand imkdirCommand = new ImkdirCommand();
		imkdirCommand.setCollectionName(targetIrodsCollection);

		IcommandInvoker invoker = new IcommandInvoker(invocationContext);
		invoker.invokeCommandAndGetResultAsString(imkdirCommand);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();
		CollectionAO collectionAO = accessObjectFactory
				.getCollectionAO(irodsAccount);

		AvuData dataToAdd = AvuData.instance(expectedAttribName,
				expectedAttribValue, expectedUnit);
		collectionAO.addAVUMetadata(targetIrodsCollection, dataToAdd);

		// verify the metadata was added
		// now get back the avu data and make sure it's there
		ImetaListCommand imetaList = new ImetaListCommand();
		imetaList.setAttribName(expectedAttribName);
		imetaList.setMetaObjectType(MetaObjectType.COLLECTION_META);
		imetaList.setObjectPath(targetIrodsCollection);
		String metaValues = invoker
				.invokeCommandAndGetResultAsString(imetaList);
		Assert.assertTrue("did not find expected attrib name",
				metaValues.indexOf(expectedAttribName) > -1);
		Assert.assertTrue("did not find expected attrib value",
				metaValues.indexOf(expectedAttribValue) > -1);

	}

	@Test
	public void testMkdirThenAddAvuMetadataWithColonInArg() throws Exception {
		String testDirName = "testMkdirThenAddAvuMetadataWithColonInArg";
		String expectedAttribName = "testattrib1:testAttribAnother";
		String expectedAttribValue = "testvalue1|somemore";
		String expectedUnit = "test:thisHasAColon";

		String subdir = "subdir";
		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + '/'
								+ testDirName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		IRODSFile dirFile = irodsFileSystem.getIRODSFileFactory(irodsAccount)
				.instanceIRODSFile(targetIrodsCollection + "/" + subdir);
		dirFile.mkdirs();

		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();
		CollectionAO collectionAO = accessObjectFactory
				.getCollectionAO(irodsAccount);

		AvuData dataToAdd = AvuData.instance(expectedAttribName,
				expectedAttribValue, expectedUnit);
		collectionAO.addAVUMetadata(dirFile.getAbsolutePath(), dataToAdd);
		List<MetaDataAndDomainData> actualMetadata = collectionAO
				.findMetadataValuesForCollection(dirFile.getAbsolutePath());

		Assert.assertEquals("did not get back metadata", 1,
				actualMetadata.size());

	}

	@Test
	public void testRemoveAvuMetadata() throws Exception {
		String testDirName = "testRemoveAvuMetadataTestingDir";
		String expectedAttribName = "testattrib1";
		String expectedAttribValue = "testvalue1";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + '/'
								+ testDirName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();
		CollectionAO collectionAO = accessObjectFactory
				.getCollectionAO(irodsAccount);
		IRODSFile targetCollectionAsFile = irodsFileSystem.getIRODSFileFactory(
				irodsAccount).instanceIRODSFile(targetIrodsCollection);
		targetCollectionAsFile.mkdirs();

		AvuData dataToAdd = AvuData.instance(expectedAttribName,
				expectedAttribValue, "");
		collectionAO.addAVUMetadata(targetIrodsCollection, dataToAdd);

		collectionAO.deleteAVUMetadata(targetIrodsCollection, dataToAdd);

		List<MetaDataAndDomainData> metadata = collectionAO
				.findMetadataValuesForCollection(targetIrodsCollection, 0);

		for (MetaDataAndDomainData metadataEntry : metadata) {
			Assert.assertFalse("did not expect attrib name", metadataEntry
					.getAvuAttribute().equals(expectedAttribName));
		}
	}

	@Test
	public void testOverwriteAvuMetadata() throws Exception {
		String testDirName = "testOverwriteAvuMetadataTestingDir";
		String expectedAttribName = "testOverwriteAvuMetadataAttrib1";
		String expectedAttribValue = "testOverwriteAvuMetadataValue1";
		String expectedNewValue = "testOverwriteAvuMetadataValue1ThatsOverwriten";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + '/'
								+ testDirName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();
		CollectionAO collectionAO = accessObjectFactory
				.getCollectionAO(irodsAccount);
		IRODSFile targetCollectionAsFile = irodsFileSystem.getIRODSFileFactory(
				irodsAccount).instanceIRODSFile(targetIrodsCollection);
		targetCollectionAsFile.mkdirs();

		AvuData dataToAdd = AvuData.instance(expectedAttribName,
				expectedAttribValue, "");
		collectionAO.addAVUMetadata(targetIrodsCollection, dataToAdd);
		AvuData overwriteAvuData = AvuData.instance(expectedAttribName,
				expectedNewValue, "");

		collectionAO.modifyAVUMetadata(targetIrodsCollection, dataToAdd,
				overwriteAvuData);

		List<MetaDataAndDomainData> metadata = collectionAO
				.findMetadataValuesForCollection(targetIrodsCollection, 0);

		Assert.assertEquals("should only be one avu entry", 1, metadata.size());

		for (MetaDataAndDomainData metadataEntry : metadata) {
			Assert.assertEquals("did not find attrib name", expectedAttribName,
					metadataEntry.getAvuAttribute());
			Assert.assertEquals("did not find attrib val", expectedNewValue,
					metadataEntry.getAvuValue());
		}

	}

	/**
	 * [#677] strip quotes, commas from tags
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAddAvuWithEmbedededQuoteInValue() throws Exception {
		String testDirName = "testAddAvuWithEmbedededQuoteInValue";
		String expectedAttribName = "testAddAvuWithEmbedededQuoteInValue1\"";
		String expectedAttribValue = "testAddAvuWithEmbedededQuoteInValue1";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + '/'
								+ testDirName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();
		CollectionAO collectionAO = accessObjectFactory
				.getCollectionAO(irodsAccount);
		IRODSFile targetCollectionAsFile = irodsFileSystem.getIRODSFileFactory(
				irodsAccount).instanceIRODSFile(targetIrodsCollection);
		targetCollectionAsFile.mkdirs();

		AvuData dataToAdd = AvuData.instance(expectedAttribName,
				expectedAttribValue, "");
		collectionAO.addAVUMetadata(targetIrodsCollection, dataToAdd);

		List<MetaDataAndDomainData> metadata = collectionAO
				.findMetadataValuesForCollection(targetIrodsCollection, 0);

		Assert.assertEquals("should only be one avu entry", 1, metadata.size());

		for (MetaDataAndDomainData metadataEntry : metadata) {
			Assert.assertEquals("did not find attrib name", expectedAttribName,
					metadataEntry.getAvuAttribute());
		}

	}

	/**
	 * [#662] mod avu when setting a present units value to blank does not work
	 * 
	 * Currently ignored, seeing if it will be fixed in iRODS - MCC
	 * 
	 * @throws Exception
	 */
	@Test
	public void testOverwriteAvuMetadataWithABlankUnit() throws Exception {
		String testDirName = "testOverwriteAvuMetadataWithABlankUnit";
		String expectedAttribName = "testOverwriteAvuMetadataAttrib1";
		String expectedAttribValue = "testOverwriteAvuMetadataValue1";
		String expectedAttribUnit = "testOverwriteAvuMetadataUnit1";
		String expectedNewUnit = "";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + '/'
								+ testDirName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();

		EnvironmentalInfoAO environmentalInfoAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getEnvironmentalInfoAO(
						irodsAccount);
		IRODSServerProperties props = environmentalInfoAO
				.getIRODSServerPropertiesFromIRODSServer();

		if (!props.isTheIrodsServerAtLeastAtTheGivenReleaseVersion("rods3.2")) {
			irodsFileSystem.closeAndEatExceptions();
			return;
		}

		CollectionAO collectionAO = accessObjectFactory
				.getCollectionAO(irodsAccount);
		IRODSFile targetCollectionAsFile = irodsFileSystem.getIRODSFileFactory(
				irodsAccount).instanceIRODSFile(targetIrodsCollection);
		targetCollectionAsFile.mkdirs();

		AvuData dataToAdd = AvuData.instance(expectedAttribName,
				expectedAttribValue, expectedAttribUnit);
		collectionAO.addAVUMetadata(targetIrodsCollection, dataToAdd);
		AvuData overwriteAvuData = AvuData.instance(expectedAttribName,
				expectedAttribValue, expectedNewUnit);

		collectionAO.modifyAVUMetadata(targetIrodsCollection, dataToAdd,
				overwriteAvuData);

		List<MetaDataAndDomainData> metadata = collectionAO
				.findMetadataValuesForCollection(targetIrodsCollection, 0);

		Assert.assertEquals("should only be one avu entry", 1, metadata.size());

		for (MetaDataAndDomainData metadataEntry : metadata) {
			Assert.assertEquals("did not find attrib name", expectedAttribName,
					metadataEntry.getAvuAttribute());
			Assert.assertEquals("did not find new Unit", expectedNewUnit,
					metadataEntry.getAvuUnit());
		}

	}

	@Test
	public void testOverwriteAvuMetadataWithADifferentUnit() throws Exception {
		String testDirName = "testOverwriteAvuMetadataWithADifferentUnit";
		String expectedAttribName = "testOverwriteAvuMetadataAttrib1";
		String expectedAttribValue = "testOverwriteAvuMetadataValue1";
		String expectedAttribUnit = "testOverwriteAvuMetadataUnit1";
		String expectedNewUnit = "testOverwriteAvuMetadataDifferentUnit1";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + '/'
								+ testDirName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();
		CollectionAO collectionAO = accessObjectFactory
				.getCollectionAO(irodsAccount);
		IRODSFile targetCollectionAsFile = irodsFileSystem.getIRODSFileFactory(
				irodsAccount).instanceIRODSFile(targetIrodsCollection);
		targetCollectionAsFile.mkdirs();

		AvuData dataToAdd = AvuData.instance(expectedAttribName,
				expectedAttribValue, expectedAttribUnit);
		collectionAO.addAVUMetadata(targetIrodsCollection, dataToAdd);
		AvuData overwriteAvuData = AvuData.instance(expectedAttribName,
				expectedAttribValue, expectedNewUnit);

		collectionAO.modifyAVUMetadata(targetIrodsCollection, dataToAdd,
				overwriteAvuData);

		List<MetaDataAndDomainData> metadata = collectionAO
				.findMetadataValuesForCollection(targetIrodsCollection, 0);

		Assert.assertEquals("should only be one avu entry", 1, metadata.size());

		for (MetaDataAndDomainData metadataEntry : metadata) {
			Assert.assertEquals("did not find attrib name", expectedAttribName,
					metadataEntry.getAvuAttribute());
			Assert.assertEquals("did not find new Unit", expectedNewUnit,
					metadataEntry.getAvuUnit());
		}

	}

	@Test
	public void testOverwriteAvuMetadataGivenNameAndUnit() throws Exception {
		String testDirName = "testOverwriteAvuMetadataGivenNameAndUnit";
		String expectedAttribName = "testOverwriteAvuMetadataGivenNameAndUnitAttrib1";
		String expectedAttribValue = "testOverwriteAvuMetadataGivenNameAndUnitValue1";
		String expectedAttribUnit = "testOverwriteAvuMetadataGivenNameAndUnitUnit1";
		String expectedNewValue = "testOverwriteAvuMetadataGivenNameAndUnitThatsOverwriten";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + '/'
								+ testDirName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();
		CollectionAO collectionAO = accessObjectFactory
				.getCollectionAO(irodsAccount);
		IRODSFile targetCollectionAsFile = irodsFileSystem.getIRODSFileFactory(
				irodsAccount).instanceIRODSFile(targetIrodsCollection);
		targetCollectionAsFile.mkdirs();

		AvuData dataToAdd = AvuData.instance(expectedAttribName,
				expectedAttribValue, expectedAttribUnit);
		collectionAO.deleteAVUMetadata(targetIrodsCollection, dataToAdd);
		collectionAO.addAVUMetadata(targetIrodsCollection, dataToAdd);
		AvuData overwriteAvuData = AvuData.instance(expectedAttribName,
				expectedNewValue, expectedAttribUnit);
		collectionAO.modifyAvuValueBasedOnGivenAttributeAndUnit(
				targetIrodsCollection, overwriteAvuData);
		List<MetaDataAndDomainData> metadata = collectionAO
				.findMetadataValuesForCollection(targetIrodsCollection, 0);

		Assert.assertEquals("should only be one avu entry", 1, metadata.size());

		for (MetaDataAndDomainData metadataEntry : metadata) {
			Assert.assertEquals("did not find attrib name", expectedAttribName,
					metadataEntry.getAvuAttribute());
			Assert.assertEquals("did not find attrib val", expectedNewValue,
					metadataEntry.getAvuValue());
			Assert.assertEquals("did not find attrib unit", expectedAttribUnit,
					metadataEntry.getAvuUnit());

		}

	}

	@Test(expected = DataNotFoundException.class)
	public void testOverwriteAvuMetadataGivenNameAndUnitNoVals()
			throws Exception {
		String testDirName = "testOverwriteAvuMetadataGivenNameAndUnitNoVals";
		String expectedAttribName = "testOverwriteAvuMetadataGivenNameAndUnitAttrib1";
		String expectedAttribUnit = "testOverwriteAvuMetadataGivenNameAndUnitUnit1";
		String expectedNewValue = "testOverwriteAvuMetadataGivenNameAndUnitThatsOverwriten";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + '/'
								+ testDirName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();
		CollectionAO collectionAO = accessObjectFactory
				.getCollectionAO(irodsAccount);
		IRODSFile targetCollectionAsFile = irodsFileSystem.getIRODSFileFactory(
				irodsAccount).instanceIRODSFile(targetIrodsCollection);
		targetCollectionAsFile.mkdirs();

		AvuData overwriteAvuData = AvuData.instance(expectedAttribName,
				expectedNewValue, expectedAttribUnit);
		collectionAO.modifyAvuValueBasedOnGivenAttributeAndUnit(
				targetIrodsCollection, overwriteAvuData);

	}

	@Test
	public void testRemoveAvuMetadataAvuDataDoesNotExist() throws Exception {
		String testDirName = "testRemoveAvuMetadataAvuDataDoesNotExistDir";
		String expectedAttribName = "testattrib1";
		String expectedAttribValue = "testvalue1";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + '/'
								+ testDirName);

		// put scratch collection into irods in the right place
		IrodsInvocationContext invocationContext = testingPropertiesHelper
				.buildIRODSInvocationContextFromTestProperties(testingProperties);
		ImkdirCommand imkdirCommand = new ImkdirCommand();
		imkdirCommand.setCollectionName(targetIrodsCollection);

		IcommandInvoker invoker = new IcommandInvoker(invocationContext);
		invoker.invokeCommandAndGetResultAsString(imkdirCommand);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();

		CollectionAO collectionAO = accessObjectFactory
				.getCollectionAO(irodsAccount);

		AvuData avuData = AvuData.instance(expectedAttribName,
				expectedAttribValue, "");

		collectionAO.deleteAVUMetadata(targetIrodsCollection, avuData);
	}

	@Test(expected = DataNotFoundException.class)
	public void testRemoveAvuMetadataCollectionNotExists() throws Exception {
		String testDirName = "testRemoveAvuMetadataIDontExistDir";
		String expectedAttribName = "testattrib1";
		String expectedAttribValue = "testvalue1";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + '/'
								+ testDirName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();

		CollectionAO collectionAO = accessObjectFactory
				.getCollectionAO(irodsAccount);

		AvuData dataToAdd = AvuData.instance(expectedAttribName,
				expectedAttribValue, "");
		collectionAO.deleteAVUMetadata(targetIrodsCollection, dataToAdd);

	}

	@Test
	public void testRewriteAvuMetadata() throws Exception {
		String testDirName = "testRewriteAvuMetadataDir";
		String expectedAttribName = "testrwattrib1";
		String expectedAttribValue = "testrwvalue1";
		String expectedNewAttribValue = "testrwvalue1";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + '/'
								+ testDirName);

		// put scratch collection into irods in the right place
		IrodsInvocationContext invocationContext = testingPropertiesHelper
				.buildIRODSInvocationContextFromTestProperties(testingProperties);
		ImkdirCommand imkdirCommand = new ImkdirCommand();
		imkdirCommand.setCollectionName(targetIrodsCollection);

		IcommandInvoker invoker = new IcommandInvoker(invocationContext);
		invoker.invokeCommandAndGetResultAsString(imkdirCommand);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();

		CollectionAO collectionAO = accessObjectFactory
				.getCollectionAO(irodsAccount);

		AvuData dataToAdd = AvuData.instance(expectedAttribName,
				expectedAttribValue, "");
		collectionAO.addAVUMetadata(targetIrodsCollection, dataToAdd);

		AvuData.instance(expectedAttribValue, expectedNewAttribValue, "");

		// now get back the avu data and make sure it's there
		ImetaListCommand imetaList = new ImetaListCommand();
		imetaList.setAttribName(expectedAttribName);
		imetaList.setMetaObjectType(MetaObjectType.COLLECTION_META);
		imetaList.setObjectPath(targetIrodsCollection);
		String metaValues = invoker
				.invokeCommandAndGetResultAsString(imetaList);
		Assert.assertTrue("did not find expected attrib name",
				metaValues.indexOf(expectedAttribName) > -1);
		Assert.assertTrue("did not find expected attrib value",
				metaValues.indexOf(expectedNewAttribValue) > -1);
	}

	@Test
	public final void findMetadataValuesForCollection() throws Exception {
		String testDirName = "findMetadataValuesForCollection";
		String testSubdir1 = "subdir1";
		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + '/'
								+ testDirName);

		// put scratch collection into irods in the right place
		IrodsInvocationContext invocationContext = testingPropertiesHelper
				.buildIRODSInvocationContextFromTestProperties(testingProperties);
		ImkdirCommand imkdirCommand = new ImkdirCommand();
		imkdirCommand.setCollectionName(targetIrodsCollection);

		IcommandInvoker invoker = new IcommandInvoker(invocationContext);
		invoker.invokeCommandAndGetResultAsString(imkdirCommand);

		imkdirCommand = new ImkdirCommand();
		imkdirCommand.setCollectionName(targetIrodsCollection + "/"
				+ testSubdir1);
		invoker.invokeCommandAndGetResultAsString(imkdirCommand);

		// initialize the AVU data
		String expectedAttribName = "testmdforcollectionattrib1";
		String expectedAttribValue = "testmdforcollectionvalue1";
		String expectedAttribUnits = "test1mdforcollectionunits";

		ImetaRemoveCommand imetaRemoveCommand = new ImetaRemoveCommand();
		imetaRemoveCommand.setAttribName(expectedAttribName);
		imetaRemoveCommand.setAttribValue(expectedAttribValue);
		imetaRemoveCommand.setAttribUnits(expectedAttribUnits);
		imetaRemoveCommand.setMetaObjectType(MetaObjectType.COLLECTION_META);
		imetaRemoveCommand.setObjectPath(targetIrodsCollection + "/"
				+ testSubdir1);
		invoker.invokeCommandAndGetResultAsString(imetaRemoveCommand);

		ImetaAddCommand imetaAddCommand = new ImetaAddCommand();
		imetaAddCommand.setMetaObjectType(MetaObjectType.COLLECTION_META);
		imetaAddCommand.setAttribName(expectedAttribName);
		imetaAddCommand.setAttribValue(expectedAttribValue);
		imetaAddCommand.setAttribUnits(expectedAttribUnits);
		imetaAddCommand
				.setObjectPath(targetIrodsCollection + "/" + testSubdir1);
		invoker.invokeCommandAndGetResultAsString(imetaAddCommand);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		CollectionAO collectionAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getCollectionAO(irodsAccount);

		List<MetaDataAndDomainData> queryResults = collectionAO
				.findMetadataValuesForCollection(targetIrodsCollection + "/"
						+ testSubdir1, 0);

		Assert.assertFalse("no query result returned", queryResults.isEmpty());
	}
	
	@Test
	public void testFindById() throws Exception {
		String testDirName = "testFindById";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + '/'
								+ testDirName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();
		
		IRODSFile collFile = accessObjectFactory.getIRODSFileFactory(irodsAccount).instanceIRODSFile(targetIrodsCollection);
		collFile.mkdirs();

		CollectionAO collectionAO = accessObjectFactory
				.getCollectionAO(irodsAccount);
		Collection collection = collectionAO
				.findByAbsolutePath(targetIrodsCollection);
		Collection actual = collectionAO.findById(collection.getCollectionId());
		Assert.assertNotNull("did not find collection", actual);
	}
	
	@Test(expected=DataNotFoundException.class)
	public void testFindByIdNotFound() throws Exception {

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();
		
		CollectionAO collectionAO = accessObjectFactory
				.getCollectionAO(irodsAccount);
	
		collectionAO.findById(99999999);
	}

	@Test
	public void findByAbsolutePath() throws Exception {
		String testDirName = "findByAbsolutePath";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + '/'
								+ testDirName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();
		
		IRODSFile collFile = accessObjectFactory.getIRODSFileFactory(irodsAccount).instanceIRODSFile(targetIrodsCollection);
		collFile.mkdirs();

		CollectionAO collectionAO = accessObjectFactory
				.getCollectionAO(irodsAccount);
		Collection collection = collectionAO
				.findByAbsolutePath(targetIrodsCollection);

		Assert.assertNotNull("did not find the collection, was null",
				collection);
		Assert.assertEquals("should be normal coll type", SpecColType.NORMAL,
				collection.getSpecColType());
		Assert.assertEquals("absPath should be same as requested path",
				targetIrodsCollection, collection.getCollectionName());
		Assert.assertEquals("collection Name should be same as requested path",
				targetIrodsCollection, collection.getCollectionName());
	}

	@Test(expected = FileNotFoundException.class)
	public void findByAbsolutePathNotExists() throws Exception {
		String testDirName = "findByAbsolutePathNotExists";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + '/'
								+ testDirName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();

		CollectionAO collectionAO = accessObjectFactory
				.getCollectionAO(irodsAccount);
		collectionAO.findByAbsolutePath(targetIrodsCollection);

	}

	@Test(expected = IllegalArgumentException.class)
	public void findByAbsolutePathNullPath() throws Exception {

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();
		CollectionAO collectionAO = accessObjectFactory
				.getCollectionAO(irodsAccount);
		collectionAO.findByAbsolutePath(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCountAllFilesUnderneathCollectionNullFile()
			throws Exception {

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		CollectionAO collectionAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getCollectionAO(irodsAccount);
		collectionAO.countAllFilesUnderneathTheGivenCollection(null);

	}

	@Test(expected = IllegalArgumentException.class)
	public void testCountAllFilesUnderneathCollectionEmptyFile()
			throws Exception {

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		CollectionAO collectionAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getCollectionAO(irodsAccount);
		collectionAO.countAllFilesUnderneathTheGivenCollection("");

	}

	@Test
	public void testPutCollectionWithTwoFilesAndCountThem() throws Exception {

		String rootCollection = "testPutCollectionWithTwoFilesAndCountThemAfter";
		String localCollectionAbsolutePath = scratchFileUtils
				.createAndReturnAbsoluteScratchPath(IRODS_TEST_SUBDIR_PATH
						+ '/' + rootCollection);

		String irodsCollectionRootAbsolutePath = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH);

		FileGenerator
				.generateManyFilesAndCollectionsInParentCollectionByAbsolutePath(
						localCollectionAbsolutePath,
						"testPutCollectionWithTwoFiles", 1, 1, 1, "testFile",
						".txt", 2, 2, 1, 2);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		IRODSFileFactory irodsFileFactory = irodsFileSystem
				.getIRODSFileFactory(irodsAccount);
		IRODSFile destFile = irodsFileFactory
				.instanceIRODSFile(irodsCollectionRootAbsolutePath);
		DataTransferOperations dataTransferOperationsAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getDataTransferOperations(
						irodsAccount);
		File localFile = new File(localCollectionAbsolutePath);

		dataTransferOperationsAO.putOperation(localFile, destFile, null, null);
		destFile.close();

		destFile = irodsFileFactory
				.instanceIRODSFile(irodsCollectionRootAbsolutePath + "/"
						+ rootCollection);

		CollectionAO collectionAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getCollectionAO(irodsAccount);
		int count = collectionAO
				.countAllFilesUnderneathTheGivenCollection(destFile
						.getAbsolutePath());

		Assert.assertEquals("did not get expected file count", 2, count);

	}

	@Test
	public void testFindDomainByMetadataQueryWithTwoAVUQueryElements()
			throws Exception {

		// FIXME: add check of version, don't test before 2.4.1 as this was a
		// bug in iRODS
		String testDirName = "testFindDomainByMetadataQueryWithTwoAVUQueryElements";
		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + '/'
								+ testDirName);

		// initialize the AVU data
		String expectedAttribName = "avujfiejf1221";
		String expectedAttribValue = "avujfiejf1221value1";
		String expectedAttribUnits = "avujfiejf1221units";

		String expectedAttribName2 = "avujfiejf1222";
		String expectedAttribValue2 = "avujfiejf1221value2";
		String expectedAttribUnits2 = "avujfiejf1221units2";

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		EnvironmentalInfoAO environmentalInfoAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getEnvironmentalInfoAO(
						irodsAccount);
		IRODSServerProperties props = environmentalInfoAO
				.getIRODSServerPropertiesFromIRODSServer();

		// test is only valid for post 2.4.1
		if (!props.isTheIrodsServerAtLeastAtTheGivenReleaseVersion("rods2.4.1")) {
			irodsFileSystem.closeAndEatExceptions();
			return;
		}

		CollectionAO collectionAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getCollectionAO(irodsAccount);

		IRODSFile testCollection = irodsFileSystem.getIRODSFileFactory(
				irodsAccount).instanceIRODSFile(targetIrodsCollection);
		testCollection.mkdirs();

		AvuData avuData = AvuData.instance(expectedAttribName,
				expectedAttribValue, expectedAttribUnits);
		collectionAO.addAVUMetadata(testCollection.getAbsolutePath(), avuData);

		avuData = AvuData.instance(expectedAttribName2, expectedAttribValue2,
				expectedAttribUnits2);
		collectionAO.addAVUMetadata(testCollection.getAbsolutePath(), avuData);

		List<AVUQueryElement> queryElements = new ArrayList<AVUQueryElement>();

		queryElements.add(AVUQueryElement.instanceForValueQuery(
				AVUQueryElement.AVUQueryPart.ATTRIBUTE,
				AVUQueryOperatorEnum.EQUAL, expectedAttribName));

		queryElements.add(AVUQueryElement.instanceForValueQuery(
				AVUQueryElement.AVUQueryPart.VALUE, AVUQueryOperatorEnum.EQUAL,
				expectedAttribValue));

		queryElements.add(AVUQueryElement.instanceForValueQuery(
				AVUQueryElement.AVUQueryPart.ATTRIBUTE,
				AVUQueryOperatorEnum.EQUAL, expectedAttribName2));

		queryElements.add(AVUQueryElement.instanceForValueQuery(
				AVUQueryElement.AVUQueryPart.VALUE, AVUQueryOperatorEnum.EQUAL,
				expectedAttribValue2));

		List<Collection> result = collectionAO
				.findDomainByMetadataQuery(queryElements);
		Assert.assertFalse("no query result returned", result.isEmpty());
		Assert.assertEquals(targetIrodsCollection, result.get(0)
				.getCollectionName());

	}

	@Test
	public final void testSetRead() throws Exception {

		String testFileName = "testSetRead";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + "/"
								+ testFileName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		CollectionAO collectionAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getCollectionAO(irodsAccount);
		IRODSFile irodsFile = irodsFileSystem.getIRODSFileFactory(irodsAccount)
				.instanceIRODSFile(targetIrodsCollection);
		irodsFile.mkdirs();

		collectionAO
				.setAccessPermissionRead(
						"",
						targetIrodsCollection,
						testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_SECONDARY_USER_KEY),
						true);

		// log in as the secondary user and test read access
		IRODSAccount secondaryAccount = testingPropertiesHelper
				.buildIRODSAccountFromSecondaryTestProperties(testingProperties);
		IRODSFile irodsFileForSecondaryUser = irodsFileSystem
				.getIRODSFileFactory(secondaryAccount).instanceIRODSFile(
						targetIrodsCollection);
		Assert.assertTrue(irodsFileForSecondaryUser.canRead());

	}

	@Test
	public final void testSetReadAsAdmin() throws Exception {

		String testFileName = "testSetReadAsAdmin";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + "/"
								+ testFileName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		IRODSFile irodsFile = irodsFileSystem.getIRODSFileFactory(irodsAccount)
				.instanceIRODSFile(targetIrodsCollection);
		irodsFile.mkdirs();

		IRODSAccount irodsAccountRods = testingPropertiesHelper
				.buildIRODSAccountForIRODSUserFromTestPropertiesForGivenUser(
						testingProperties,
						testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_ADMIN_USER_KEY),
						testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_ADMIN_PASSWORD_KEY));
		CollectionAO collectionAORods = irodsFileSystem
				.getIRODSAccessObjectFactory()
				.getCollectionAO(irodsAccountRods);

		collectionAORods
				.setAccessPermissionReadAsAdmin(
						"",
						targetIrodsCollection,
						testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_SECONDARY_USER_KEY),
						true);

		// log in as the secondary user and test read access
		IRODSAccount secondaryAccount = testingPropertiesHelper
				.buildIRODSAccountFromSecondaryTestProperties(testingProperties);
		IRODSFile irodsFileForSecondaryUser = irodsFileSystem
				.getIRODSFileFactory(secondaryAccount).instanceIRODSFile(
						targetIrodsCollection);
		Assert.assertTrue(irodsFileForSecondaryUser.canRead());

	}

	@Test
	public final void testSetWriteAsAdmin() throws Exception {

		String testFileName = "testSetWriteAsAdmin";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + "/"
								+ testFileName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		IRODSFile irodsFile = irodsFileSystem.getIRODSFileFactory(irodsAccount)
				.instanceIRODSFile(targetIrodsCollection);
		irodsFile.mkdirs();

		IRODSAccount irodsAccountRods = testingPropertiesHelper
				.buildIRODSAccountForIRODSUserFromTestPropertiesForGivenUser(
						testingProperties,
						testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_ADMIN_USER_KEY),
						testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_ADMIN_PASSWORD_KEY));
		CollectionAO collectionAORods = irodsFileSystem
				.getIRODSAccessObjectFactory()
				.getCollectionAO(irodsAccountRods);

		collectionAORods
				.setAccessPermissionWriteAsAdmin(
						"",
						targetIrodsCollection,
						testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_SECONDARY_USER_KEY),
						true);

		// log in as the secondary user and test read access
		IRODSAccount secondaryAccount = testingPropertiesHelper
				.buildIRODSAccountFromSecondaryTestProperties(testingProperties);
		IRODSFile irodsFileForSecondaryUser = irodsFileSystem
				.getIRODSFileFactory(secondaryAccount).instanceIRODSFile(
						targetIrodsCollection);
		Assert.assertTrue("user should be able to write",
				irodsFileForSecondaryUser.canWrite());

	}

	@Test
	public final void testSetOwnAsAdmin() throws Exception {

		String testFileName = "testSetOwnAsAdmin";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + "/"
								+ testFileName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		IRODSFile irodsFile = irodsFileSystem.getIRODSFileFactory(irodsAccount)
				.instanceIRODSFile(targetIrodsCollection);
		irodsFile.mkdirs();

		IRODSAccount irodsAccountRods = testingPropertiesHelper
				.buildIRODSAccountForIRODSUserFromTestPropertiesForGivenUser(
						testingProperties,
						testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_ADMIN_USER_KEY),
						testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_ADMIN_PASSWORD_KEY));
		CollectionAO collectionAORods = irodsFileSystem
				.getIRODSAccessObjectFactory()
				.getCollectionAO(irodsAccountRods);

		collectionAORods
				.setAccessPermissionOwnAsAdmin(
						"",
						targetIrodsCollection,
						testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_SECONDARY_USER_KEY),
						true);

		// log in as the secondary user and test read access
		IRODSAccount secondaryAccount = testingPropertiesHelper
				.buildIRODSAccountFromSecondaryTestProperties(testingProperties);
		IRODSFile irodsFileForSecondaryUser = irodsFileSystem
				.getIRODSFileFactory(secondaryAccount).instanceIRODSFile(
						targetIrodsCollection);
		Assert.assertTrue("user should be ownder",
				irodsFileForSecondaryUser.canWrite());

	}

	@Test
	public final void testRemovePermissionAsAdmin() throws Exception {

		String testFileName = "testRemovePermissionAsAdmin";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + "/"
								+ testFileName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		IRODSFile irodsFile = irodsFileSystem.getIRODSFileFactory(irodsAccount)
				.instanceIRODSFile(targetIrodsCollection);
		irodsFile.mkdirs();

		IRODSAccount irodsAccountRods = testingPropertiesHelper
				.buildIRODSAccountForIRODSUserFromTestPropertiesForGivenUser(
						testingProperties,
						testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_ADMIN_USER_KEY),
						testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_ADMIN_PASSWORD_KEY));
		CollectionAO collectionAORods = irodsFileSystem
				.getIRODSAccessObjectFactory()
				.getCollectionAO(irodsAccountRods);

		collectionAORods
				.setAccessPermissionOwnAsAdmin(
						"",
						targetIrodsCollection,
						testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_SECONDARY_USER_KEY),
						true);

		// log in as the secondary user
		IRODSAccount secondaryAccount = testingPropertiesHelper
				.buildIRODSAccountFromSecondaryTestProperties(testingProperties);
		CollectionAO collectionAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getCollectionAO(irodsAccount);
		FilePermissionEnum userAccessPermission = collectionAO
				.getPermissionForCollection(targetIrodsCollection,
						secondaryAccount.getUserName(), "");

		Assert.assertTrue("user should be owner",
				userAccessPermission == FilePermissionEnum.OWN);

		collectionAORods
				.removeAccessPermissionForUserAsAdmin(
						"",
						targetIrodsCollection,
						testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_SECONDARY_USER_KEY),
						true);
		userAccessPermission = collectionAO.getPermissionForCollection(
				targetIrodsCollection, secondaryAccount.getUserName(), "");
		Assert.assertTrue("user should not be owner",
				userAccessPermission == FilePermissionEnum.NONE);
	}

	@Test(expected = JargonException.class)
	public final void testSetReadFileNotExist() throws Exception {

		String testFileName = "testSetReadIDontExist";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + "/"
								+ testFileName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		CollectionAO collectionAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getCollectionAO(irodsAccount);
		collectionAO
				.setAccessPermissionRead(
						"",
						targetIrodsCollection,
						testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_SECONDARY_USER_KEY),
						true);

	}

	@Test
	public final void testSetWrite() throws Exception {

		String testFileName = "testSetWrite";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + "/"
								+ testFileName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		CollectionAO collectionAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getCollectionAO(irodsAccount);
		IRODSFile irodsFile = irodsFileSystem.getIRODSFileFactory(irodsAccount)
				.instanceIRODSFile(targetIrodsCollection);
		irodsFile.mkdirs();

		collectionAO
				.setAccessPermissionWrite(
						"",
						targetIrodsCollection,
						testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_SECONDARY_USER_KEY),
						false);

		// log in as the secondary user and test write access
		IRODSAccount secondaryAccount = testingPropertiesHelper
				.buildIRODSAccountFromSecondaryTestProperties(testingProperties);
		IRODSFile irodsFileForSecondaryUser = irodsFileSystem
				.getIRODSFileFactory(secondaryAccount).instanceIRODSFile(
						targetIrodsCollection);
		Assert.assertTrue(irodsFileForSecondaryUser.canWrite());

	}

	@Test
	public final void testSetOwnGivingPermission() throws Exception {

		String testFileName = "testSetOwnGivingPermission";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + "/"
								+ testFileName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		CollectionAO collectionAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getCollectionAO(irodsAccount);
		IRODSFile irodsFile = irodsFileSystem.getIRODSFileFactory(irodsAccount)
				.instanceIRODSFile(targetIrodsCollection);
		irodsFile.mkdirs();

		collectionAO
				.setAccessPermission(
						"",
						targetIrodsCollection,
						testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_SECONDARY_USER_KEY),
						true, FilePermissionEnum.OWN);

		// log in as the secondary user and test write access
		IRODSAccount secondaryAccount = testingPropertiesHelper
				.buildIRODSAccountFromSecondaryTestProperties(testingProperties);
		IRODSFile irodsFileForSecondaryUser = irodsFileSystem
				.getIRODSFileFactory(secondaryAccount).instanceIRODSFile(
						targetIrodsCollection);
		IRODSFileSystemAO irodsFileSystemAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getIRODSFileSystemAO(
						secondaryAccount);
		int permissions = irodsFileSystemAO
				.getDirectoryPermissions(irodsFileForSecondaryUser);

		Assert.assertTrue(permissions >= IRODSFile.OWN_PERMISSIONS);

	}

	@Test
	public final void testSetReadGivingPermission() throws Exception {

		String testFileName = "testSetReadGivingPermission";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + "/"
								+ testFileName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		CollectionAO collectionAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getCollectionAO(irodsAccount);
		IRODSFile irodsFile = irodsFileSystem.getIRODSFileFactory(irodsAccount)
				.instanceIRODSFile(targetIrodsCollection);
		irodsFile.mkdirs();

		collectionAO
				.setAccessPermission(
						"",
						targetIrodsCollection,
						testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_SECONDARY_USER_KEY),
						true, FilePermissionEnum.READ);

		// log in as the secondary user and test write access
		IRODSAccount secondaryAccount = testingPropertiesHelper
				.buildIRODSAccountFromSecondaryTestProperties(testingProperties);
		IRODSFile irodsFileForSecondaryUser = irodsFileSystem
				.getIRODSFileFactory(secondaryAccount).instanceIRODSFile(
						targetIrodsCollection);
		IRODSFileSystemAO irodsFileSystemAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getIRODSFileSystemAO(
						secondaryAccount);
		int permissions = irodsFileSystemAO
				.getDirectoryPermissions(irodsFileForSecondaryUser);

		Assert.assertTrue(permissions >= IRODSFile.READ_PERMISSIONS);

	}

	@Test
	public final void testSetWriteGivingPermission() throws Exception {

		String testFileName = "testSetWriteGivingPermission";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + "/"
								+ testFileName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		CollectionAO collectionAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getCollectionAO(irodsAccount);
		IRODSFile irodsFile = irodsFileSystem.getIRODSFileFactory(irodsAccount)
				.instanceIRODSFile(targetIrodsCollection);
		irodsFile.mkdirs();

		collectionAO
				.setAccessPermission(
						"",
						targetIrodsCollection,
						testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_SECONDARY_USER_KEY),
						true, FilePermissionEnum.WRITE);

		// log in as the secondary user and test write access
		IRODSAccount secondaryAccount = testingPropertiesHelper
				.buildIRODSAccountFromSecondaryTestProperties(testingProperties);
		IRODSFile irodsFileForSecondaryUser = irodsFileSystem
				.getIRODSFileFactory(secondaryAccount).instanceIRODSFile(
						targetIrodsCollection);
		IRODSFileSystemAO irodsFileSystemAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getIRODSFileSystemAO(
						secondaryAccount);
		int permissions = irodsFileSystemAO
				.getDirectoryPermissions(irodsFileForSecondaryUser);

		Assert.assertTrue(permissions >= IRODSFile.WRITE_PERMISSIONS);

	}

	@Test
	public final void testSetWriteThenNoneGivingPermission() throws Exception {

		String testFileName = "testSetWriteThenNoneGivingPermission";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + "/"
								+ testFileName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		CollectionAO collectionAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getCollectionAO(irodsAccount);
		IRODSFile irodsFile = irodsFileSystem.getIRODSFileFactory(irodsAccount)
				.instanceIRODSFile(targetIrodsCollection);
		irodsFile.mkdirs();

		collectionAO
				.setAccessPermission(
						"",
						targetIrodsCollection,
						testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_SECONDARY_USER_KEY),
						true, FilePermissionEnum.WRITE);
		collectionAO
				.setAccessPermission(
						"",
						targetIrodsCollection,
						testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_SECONDARY_USER_KEY),
						true, FilePermissionEnum.NONE);

		// log in as the secondary user and test write access
		IRODSAccount secondaryAccount = testingPropertiesHelper
				.buildIRODSAccountFromSecondaryTestProperties(testingProperties);
		IRODSFile irodsFileForSecondaryUser = irodsFileSystem
				.getIRODSFileFactory(secondaryAccount).instanceIRODSFile(
						targetIrodsCollection);
		IRODSFileSystemAO irodsFileSystemAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getIRODSFileSystemAO(
						secondaryAccount);
		int permissions = irodsFileSystemAO
				.getDirectoryPermissions(irodsFileForSecondaryUser);

		Assert.assertFalse(permissions >= IRODSFile.WRITE_PERMISSIONS);

	}

	@Test(expected = JargonException.class)
	public final void testSetWGivingPermissionUnsupported() throws Exception {

		String testFileName = "testSetWGivingPermissionUnsupported";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + "/"
								+ testFileName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		CollectionAO collectionAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getCollectionAO(irodsAccount);
		IRODSFile irodsFile = irodsFileSystem.getIRODSFileFactory(irodsAccount)
				.instanceIRODSFile(targetIrodsCollection);
		irodsFile.mkdirs();

		collectionAO
				.setAccessPermission(
						"",
						targetIrodsCollection,
						testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_SECONDARY_USER_KEY),
						true, FilePermissionEnum.CREATE_TOKEN);

	}

	@Test
	public final void testSetOwn() throws Exception {

		String testFileName = "testSetOwn";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + "/"
								+ testFileName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		CollectionAO collectionAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getCollectionAO(irodsAccount);
		IRODSFile irodsFile = irodsFileSystem.getIRODSFileFactory(irodsAccount)
				.instanceIRODSFile(targetIrodsCollection);
		irodsFile.mkdirs();

		collectionAO
				.setAccessPermissionOwn(
						"",
						targetIrodsCollection,
						testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_SECONDARY_USER_KEY),
						false);

		// log in as the secondary user and test write access
		IRODSAccount secondaryAccount = testingPropertiesHelper
				.buildIRODSAccountFromSecondaryTestProperties(testingProperties);
		IRODSFile irodsFileForSecondaryUser = irodsFileSystem
				.getIRODSFileFactory(secondaryAccount).instanceIRODSFile(
						targetIrodsCollection);
		IRODSFileSystemAO irodsFileSystemAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getIRODSFileSystemAO(
						secondaryAccount);
		int permissions = irodsFileSystemAO
				.getDirectoryPermissions(irodsFileForSecondaryUser);

		Assert.assertTrue(permissions >= IRODSFile.OWN_PERMISSIONS);

	}

	@Test
	public final void testGetFilePermissionForOwn() throws Exception {

		String testFileName = "testGetFilePermissionForOwn";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + "/"
								+ testFileName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		CollectionAO collectionAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getCollectionAO(irodsAccount);
		IRODSFile irodsFile = irodsFileSystem.getIRODSFileFactory(irodsAccount)
				.instanceIRODSFile(targetIrodsCollection);
		irodsFile.mkdirs();

		collectionAO
				.setAccessPermissionOwn(
						"",
						targetIrodsCollection,
						testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_SECONDARY_USER_KEY),
						false);

		// log in as the secondary user and test write access
		IRODSAccount secondaryAccount = testingPropertiesHelper
				.buildIRODSAccountFromSecondaryTestProperties(testingProperties);
		CollectionAO collectionAOSecondary = irodsFileSystem
				.getIRODSAccessObjectFactory()
				.getCollectionAO(secondaryAccount);
		FilePermissionEnum enumVal = collectionAOSecondary
				.getPermissionForCollection(targetIrodsCollection,
						secondaryAccount.getUserName(), "");

		Assert.assertEquals("should have found own permissions",
				FilePermissionEnum.OWN, enumVal);

	}

	@Test
	public final void testGetFilePermissionForOtherUserWhoHasRead()
			throws Exception {

		String testFileName = "testGetFilePermissionForOtherUserWhoHasRead";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + "/"
								+ testFileName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		CollectionAO collectionAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getCollectionAO(irodsAccount);
		IRODSFile irodsFile = irodsFileSystem.getIRODSFileFactory(irodsAccount)
				.instanceIRODSFile(targetIrodsCollection);
		irodsFile.mkdirs();

		collectionAO
				.setAccessPermissionRead(
						"",
						targetIrodsCollection,
						testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_SECONDARY_USER_KEY),
						false);

		// log in as the secondary user and test write access
		IRODSAccount secondaryAccount = testingPropertiesHelper
				.buildIRODSAccountFromSecondaryTestProperties(testingProperties);

		FilePermissionEnum enumVal = collectionAO.getPermissionForCollection(
				targetIrodsCollection, secondaryAccount.getUserName(), "");

		Assert.assertEquals("should have found read permissions",
				FilePermissionEnum.READ, enumVal);

	}

	@Test
	public final void testSetInherit() throws Exception {

		String testFileName = "testSetInherit";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + "/"
								+ testFileName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		CollectionAO collectionAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getCollectionAO(irodsAccount);
		IRODSFile irodsFile = irodsFileSystem.getIRODSFileFactory(irodsAccount)
				.instanceIRODSFile(targetIrodsCollection);
		irodsFile.mkdirs();

		collectionAO.setAccessPermissionInherit("", targetIrodsCollection,
				false);

		boolean isInherit = collectionAO
				.isCollectionSetForPermissionInheritance(targetIrodsCollection);

		Assert.assertTrue("collection should have inherit set", isInherit);

	}

	@Test
	public final void testSetNoInherit() throws Exception {

		String testFileName = "testSetNoInherit";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + "/"
								+ testFileName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		CollectionAO collectionAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getCollectionAO(irodsAccount);
		IRODSFile irodsFile = irodsFileSystem.getIRODSFileFactory(irodsAccount)
				.instanceIRODSFile(targetIrodsCollection);
		irodsFile.mkdirs();

		collectionAO.setAccessPermissionInherit("", targetIrodsCollection,
				false);
		collectionAO.setAccessPermissionToNotInherit("", targetIrodsCollection,
				false);

		boolean isInherit = collectionAO
				.isCollectionSetForPermissionInheritance(targetIrodsCollection);

		Assert.assertFalse("collection should have inherit turned back off",
				isInherit);

	}

	@Test
	public final void testGetPermissionsForCollection() throws Exception {

		String testCollectionName = "testGetPermissionsForCollection";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + "/"
								+ testCollectionName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		CollectionAO collectionAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getCollectionAO(irodsAccount);
		IRODSFile irodsFile = irodsFileSystem.getIRODSFileFactory(irodsAccount)
				.instanceIRODSFile(targetIrodsCollection);
		irodsFile.mkdirs();

		collectionAO
				.setAccessPermissionRead(
						"",
						targetIrodsCollection,
						testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_SECONDARY_USER_KEY),
						true);

		List<UserFilePermission> userFilePermissions = collectionAO
				.listPermissionsForCollection(targetIrodsCollection);
		Assert.assertNotNull("got a null userFilePermissions",
				userFilePermissions);
		Assert.assertEquals("did not find the two permissions", 2,
				userFilePermissions.size());

		boolean secondaryUserFound = false;
		for (UserFilePermission permission : userFilePermissions) {
			if (permission
					.getUserName()
					.equalsIgnoreCase(
							testingProperties
									.getProperty(TestingPropertiesHelper.IRODS_SECONDARY_USER_KEY))) {
				secondaryUserFound = true;
				Assert.assertEquals("should have normal zone",
						irodsAccount.getZone(), permission.getUserZone());
				Assert.assertEquals("should have read permissions",
						FilePermissionEnum.READ,
						permission.getFilePermissionEnum());
			}
		}

		Assert.assertTrue("did not find secondary user", secondaryUserFound);

	}

	@Test
	public final void testGetPermissionsForCollectionForUser() throws Exception {

		String testCollectionName = "testGetPermissionsForCollectionForUser";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + "/"
								+ testCollectionName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		CollectionAO collectionAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getCollectionAO(irodsAccount);
		IRODSFile irodsFile = irodsFileSystem.getIRODSFileFactory(irodsAccount)
				.instanceIRODSFile(targetIrodsCollection);
		irodsFile.mkdirs();

		collectionAO
				.setAccessPermissionRead(
						"",
						targetIrodsCollection,
						testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_SECONDARY_USER_KEY),
						true);

		UserFilePermission userFilePermission = collectionAO
				.getPermissionForUserName(
						targetIrodsCollection,
						testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_SECONDARY_USER_KEY));
		Assert.assertNotNull("got a null userFilePermission",
				userFilePermission);
		Assert.assertEquals(
				"userName did not match expected",
				testingProperties
						.getProperty(TestingPropertiesHelper.IRODS_SECONDARY_USER_KEY),
				userFilePermission.getUserName());
		Assert.assertEquals("zone incorrect", irodsAccount.getZone(),
				userFilePermission.getUserZone());
	}

	@Test
	public final void testGetPermissionsForCollectionForUserNoUser()
			throws Exception {

		String testCollectionName = "testGetPermissionsForCollectionForUser";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + "/"
								+ testCollectionName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		CollectionAO collectionAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getCollectionAO(irodsAccount);
		IRODSFile irodsFile = irodsFileSystem.getIRODSFileFactory(irodsAccount)
				.instanceIRODSFile(targetIrodsCollection);
		irodsFile.mkdirs();

		collectionAO
				.setAccessPermissionRead(
						"",
						targetIrodsCollection,
						testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_SECONDARY_USER_KEY),
						true);

		UserFilePermission userFilePermission = collectionAO
				.getPermissionForUserName(targetIrodsCollection, "notausername");
		Assert.assertNull(
				"got a userFilePermission when should have been null",
				userFilePermission);

	}

	@Test
	public final void testIsAccessForCollectionForUserWhenRead()
			throws Exception {

		String testCollectionName = "testIsAccessForCollectionForUserWhenRead";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + "/"
								+ testCollectionName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		CollectionAO collectionAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getCollectionAO(irodsAccount);
		IRODSFile irodsFile = irodsFileSystem.getIRODSFileFactory(irodsAccount)
				.instanceIRODSFile(targetIrodsCollection);
		irodsFile.mkdirs();

		collectionAO
				.setAccessPermissionRead(
						"",
						targetIrodsCollection,
						testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_SECONDARY_USER_KEY),
						true);

		boolean hasPermission = collectionAO
				.isUserHasAccess(
						targetIrodsCollection,
						testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_SECONDARY_USER_KEY));
		Assert.assertTrue("did not get expected permission", hasPermission);

	}

	@Test
	public final void testIsAccessForCollectionForUserWhenNoPermission()
			throws Exception {

		String testCollectionName = "testIsAccessForCollectionForUserWhenNoPermission";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + "/"
								+ testCollectionName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		CollectionAO collectionAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getCollectionAO(irodsAccount);
		IRODSFile irodsFile = irodsFileSystem.getIRODSFileFactory(irodsAccount)
				.instanceIRODSFile(targetIrodsCollection);
		irodsFile.mkdirs();

		boolean hasPermission = collectionAO
				.isUserHasAccess(
						targetIrodsCollection,
						testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_SECONDARY_USER_KEY));
		Assert.assertFalse("should not have permission", hasPermission);

	}

	@Test(expected = IllegalArgumentException.class)
	public final void testIsUserHasAccessNullFileName() throws Exception {
		// generate a local scratch file

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		CollectionAO collectionAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getCollectionAO(irodsAccount);
		collectionAO.isUserHasAccess(null, "hello");
	}

	@Test(expected = IllegalArgumentException.class)
	public final void testIsUserHasAccessBlankFileName() throws Exception {
		// generate a local scratch file

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		CollectionAO collectionAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getCollectionAO(irodsAccount);
		collectionAO.isUserHasAccess("", "hello");
	}

	@Test(expected = IllegalArgumentException.class)
	public final void testIsUserHasAccessNullUserName() throws Exception {
		// generate a local scratch file

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		CollectionAO collectionAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getCollectionAO(irodsAccount);
		collectionAO.isUserHasAccess("file", null);
	}

	@Test(expected = IllegalArgumentException.class)
	public final void testIsUserHasAccessBlankUserName() throws Exception {
		// generate a local scratch file

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		CollectionAO collectionAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getCollectionAO(irodsAccount);

		collectionAO.isUserHasAccess("file", "");
	}

	/**
	 * Bug [#1080] metadata edit seems to fail with file with ' in name
	 * 
	 * @throws Exception
	 */
	@Test
	public void testQueryAVUWithApostropheInFileNameBug1080() throws Exception {
		String testDirName = "testAddAvuMetadataWith ' InVals";
		String expectedAttribName = "testQueryAVUWithApostropheInFileNameBug1080 attrib";
		String expectedAttribValue = "testQueryAVUWithApostropheInFileNameBug1080 value";

		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + '/'
								+ testDirName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();

		IRODSFile targetIRODSFile = accessObjectFactory.getIRODSFileFactory(
				irodsAccount).instanceIRODSFile(targetIrodsCollection);
		targetIRODSFile.mkdirs();

		CollectionAO collectionAO = accessObjectFactory
				.getCollectionAO(irodsAccount);

		AvuData dataToAdd = AvuData.instance(expectedAttribName,
				expectedAttribValue, "");
		collectionAO.addAVUMetadata(targetIrodsCollection, dataToAdd);

		List<MetaDataAndDomainData> metadata = collectionAO
				.findMetadataValuesForCollection(targetIrodsCollection, 0);
		Assert.assertFalse("metadata not retrieved", metadata.isEmpty());

	}

}
