package org.irods.jargon.core.pub;

import java.util.ArrayList;
import java.util.List;

import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.connection.IRODSSession;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.aohelper.CollectionAOHelper;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.pub.io.IRODSFileFactory;
import org.irods.jargon.core.pub.io.IRODSFileFactoryImpl;
import org.irods.jargon.core.pub.io.IRODSFileSystemAOHelper;
import org.irods.jargon.core.query.CollectionAndDataObjectListingEntry;
import org.irods.jargon.core.query.IRODSQuery;
import org.irods.jargon.core.query.IRODSQueryResultRow;
import org.irods.jargon.core.query.IRODSQueryResultSet;
import org.irods.jargon.core.query.JargonQueryException;
import org.irods.jargon.core.query.RodsGenQueryEnum;
import org.irods.jargon.core.utils.IRODSDataConversionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This access object contains methods that can assist in searching across
 * Collections and Data Objects, and in listing across Collections And Data
 * Objects.
 * <p/>
 * It is very common to create interfaces with search boxes, and with tree
 * depictions of the iRODS hierarchy. This class is meant to contain such
 * methods. Note that there are specific search and query methods for Data
 * Objects {@link DataObjectAO} and Collections {@link CollectionAO} that are
 * useful for general development.
 * 
 * @author Mike Conway - DICE (www.irods.org)
 * 
 */
public class CollectionAndDataObjectListAndSearchAOImpl extends IRODSGenericAO
		implements CollectionAndDataObjectListAndSearchAO {

	public static final Logger LOG = LoggerFactory.getLogger(CollectionAndDataObjectListAndSearchAOImpl.class);
	private IRODSFileFactory irodsFileFactory = new IRODSFileFactoryImpl(
			getIRODSSession(), getIRODSAccount());
	private IRODSGenQueryExecutor irodsGenQueryExecutor = new IRODSGenQueryExecutorImpl(
			getIRODSSession(), getIRODSAccount());

	
	protected CollectionAndDataObjectListAndSearchAOImpl(
			final IRODSSession irodsSession, final IRODSAccount irodsAccount)
			throws JargonException {
		super(irodsSession, irodsAccount);
	}


	@Override
	public List<CollectionAndDataObjectListingEntry> listDataObjectsAndCollectionsUnderPath(
			final String absolutePathToParent) throws JargonException {
		List<CollectionAndDataObjectListingEntry> entries = listCollectionsUnderPath(
				absolutePathToParent, 0);
		entries.addAll(listDataObjectsUnderPath(absolutePathToParent, 0));
		return entries;
	}

	
	@Override
	public int countDataObjectsAndCollectionsUnderPath(
			final String absolutePathToParent) throws JargonException {

		if (absolutePathToParent == null) {
			throw new JargonException("absolutePathToParent is null");
		}

		LOG.info("countDataObjectsAndCollectionsUnder: {}",
				absolutePathToParent);
		IRODSFile irodsFile = irodsFileFactory
				.instanceIRODSFile(absolutePathToParent);

		// I cannot get children if this is not a directory (a file has no
		// children)
		if (!irodsFile.isDirectory()) {
			LOG.error(
					"this is a file, not a directory, and therefore I cannot get a count of the children: {}",
					absolutePathToParent);
			throw new JargonException(
					"attempting to count children under a file at path:"
							+ absolutePathToParent);
		}

		IRODSGenQueryExecutor irodsGenQueryExecutor = new IRODSGenQueryExecutorImpl(
				this.getIRODSSession(), this.getIRODSAccount());

		StringBuilder query = new StringBuilder();
		query.append("SELECT COUNT(");

		query.append(RodsGenQueryEnum.COL_COLL_NAME.getName());
		query.append("), COUNT(");
		query.append(RodsGenQueryEnum.COL_DATA_NAME.getName());

		query.append(") WHERE ");
		query.append(RodsGenQueryEnum.COL_COLL_NAME.getName());
		query.append(" = '");
		query.append(IRODSDataConversionUtil
				.escapeSingleQuotes(absolutePathToParent));
		query.append("'");

		IRODSQuery irodsQuery = IRODSQuery.instance(query.toString(), 1);
		IRODSQueryResultSet resultSet;

		try {
			resultSet = irodsGenQueryExecutor.executeIRODSQuery(irodsQuery, 0);
		} catch (JargonQueryException e) {
			LOG.error("query exception for  query:" + query.toString(), e);
			throw new JargonException("error in exists query", e);
		}

		int fileCtr = 0;

		if (resultSet.getResults().size() > 0) {
			fileCtr = IRODSDataConversionUtil
					.getIntOrZeroFromIRODSValue(resultSet.getFirstResult()
							.getColumn(0));
		}

		query = new StringBuilder();
		query.append("SELECT COUNT(");

		query.append(RodsGenQueryEnum.COL_COLL_TYPE.getName());
		query.append(") , COUNT(");
		query.append(RodsGenQueryEnum.COL_COLL_NAME.getName());

		query.append(") WHERE ");
		query.append(RodsGenQueryEnum.COL_COLL_PARENT_NAME.getName());
		query.append(" = '");
		query.append(IRODSDataConversionUtil
				.escapeSingleQuotes(absolutePathToParent));
		query.append("'");

		irodsQuery = IRODSQuery.instance(query.toString(), 1);

		try {
			resultSet = irodsGenQueryExecutor.executeIRODSQuery(irodsQuery, 0);
		} catch (JargonQueryException e) {
			LOG.error("query exception for  query:" + query.toString(), e);
			throw new JargonException("error in exists query", e);
		}

		int collCtr = 0;
		if (resultSet.getResults().size() > 0) {
			collCtr = IRODSDataConversionUtil
					.getIntOrZeroFromIRODSValue(resultSet.getFirstResult()
							.getColumn(0));
		}

		int total = fileCtr + collCtr;

		LOG.debug("computed count = {}", total);
		return total;

	}

	@Override
	public List<CollectionAndDataObjectListingEntry> searchCollectionsBasedOnName(
			final String searchTerm) throws JargonException {

		if (searchTerm == null || searchTerm.isEmpty()) {
			throw new IllegalArgumentException("null or empty search term");
		}

		LOG.info("searchCollectionsBasedOnName:{}", searchTerm);

		StringBuilder sb = new StringBuilder();
		sb.append("SELECT ");
		sb.append(CollectionAOHelper
				.buildSelectsNeededForCollectionsInCollectionsAndDataObjectsListingEntry());
		sb.append(" WHERE ");
		sb.append(RodsGenQueryEnum.COL_COLL_NAME.getName());
		sb.append(" LIKE '%");
		sb.append(searchTerm.trim());
		sb.append("%'");

		IRODSGenQueryExecutor irodsGenQueryExecutor = new IRODSGenQueryExecutorImpl(
				this.getIRODSSession(), this.getIRODSAccount());

		StringBuilder query = new StringBuilder(
				IRODSFileSystemAOHelper.buildQueryListAllDirs(sb.toString()));
		IRODSQuery irodsQuery = IRODSQuery.instance(sb.toString(), 1000);
		IRODSQueryResultSet resultSet;

		try {
			resultSet = irodsGenQueryExecutor.executeIRODSQueryWithPaging(
					irodsQuery, 0);
		} catch (JargonQueryException e) {
			LOG.error("query exception for  query:" + query.toString(), e);
			throw new JargonException("error in exists query", e);
		}

		List<CollectionAndDataObjectListingEntry> entries = new ArrayList<CollectionAndDataObjectListingEntry>();
		for (IRODSQueryResultRow row : resultSet.getResults()) {
			entries.add(CollectionAOHelper
					.buildCollectionListEntryFromResultSetRowForCollectionQuery(row));
		}

		return entries;

	}

	@Override
	public List<CollectionAndDataObjectListingEntry> listCollectionsUnderPath(
			final String absolutePathToParent, final int partialStartIndex)
			throws JargonException {

		if (absolutePathToParent == null) {
			throw new JargonException("absolutePathToParent is null");
		}

		String path;
		List<CollectionAndDataObjectListingEntry> subdirs = new ArrayList<CollectionAndDataObjectListingEntry>();

		if (absolutePathToParent.isEmpty()) {
			path = "/";
		} else {
			path = absolutePathToParent;
		}

		LOG.info("listCollectionsAndDataObjectsUnderPath for: {}", path);
		IRODSFile irodsFile = irodsFileFactory
				.instanceIRODSFile(absolutePathToParent);

		if (irodsFile.isDirectory()) {
			LOG.debug("is directory");
			path = irodsFile.getAbsolutePath();
		} else {
			path = irodsFile.getParent();
			LOG.debug("is file, using parent path: {}", path);
		}

		IRODSGenQueryExecutor irodsGenQueryExecutor = new IRODSGenQueryExecutorImpl(
				this.getIRODSSession(), this.getIRODSAccount());

		StringBuilder query = new StringBuilder(
				IRODSFileSystemAOHelper.buildQueryListAllDirs(path));
		IRODSQuery irodsQuery = IRODSQuery.instance(query.toString(), 1000);
		IRODSQueryResultSet resultSet;

		try {
			resultSet = irodsGenQueryExecutor.executeIRODSQueryWithPaging(
					irodsQuery, partialStartIndex);
		} catch (JargonQueryException e) {
			LOG.error("query exception for  query:" + query.toString(), e);
			throw new JargonException("error in exists query");
		}

		CollectionAndDataObjectListingEntry collectionAndDataObjectListingEntry = null;

		for (IRODSQueryResultRow row : resultSet.getResults()) {
			collectionAndDataObjectListingEntry = CollectionAOHelper
					.buildCollectionListEntryFromResultSetRowForCollectionQuery(row);

			/*
			 * for some reason, a query for collections with a parent of '/'
			 * returns the root as a result, which creates weird situations
			 * when trying to show collections in a tree structure. This
			 * test papers over that idiosyncrasy and discards that
			 * extraneous result.
			 */
			if (!collectionAndDataObjectListingEntry.getPathOrName().equals("/")) {
				subdirs.add(collectionAndDataObjectListingEntry);
			}
		}

		return subdirs;

	}

	@Override
	public List<CollectionAndDataObjectListingEntry> listDataObjectsUnderPath(
			final String absolutePathToParent, final int partialStartIndex)
			throws JargonException {

		if (absolutePathToParent == null) {
			throw new JargonException("absolutePathToParent is null");
		}

		String path;
		List<CollectionAndDataObjectListingEntry> files = new ArrayList<CollectionAndDataObjectListingEntry>();

		if (absolutePathToParent.isEmpty()) {
			return files;
		}

		path = absolutePathToParent;

		LOG.info("listDataObjectsUnderPath for: {}", path);
		IRODSFile irodsFile = irodsFileFactory
				.instanceIRODSFile(absolutePathToParent);

		if (irodsFile.isDirectory()) {
			LOG.debug("is directory");
			path = irodsFile.getAbsolutePath();
		} else {
			path = irodsFile.getParent();
			LOG.debug("is file, using parent path: {}", path);
		}

		IRODSGenQueryExecutor irodsGenQueryExecutor = new IRODSGenQueryExecutorImpl(
				this.getIRODSSession(), this.getIRODSAccount());

		StringBuilder query = new StringBuilder(
				IRODSFileSystemAOHelper
						.buildQueryListAllFilesWithSizeAndDateInfo(path));
		IRODSQuery irodsQuery = IRODSQuery.instance(query.toString(), 1000);
		IRODSQueryResultSet resultSet;

		try {
			resultSet = irodsGenQueryExecutor.executeIRODSQueryWithPaging(
					irodsQuery, partialStartIndex);
		} catch (JargonQueryException e) {
			LOG.error("query exception for  query:" + query.toString(), e);
			throw new JargonException("error in exists query");
		}

		/*
		 * the query that gives the necessary data will cause duplication when
		 * there are replicas, but the data is necessary to get, so discard
		 * duplicates.
		 */
		String lastPath = "";
		String currentPath = "";
		CollectionAndDataObjectListingEntry entry;
		StringBuilder sb;
		for (IRODSQueryResultRow row : resultSet.getResults()) {
			entry = CollectionAOHelper
					.buildCollectionListEntryFromResultSetRowForDataObjectQuery(row);

			sb = new StringBuilder();
			sb.append(entry.getParentPath());
			sb.append('/');
			sb.append(entry.getPathOrName());
			currentPath = sb.toString();
			if (currentPath.equals(lastPath)) {
				continue;
			}

			lastPath = currentPath;
			files.add(entry);
		}

		return files;

	}
}