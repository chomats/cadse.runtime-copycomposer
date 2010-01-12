/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package fede.workspace.eclipse.composition.copy.composer;

import java.util.List;
import java.util.Properties;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import fede.workspace.eclipse.composition.copy.exporter.FolderExportedContent;
import fede.workspace.eclipse.composition.copy.exporter.PropertyFile;
import fede.workspace.tool.eclipse.MappingManager;
import fr.imag.adele.cadse.core.CadseException;
import fr.imag.adele.cadse.core.Item;
import fr.imag.adele.cadse.core.build.IExportedContent;
import fr.imag.adele.fede.workspace.si.view.View;

/**
 * Represent a folder which is the target place to make a composition.
 * 
 * @author Thomas
 * 
 */
public class FolderExporterTarget implements ICompositeDescExporterTarget {

	private static final String	TEMP_FOLDER_NAME								= ".temp";
	private static final String	REPO_FILE_NAME									= "repo.properties";
	private static final String	CREATED_FOLDER_PATH_PART_PROP_NAME				= "createdFolderPathPart";
	private static final String	LAST_CREATED_FOLDER_PATH_PART_PROP_NAME			= "lastCreatedFolderPathPart";
	private static final String	LAST_TARGET_FOLDER_PROP_NAME					= "lastTargetFolder";
	private static final String	CURRENT_TARGET_FOLDER_PROP_NAME					= "currentTargetFolder";
	private static final String	CHANGE_TARGET_FOLDER_PROCESS_PHASE1_PROP_NAME	= "changeTargetFolderProcessPhase1";

	private IContainer			_targetFolder;

	private IContainer			_targetParentContainer;

	private Item				_item;

	private String				_composerType;

	private String				_composerName;

	public FolderExporterTarget(Item compositeItem, IPath targetPath, String composerType, String composerName)
			throws CoreException {

		this._item = compositeItem;
		this._composerType = composerType;
		this._composerName = composerName;
		this._targetParentContainer = compositeItem.getContentItem().getMainMappingContent(IContainer.class);
		this._targetFolder = targetPath.isEmpty() ? _targetParentContainer : _targetParentContainer
				.getFolder(targetPath);

		// create the target folder if it does not exist
		if (!_targetFolder.exists()) {
			createTargetFolder();
		}
	}

	/**
	 * Return the composite item related to the composer which instantiated this
	 * exporter target.
	 * 
	 * @return the composite item related to the composer which instantiated
	 *         this exporter target.
	 */
	public Item getCompositeItem() {
		return _item;
	}

	private void createTargetFolder() throws CoreException {

		saveCreatedFolderNames();

		if (FileUtil.isFolder(_targetFolder)) {
			MappingManager.createFolder((IFolder) _targetFolder, View.getDefaultMonitor());
		}
	}

	private void saveCreatedFolderNames() {

		// save the last created folder part when the target folder has changed
		if (targetFolderChanged()) {
			Properties repoProps = loadRepoProperties();
			String lastCreatedPath = repoProps.getProperty(CREATED_FOLDER_PATH_PART_PROP_NAME, "");
			repoProps.setProperty(LAST_CREATED_FOLDER_PATH_PART_PROP_NAME, lastCreatedPath);
			saveRepositoryProperties(repoProps);
		}

		// list the names of the folders which will be created
		IPath relPath = _targetFolder.getProjectRelativePath();
		String createdPathPart = "";
		if (relPath.segmentCount() <= 0) {
			int i = 1;
			while ((i < relPath.segmentCount() + 1) && (_targetParentContainer.exists(relPath.uptoSegment(i)))) {
				/*
				 * do nothing
				 */
				i++;
			}
			IPath createdPath = (i > relPath.segmentCount()) ? relPath : relPath.uptoSegment(i);
			createdPathPart = createdPath.toPortableString();
		}

		// save the names of the folders which will be created
		Properties repoProps = loadRepoProperties();
		repoProps.setProperty(CREATED_FOLDER_PATH_PART_PROP_NAME, createdPathPart);
		saveRepositoryProperties(repoProps);
	}

	public IContainer getTargetContainer() {
		return getTargetFolder();
	}

	public IContainer getTargetParentContainer() {
		return _targetParentContainer;
	}

	public List<IExportedContent> getRepositoryComponents(String exporterType) throws CadseException {
		try {
			return getRepository(exporterType).getExistingContents();
		} catch (CoreException e) {
			throw new CadseException("Unable to find the repository for exporter type = " + exporterType, e);
		}
	}

	public IRepository getRepository(String exporterType) throws CoreException {

		IFolder repoFolder = getGlobalRepoFolder().getFolder(exporterType);

		return new FilesystemRepository(repoFolder);
	}

	private IFolder getGlobalRepoFolder() {
		return _targetParentContainer.getFolder(new Path("." + _composerType)).getFolder(getCompositeItemId())
				.getFolder(_composerName);
	}

	private String getCompositeItemId() {
		return _item.getId().toString();
	}

	/**
	 * Return the current target folder used by the composer.
	 * 
	 * @return the current target folder used by the composer.
	 */
	public IContainer getTargetFolder() {
		return _targetFolder;
	}

	/**
	 * Return the current target folder used by the composer.
	 * 
	 * @return the current target folder used by the composer.
	 * @throws CoreException
	 */
	public IContainer getTargetFolder(String path) throws CoreException {
		if (FolderExportedContent.DEFAULT_TARGET.equals(path)) {
			return _targetFolder;
		}
		Path targetPath = new Path(path);
		IContainer folder = targetPath.isEmpty() ? _targetParentContainer : _targetParentContainer
				.getFolder(targetPath);
		if (!folder.exists() && FileUtil.isFolder(folder)) {
			MappingManager.createFolder((IFolder) folder, View.getDefaultMonitor());
		}

		return folder;
	}

	/**
	 * Return the last target folder (from the last build) which has been used
	 * by the composer. If there is no previous build, return null.
	 * 
	 * @return the last folder (from the last build) which has been exposed to
	 *         the composers.
	 * @throws CoreException
	 *             if an error happens during loading of the repository
	 *             properties.
	 */
	public IContainer getLastTargetFolder() throws CoreException {

		Properties repoProps = loadRepoProperties();
		String lastTargetFoldPath = repoProps.getProperty(LAST_TARGET_FOLDER_PROP_NAME);
		if (lastTargetFoldPath != null) {
			IPath relPath = PathUtil
					.getRelativePath(_targetParentContainer.getFullPath(), new Path(lastTargetFoldPath));
			if (relPath.isEmpty()) {
				return _targetParentContainer;
			}
			return _targetParentContainer.getFolder(relPath);
		}

		return null;
	}

	private void saveRepositoryProperties(Properties repoProps) {

		PropertyFile propFile = getPropertyFile();
		propFile.saveProperties(repoProps, "Generated by FolderExporterTarget.");
	}

	private PropertyFile getPropertyFile() {
		IFile repoFile = getGlobalRepoFolder().getFile(REPO_FILE_NAME);
		PropertyFile propFile = new PropertyFile(repoFile);
		return propFile;
	}

	/**
	 * Return all the properties saved in the repository specified file.
	 * 
	 * @return all the properties saved in the repository specified file.
	 */
	private Properties loadRepoProperties() {

		PropertyFile propFile = getPropertyFile();
		return propFile.loadProperties();
	}

	/**
	 * Return true if and only if the target folder has changed from the last
	 * build.
	 * 
	 * @return true if and only if the target folder has changed from the last
	 *         build.
	 */
	public boolean targetFolderChanged() {

		try {
			IContainer lastTargetFolder = getLastTargetFolder();
			if (lastTargetFolder == null) {
				return false; // first time build
			}

			return !getTargetFolder().equals(lastTargetFolder);
		} catch (CoreException e) {
			e.printStackTrace();
			// if not able to know, consider true
			// (move last target folder to new target folder is same as no
			// modification if they are equal)
			return true;
		}
	}

	/**
	 * Flag the target folder change process as started.
	 */
	public void startTargetChangeProcess() {
		Properties repoProps = loadRepoProperties();

		repoProps.setProperty(CHANGE_TARGET_FOLDER_PROCESS_PHASE1_PROP_NAME, Boolean.toString(false));

		saveRepositoryProperties(repoProps);
	}

	/**
	 * Flag the target folder change process as finished.
	 */
	public void finishTargetChangeProcess() {
		Properties repoProps = loadRepoProperties();

		repoProps.setProperty(CHANGE_TARGET_FOLDER_PROCESS_PHASE1_PROP_NAME, Boolean.toString(false));
		String newTargetFolderStr = getTargetFolder().getFullPath().toPortableString();
		repoProps.setProperty(LAST_TARGET_FOLDER_PROP_NAME, newTargetFolderStr);
		repoProps.setProperty(CURRENT_TARGET_FOLDER_PROP_NAME, newTargetFolderStr);

		saveRepositoryProperties(repoProps);
	}

	/**
	 * Flag the target folder change process phase1 as completed.
	 */
	public void finishTargetChangeProcessPhase1() {
		Properties repoProps = loadRepoProperties();

		repoProps.setProperty(CHANGE_TARGET_FOLDER_PROCESS_PHASE1_PROP_NAME, Boolean.toString(true));

		saveRepositoryProperties(repoProps);
	}

	/**
	 * Return the temporary directory. Note that it might not be created (never
	 * return null).
	 * 
	 * @return the temporary directory.
	 */
	public IFolder getTemporaryFolder() {
		return getGlobalRepoFolder().getFolder(TEMP_FOLDER_NAME);
	}

	/**
	 * Create a temporary directory and return it.
	 * 
	 * @return a temporary directory.
	 */
	public IFolder createTemporaryFolder() {
		IFolder tempFold = getTemporaryFolder();

		// We don't delete temporary folder content
		// as some content can be no more available anywhere else

		try {
			MappingManager.createFolder(tempFold, View.getDefaultMonitor());
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

		return tempFold;
	}

	/**
	 * Delete the temporary directory if it exists.
	 */
	public void deleteTemporaryFolder() {
		IFolder tempFold = getTemporaryFolder();

		if (tempFold.exists()) {
			try {
				tempFold.delete(true, View.getDefaultMonitor());
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Save the current target folder path in the repository properties.
	 */
	public void saveTargetFolderRef() {
		Properties repoProps = loadRepoProperties();

		String targetFolderStr = getTargetFolder().getFullPath().toPortableString();
		repoProps.setProperty(LAST_TARGET_FOLDER_PROP_NAME, targetFolderStr);
		repoProps.setProperty(CURRENT_TARGET_FOLDER_PROP_NAME, targetFolderStr);

		saveRepositoryProperties(repoProps);
	}

	/**
	 * Return true if and only if a target folder change process has been
	 * started and its phase 1 is finished.
	 * 
	 * @return true if and only if a target folder change process has been
	 *         started and its phase 1 is finished.
	 */
	public boolean targetFolderChangeProcessPhase1Finished() {

		Properties repoProps = loadRepoProperties();
		String phase1Finished = repoProps.getProperty(CHANGE_TARGET_FOLDER_PROCESS_PHASE1_PROP_NAME);
		return Boolean.parseBoolean(phase1Finished);
	}

	/**
	 * Delete the last target folder if it is empty and has been created by the
	 * composer of this target.
	 */
	public void deleteOldTargetFolder() {

		// only delete it if it is empty
		try {
			if (getLastTargetFolder().members(false).length != 0) {
				return;
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new IllegalStateException("Can't get a reference on the last target folder.");
		}

		// only delete it if it has been created by us
		Properties repoProps = loadRepoProperties();
		String createdFolderPathPartStr = repoProps.getProperty(LAST_CREATED_FOLDER_PATH_PART_PROP_NAME, "");
		IPath createdFolderPathPart = Path.fromPortableString(createdFolderPathPartStr);
		if (createdFolderPathPart.isEmpty()) {
			return;
		}

		try {
			IPath relPath = getLastTargetFolder().getProjectRelativePath();
			if (relPath.segmentCount() == 0) {
				return;
			}

			int i = relPath.segmentCount();
			while (i > 0) {
				IFolder folder = _targetParentContainer.getFolder(relPath.uptoSegment(i));
				if ((!folder.exists()) || (!containsOnlyFolders(folder))) {
					break;
				}

				i--;
			}

			// delete the empty created folders
			IPath emptyFoldPath = (i == relPath.segmentCount()) ? relPath : relPath.uptoSegment(i + 1);
			IPath foldToDelPath = emptyFoldPath;
			if (emptyFoldPath.segmentCount() <= (relPath.segmentCount() - createdFolderPathPart.segmentCount())) {
				int segNb = (relPath.segmentCount() - createdFolderPathPart.segmentCount()) + 1;
				foldToDelPath = relPath.uptoSegment(segNb);
			}

			if (foldToDelPath.segmentCount() > 0) {
				_targetParentContainer.getFolder(foldToDelPath).delete(true, false, View.getDefaultMonitor());
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// TODO finish it, I don't remember why I added this TODO note
		clearLastCreatedFolderNames();
	}

	/**
	 * Return true if and only if the specified folder contains only folders
	 * (recursivly).
	 * 
	 * @param folder
	 *            a folder to test
	 * @return true if and only if the specified folder contains only folders
	 *         (recursivly).
	 * @throws CoreException
	 *             if we can't list members of the specified folder.
	 */
	private boolean containsOnlyFolders(IFolder folder) throws CoreException {

		for (IResource resource : folder.members(false)) {
			if (!FileUtil.isFolder(resource)) {
				return false;
			}

			if (!containsOnlyFolders((IFolder) resource)) {
				return false;
			}
		}

		return true;
	}

	private void clearLastCreatedFolderNames() {

		// save that no folder have been created
		Properties repoProps = loadRepoProperties();
		repoProps.setProperty(LAST_CREATED_FOLDER_PATH_PART_PROP_NAME, "");
		saveRepositoryProperties(repoProps);
	}

}
