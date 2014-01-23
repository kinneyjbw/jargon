package org.irods.jargon.transfer.engine;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import junit.framework.Assert;

import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.pub.DataTransferOperations;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.testutils.TestingPropertiesHelper;
import org.irods.jargon.testutils.filemanip.FileGenerator;
import org.irods.jargon.transfer.dao.domain.LocalIRODSTransfer;
import org.irods.jargon.transfer.dao.domain.TransferState;
import org.irods.jargon.transfer.dao.domain.TransferStatus;
import org.irods.jargon.transfer.dao.domain.TransferType;
import org.irods.jargon.transfer.util.HibernateUtil;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TransferManagerTest {

	private static Properties testingProperties = new Properties();

	private static org.irods.jargon.testutils.TestingPropertiesHelper testingPropertiesHelper = new TestingPropertiesHelper();

	private static org.irods.jargon.testutils.filemanip.ScratchFileUtils scratchFileUtils = null;

	public static final String IRODS_TEST_SUBDIR_PATH = "TransferManagerTest";

	private static org.irods.jargon.testutils.IRODSTestSetupUtilities irodsTestSetupUtilities = null;

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
		String databaseUrl = "jdbc:derby:" + System.getProperty("user.home")
				+ "/.idrop/target/database/transfer";
		DatabasePreparationUtils.clearAllDatabaseForTesting(databaseUrl,
				"transfer", "transfer"); // TODO: make a prop

	}

	@Test
	public void testInstance() throws Exception {

		TransferManager transferManager = new TransferManagerImpl(
				IRODSFileSystem.instance());
		Assert.assertNotNull("null transferManager from instance()",
				transferManager);
	}

	/**
	 * @throws Exception
	 */
	@Before
	public void setUpEach() throws Exception {

	}

	@Test
	public void testPause() throws Exception {
		TransferManager transferManager = new TransferManagerImpl(
				IRODSFileSystem.instance());
		transferManager.pause();
		Assert.assertTrue(transferManager.isPaused());
	}

	@Test
	public void testResumeNotPaused() throws Exception {
		TransferManager transferManager = new TransferManagerImpl(
				IRODSFileSystem.instance());
		transferManager.resume();
		Assert.assertFalse(transferManager.getRunningStatus() == TransferManager.RunningStatus.PAUSED);
	}

	@Test
	public void testResumeWhenPaused() throws Exception {
		TransferManager transferManager = new TransferManagerImpl(
				IRODSFileSystem.instance());
		transferManager.pause();
		transferManager.resume();
		Assert.assertFalse(
				"transferManager should not be paused",
				transferManager.getRunningStatus() == TransferManager.RunningStatus.PAUSED);
	}

	@Test
	public void testNotifyWarningCondition() throws Exception {
		TransferManagerImpl transferManager = new TransferManagerImpl(
				IRODSFileSystem.instance());
		transferManager.notifyWarningCondition();
		Assert.assertEquals("transferManager should be in a warning state",
				TransferManager.ErrorStatus.WARNING,
				transferManager.getErrorStatus());
	}

	@Test
	public void testNotifyProcessingCondition() throws Exception {
		TransferManagerImpl transferManager = new TransferManagerImpl(
				IRODSFileSystem.instance());
		transferManager.notifyProcessing();
		Assert.assertEquals("transferManager should be in a processing state",
				TransferManager.RunningStatus.PROCESSING,
				transferManager.getRunningStatus());
	}

	@Test
	public void testNotifyWarningConditionWhenAlreadyError() throws Exception {
		TransferManagerImpl transferManager = new TransferManagerImpl(
				IRODSFileSystem.instance());
		transferManager.notifyErrorCondition();
		transferManager.notifyWarningCondition();
		Assert.assertEquals(
				"transferManager should still be in an error state",
				TransferManager.ErrorStatus.ERROR,
				transferManager.getErrorStatus());
	}

	@Test
	public void testNotifyErrorCondition() throws Exception {
		TransferManagerImpl transferManager = new TransferManagerImpl(
				IRODSFileSystem.instance());
		transferManager.notifyErrorCondition();
		Assert.assertEquals("transferManager should be in an error state",
				TransferManager.ErrorStatus.ERROR,
				transferManager.getErrorStatus());
	}

	@Test
	public void enqueueAPutWhenIdleBlankResource() throws Exception {
		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		String rootCollection = "testGetTransferOneInQueue";
		String localCollectionAbsolutePath = scratchFileUtils
				.createAndReturnAbsoluteScratchPath(IRODS_TEST_SUBDIR_PATH
						+ '/' + rootCollection);

		String irodsCollectionRootAbsolutePath = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH);

		FileGenerator
				.generateManyFilesAndCollectionsInParentCollectionByAbsolutePath(
						localCollectionAbsolutePath, "testSubdir", 1, 2, 1,
						"testFile", ".txt", 9, 8, 2, 21);

		TransferManager transferManager = new TransferManagerImpl(
				IRODSFileSystem.instance());
		transferManager.pause();

		transferManager.enqueueAPut(localCollectionAbsolutePath,
				irodsCollectionRootAbsolutePath, "", irodsAccount);

		Assert.assertEquals("should have been no errors",
				TransferManager.ErrorStatus.OK,
				transferManager.getErrorStatus());
	}

	@Test
	public void testEnqueueAPutWhenPaused() throws Exception {
		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		String rootCollection = "enqueueAPutWhenPaused";
		String localCollectionAbsolutePath = scratchFileUtils
				.createAndReturnAbsoluteScratchPath(IRODS_TEST_SUBDIR_PATH
						+ '/' + rootCollection);

		String irodsCollectionRootAbsolutePath = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH);

		FileGenerator
				.generateManyFilesAndCollectionsInParentCollectionByAbsolutePath(
						localCollectionAbsolutePath, "testSubdir", 1, 2, 1,
						"testFile", ".txt", 9, 8, 2, 21);

		TransferManager transferManager = new TransferManagerImpl(
				IRODSFileSystem.instance());
		transferManager.pause();

		transferManager.enqueueAPut(localCollectionAbsolutePath,
				irodsCollectionRootAbsolutePath, "", irodsAccount);

		Assert.assertEquals("should be finished and idle",
				TransferManager.RunningStatus.PAUSED,
				transferManager.getRunningStatus());
		Assert.assertEquals("should have been no errors",
				TransferManager.ErrorStatus.OK,
				transferManager.getErrorStatus());
		TransferQueueService transferQueueService = transferManager
				.getTransferQueueService();

		List<LocalIRODSTransfer> transfers = transferQueueService
				.getCurrentQueue();

		boolean txfrFound = false;
		for (LocalIRODSTransfer transfer : transfers) {
			if (transfer.getIrodsAbsolutePath().equals(
					irodsCollectionRootAbsolutePath)) {
				txfrFound = true;
				Assert.assertEquals("this should still be enqueued",
						transfer.getTransferState(), TransferState.ENQUEUED);

			}
		}

		Assert.assertTrue("did not find transfer", txfrFound);

	}

	@Test
	public void testEnqueueAPutWithRestart() throws Exception {

		System.out.println(">>>>>>>> testEnqueueAPutWithRestart");

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		IRODSFileSystem irodsFileSystem = IRODSFileSystem.instance();
		String rootCollection = "testEnqueueAPutWithRestart";
		String localCollectionAbsolutePath = scratchFileUtils
				.createAndReturnAbsoluteScratchPath(IRODS_TEST_SUBDIR_PATH
						+ '/' + rootCollection);

		String irodsCollectionRootAbsolutePath = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH);

		FileGenerator.generateManyFilesInGivenDirectory(IRODS_TEST_SUBDIR_PATH
				+ '/' + rootCollection, "test", ".doc", 20, 1, 2);

		// find the 3rd file and make it the last good file

		File localDirectory = new File(localCollectionAbsolutePath);
		File[] localFiles = localDirectory.listFiles();

		Assert.assertTrue("not enough files generated for the test",
				localFiles.length > 3);

		String lastFileName = localFiles[2].getAbsolutePath();

		TransferManager transferManager = new TransferManagerImpl(
				IRODSFileSystem.instance());

		LocalIRODSTransfer enqueuedTransfer = new LocalIRODSTransfer();
		enqueuedTransfer.setCreatedAt(new Date());
		enqueuedTransfer.setIrodsAbsolutePath(irodsCollectionRootAbsolutePath);
		enqueuedTransfer.setLocalAbsolutePath(localCollectionAbsolutePath);
		enqueuedTransfer.setTransferHost(irodsAccount.getHost());
		enqueuedTransfer.setTransferPort(irodsAccount.getPort());
		enqueuedTransfer.setTransferResource(irodsAccount
				.getDefaultStorageResource());
		enqueuedTransfer.setTransferZone(irodsAccount.getZone());
		enqueuedTransfer.setTransferStart(new Date());
		enqueuedTransfer.setTransferType(TransferType.PUT);
		enqueuedTransfer.setTransferUserName(irodsAccount.getUserName());
		enqueuedTransfer.setTransferPassword(HibernateUtil
				.obfuscate(irodsAccount.getPassword()));
		enqueuedTransfer.setTransferState(TransferState.ENQUEUED);
		enqueuedTransfer.setTransferStatus(TransferStatus.OK);
		enqueuedTransfer.setLastSuccessfulPath(lastFileName);

		transferManager.getTransferQueueService().updateLocalIRODSTransfer(
				enqueuedTransfer);

		// let put run

		transferManager.processNextInQueueIfIdle();

		int waitCtr = 0;

		while (true) {
			if (waitCtr++ > 20) {
				Assert.fail("put test timed out");
			}
			Thread.sleep(1000);
			if (transferManager.getRunningStatus() == TransferManager.RunningStatus.IDLE) {
				break;
			}

		}

		Assert.assertEquals("should have been no errors",
				TransferManager.ErrorStatus.OK,
				transferManager.getErrorStatus());

		// now make sure that the first 3 files were not put
		IRODSFile testDir = irodsFileSystem.getIRODSFileFactory(irodsAccount)
				.instanceIRODSFile(
						irodsCollectionRootAbsolutePath + '/' + rootCollection);
		File[] irodsFilesThatWerePut = testDir.listFiles();
		IRODSFile actualIrodsFile = null;

		for (File irodsFile : irodsFilesThatWerePut) {
			actualIrodsFile = (IRODSFile) irodsFile;

			System.out.println("IRODS file:"
					+ actualIrodsFile.getAbsolutePath());

			for (int i = 0; i < 2; i++) {
				if (irodsFile.getName().equals(localFiles[i].getName())) {
					Assert.fail("file should not have been transferred:"
							+ irodsFile.getAbsolutePath());
				}
			}
		}

		Assert.assertEquals("correct number of files were not transferred",
				localFiles.length - 3, irodsFilesThatWerePut.length);

	}

	@Test
	public void testEnqueueAPutTwice() throws Exception {
		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		String rootCollection = "testEnqueueAPutTwice";
		String localCollectionAbsolutePath = scratchFileUtils
				.createAndReturnAbsoluteScratchPath(IRODS_TEST_SUBDIR_PATH
						+ '/' + rootCollection);

		String irodsCollectionRootAbsolutePath = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH);

		FileGenerator
				.generateManyFilesAndCollectionsInParentCollectionByAbsolutePath(
						localCollectionAbsolutePath, "testSubdir", 1, 2, 1,
						"testFile", ".txt", 9, 8, 2, 21);

		TransferManager transferManager = new TransferManagerImpl(
				IRODSFileSystem.instance());
		transferManager.pause();

		transferManager.enqueueAPut(localCollectionAbsolutePath,
				irodsCollectionRootAbsolutePath, "", irodsAccount);
		transferManager.enqueueAPut(localCollectionAbsolutePath,
				irodsCollectionRootAbsolutePath, "", irodsAccount);

		List<LocalIRODSTransfer> transferQueue = transferManager
				.getCurrentQueue();
		Assert.assertEquals(2, transferQueue.size());

	}

	@Test
	public void enqueueAReplicate() throws Exception {
		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		IRODSFileSystem irodsFileSystem = IRODSFileSystem.instance();
		String rootCollection = "enqueueAReplicate";
		String localCollectionAbsolutePath = scratchFileUtils
				.createAndReturnAbsoluteScratchPath(IRODS_TEST_SUBDIR_PATH
						+ '/' + rootCollection);

		String irodsCollectionRootAbsolutePath = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH);

		FileGenerator
				.generateManyFilesAndCollectionsInParentCollectionByAbsolutePath(
						localCollectionAbsolutePath, "testSubdir", 1, 2, 1,
						"testFile", ".txt", 9, 8, 2, 21);

		TransferManager transferManager = new TransferManagerImpl(
				irodsFileSystem);
		// transferManager.pause();

		transferManager.enqueueAPut(localCollectionAbsolutePath,
				irodsCollectionRootAbsolutePath, "", irodsAccount);

		// let put run

		int waitCtr = 0;

		while (true) {
			if (waitCtr++ > 20) {
				Assert.fail("put test timed out");
			}
			Thread.sleep(1000);
			if (transferManager.getRunningStatus() == TransferManager.RunningStatus.IDLE) {
				break;
			}

		}

		Assert.assertEquals("should have been no errors",
				TransferManager.ErrorStatus.OK,
				transferManager.getErrorStatus());

		// put is done, now replicate

		transferManager
				.enqueueAReplicate(
						irodsCollectionRootAbsolutePath + "/" + rootCollection,
						testingProperties
								.getProperty(TestingPropertiesHelper.IRODS_SECONDARY_RESOURCE_KEY),
						irodsAccount);

		waitCtr = 0;

		while (true) {
			if (waitCtr++ > 20) {
				Assert.fail("replicate test timed out");
			}
			Thread.sleep(1000);
			if (transferManager.getRunningStatus().equals(
					TransferManager.RunningStatus.IDLE)) {
				break;
			}
		}

		Assert.assertEquals("should have been no errors",
				TransferManager.ErrorStatus.OK,
				transferManager.getErrorStatus());

		irodsFileSystem.close();

	}

	@Test
	public void enqueueACopy() throws Exception {
		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		IRODSFileSystem irodsFileSystem = IRODSFileSystem.instance();
		String rootCollection = "enqueueACopy";
		String localCollectionAbsolutePath = scratchFileUtils
				.createAndReturnAbsoluteScratchPath(IRODS_TEST_SUBDIR_PATH
						+ '/' + rootCollection);

		String irodsCollectionRootAbsolutePath = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH);

		FileGenerator
				.generateManyFilesAndCollectionsInParentCollectionByAbsolutePath(
						localCollectionAbsolutePath, "testSubdir", 1, 2, 1,
						"testFile", ".txt", 9, 8, 2, 21);

		String targetCollection = "enqueueACopyTarget";
		String irodsTargetCollectionRootAbsolutePath = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + "/"
								+ targetCollection);

		// put the source data into place

		DataTransferOperations dto = irodsFileSystem
				.getIRODSAccessObjectFactory().getDataTransferOperations(
						irodsAccount);
		dto.putOperation(localCollectionAbsolutePath,
				irodsCollectionRootAbsolutePath, "", null, null);

		TransferManager transferManager = new TransferManagerImpl(
				irodsFileSystem);

		transferManager.enqueueACopy(irodsCollectionRootAbsolutePath + "/"
				+ rootCollection, "", irodsTargetCollectionRootAbsolutePath,
				irodsAccount);

		// let put run

		int waitCtr = 0;

		while (true) {
			if (waitCtr++ > 20) {
				Assert.fail("put test timed out");
			}
			Thread.sleep(1000);
			if (transferManager.getRunningStatus() == TransferManager.RunningStatus.IDLE) {
				break;
			}

		}

		Assert.assertEquals("should have been no errors",
				TransferManager.ErrorStatus.OK,
				transferManager.getErrorStatus());

		Assert.assertEquals("should have been no errors",
				TransferManager.ErrorStatus.OK,
				transferManager.getErrorStatus());

		irodsFileSystem.close();

	}

	@Test
	public void enqueueAGet() throws Exception {

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		IRODSFileSystem irodsFileSystem = IRODSFileSystem.instance();
		String rootCollection = "enqueueAGet";
		String returnedCollection = "enqueueAGeReturned";

		String localCollectionAbsolutePath = scratchFileUtils
				.createAndReturnAbsoluteScratchPath(IRODS_TEST_SUBDIR_PATH
						+ '/' + rootCollection);

		String irodsCollectionRootAbsolutePath = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH);

		FileGenerator
				.generateManyFilesAndCollectionsInParentCollectionByAbsolutePath(
						localCollectionAbsolutePath, "testSubdir", 1, 2, 1,
						"testFile", ".txt", 9, 8, 2, 21);

		TransferManager transferManager = new TransferManagerImpl(
				irodsFileSystem);
		// transferManager.pause();

		transferManager.enqueueAPut(localCollectionAbsolutePath,
				irodsCollectionRootAbsolutePath, "", irodsAccount);

		// let put run

		int waitCtr = 0;

		while (true) {
			if (waitCtr++ > 20) {
				Assert.fail("put test timed out");
			}
			Thread.sleep(1000);
			if (transferManager.getRunningStatus() == TransferManager.RunningStatus.IDLE) {
				break;
			}

		}

		Assert.assertEquals("should have been no errors",
				TransferManager.ErrorStatus.OK,
				transferManager.getErrorStatus());

		// put is done, now get

		String localReturnedAbsolutePath = scratchFileUtils
				.createAndReturnAbsoluteScratchPath(IRODS_TEST_SUBDIR_PATH);
		localReturnedAbsolutePath = localReturnedAbsolutePath
				+ returnedCollection;

		transferManager.enqueueAGet(irodsCollectionRootAbsolutePath + "/"
				+ rootCollection, localReturnedAbsolutePath, testingProperties
				.getProperty(TestingPropertiesHelper.IRODS_RESOURCE_KEY),
				irodsAccount);

		waitCtr = 0;

		while (true) {
			if (waitCtr++ > 20) {
				//Assert.fail("get test timed out");
			}
			Thread.sleep(1000);
			if (transferManager.getRunningStatus().equals(
					TransferManager.RunningStatus.IDLE)) {
				break;
			}
		}

		Assert.assertEquals("should have been no errors",
				TransferManager.ErrorStatus.OK,
				transferManager.getErrorStatus());

		irodsFileSystem.close();

	}

	@Test
	public void testProcessQueueAtStartupWithAProcessingTransferHanging()
			throws Exception {
		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		IRODSFileSystem irodsFileSystem = IRODSFileSystem.instance();

		String rootCollection = "testGetErrorQueue";
		String localCollectionAbsolutePath = scratchFileUtils
				.createAndReturnAbsoluteScratchPath(IRODS_TEST_SUBDIR_PATH
						+ '/' + rootCollection);

		String irodsCollectionRootAbsolutePath = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH);

		LocalIRODSTransfer enqueuedTransfer = new LocalIRODSTransfer();
		enqueuedTransfer.setCreatedAt(new Date());
		enqueuedTransfer.setIrodsAbsolutePath(irodsCollectionRootAbsolutePath);
		enqueuedTransfer.setLocalAbsolutePath(localCollectionAbsolutePath);
		enqueuedTransfer.setTransferHost(irodsAccount.getHost());
		enqueuedTransfer.setTransferPort(irodsAccount.getPort());
		enqueuedTransfer.setTransferResource(irodsAccount
				.getDefaultStorageResource());
		enqueuedTransfer.setTransferZone(irodsAccount.getZone());
		enqueuedTransfer.setTransferStart(new Date());
		enqueuedTransfer.setTransferType(TransferType.PUT);
		enqueuedTransfer.setTransferUserName(irodsAccount.getUserName());
		enqueuedTransfer.setTransferPassword(irodsAccount.getPassword());
		enqueuedTransfer.setTransferState(TransferState.PROCESSING);
		enqueuedTransfer.setTransferStatus(TransferStatus.ERROR);

		TransferManager transferManager = new TransferManagerImpl(
				irodsFileSystem);
		transferManager.getTransferQueueService().updateLocalIRODSTransfer(
				enqueuedTransfer);
		// reinit, simulating restart, so that the init() method in
		// transferManagerImpl will fire
		transferManager = new TransferManagerImpl(irodsFileSystem);

		// now get the queue
		List<LocalIRODSTransfer> transferQueue = transferManager
				.getTransferQueueService().getCurrentQueue();
		Assert.assertEquals("did not find the error transaction", 1,
				transferQueue.size());
		LocalIRODSTransfer transfer = transferQueue.get(0);
		Assert.assertEquals("this does not have enqueued status",
				TransferState.ENQUEUED, transfer.getTransferState());

	}
	
	
}