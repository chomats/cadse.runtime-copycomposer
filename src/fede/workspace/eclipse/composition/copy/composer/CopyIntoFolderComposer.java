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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import fede.workspace.eclipse.composition.copy.exporter.FileExportedContent;
import fede.workspace.eclipse.composition.copy.exporter.FolderExportedContent;
import fede.workspace.eclipse.composition.copy.exporter.FolderMergeUtil;
import fede.workspace.eclipse.composition.copy.exporter.IDeltaSetter;
import fede.workspace.eclipse.composition.copy.exporter.IPathable;
import fede.workspace.tool.eclipse.MappingManager;
import fr.imag.adele.cadse.core.CadseException;
import fr.imag.adele.cadse.core.content.ContentItem;
import fr.imag.adele.cadse.core.Item;
import fr.imag.adele.cadse.core.Link;
import fr.imag.adele.cadse.core.build.Composer;
import fr.imag.adele.cadse.core.build.IBuildingContext;
import fr.imag.adele.cadse.core.build.IExportedContent;
import fr.imag.adele.cadse.core.build.IExporterTarget;
import fr.imag.adele.fede.workspace.si.view.View;

/**
 * This composer uses the following behaviour: It copy all files and folders -
 * which are exposed by the components to the specified folder. and - which are
 * flagged as one of the exported types managed by this composer.
 * 
 * @author Thomas
 * 
 */
public abstract class CopyIntoFolderComposer extends Composer {

	/**
	 * Inner class to represent a garbage collection job.
	 * 
	 * @author Thomas
	 * 
	 */
	private abstract class GarbageCollectJob {

		/**
		 * Delete the specified target content and the related entry in the
		 * specified repository.
		 * 
		 * @param content
		 *            target content to delete
		 * @param repository
		 *            a repository which references the target content
		 * @throws MelusineException
		 */
		public abstract void delete(ITargetContent content, IRepository repository) throws CadseException;
	}

	private FolderExporterTarget	_currentBuildTarget;

	private final String			_name;

	/**
	 * Create a Copy composer which will copy all exposed files and folders
	 * which are flagged of one of the specified exported types.
	 * 
	 * @param name
	 *            the composer name (used to distinguish the different composers
	 *            associated to a composite item).
	 * @param contentManager
	 *            the manager of the item content.
	 * @param exporterTypes
	 *            the exporter types managed by this composer.
	 */
	public CopyIntoFolderComposer(ContentItem contentManager, String name, String... exporterTypes) {
		super(contentManager, exporterTypes);
		this._name = name;
	}

	@Override
	protected boolean getFullExport(Link l, String exporterType) {
		try {
			return !((FolderExporterTarget) getCurrentTarget()).getRepository(exporterType).contains(l);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return super.getFullExport(l, exporterType);
	}

	@Override
	protected FolderExporterTarget getTarget() {
		try {
			return new FolderExporterTarget(getItem(), getTargetPath(), CopyIntoFolderComposer.class.getName(), _name);
		} catch (CoreException e) {
			e.printStackTrace();
		}

		return null;
	}

	protected FolderExporterTarget getTarget(IPath target) {
		try {
			return new FolderExporterTarget(getItem(), target, CopyIntoFolderComposer.class.getName(), _name);
		} catch (CoreException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Return target folder relative path to project related to the composite
	 * item.
	 * 
	 * @return target folder relative path to project related to the composite
	 *         item.
	 */
	public abstract IPath getTargetPath();

	/**
	 * Return the old target folder (used by the last build).
	 * 
	 * @return the old target folder (used by the last build).
	 */
	private IContainer getLastTargetFolder() {
		try {
			return _currentBuildTarget.getLastTargetFolder();
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	@Override
	protected void garbageCollect(IBuildingContext context, String exporterType, IExporterTarget target) {
		// do nothing ; garbage collection is done at the end of the composition
	}

	@Override
	public void clean(final IBuildingContext context) {
		this.currentTarget = _currentBuildTarget = getTarget();
		garbageCollect(context, new GarbageCollectJob() {
			@Override
			public void delete(ITargetContent content, IRepository repository) throws CadseException {
				if (content.addedBy() != null) {
					// content added by this composer, so remove it
					content.deleteTargetContent(context, _currentBuildTarget.getTargetFolder(), repository);
					repository.remove(content);
				}
			}
		});
		for (String exporterType : getExporterTypes()) {
			try {
				IRepository repository = _currentBuildTarget.getRepository(exporterType);
				repository.clean(context);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		this.currentTarget = _currentBuildTarget = null;
	}

	@Override
	protected void preComposer(IBuildingContext context, IExporterTarget target) {
		// keep a reference on the target to be sure to have the same target
		// during all the build process
		this._currentBuildTarget = (FolderExporterTarget) target;
	}

	@Override
	protected void postCompose(IBuildingContext context, List<IExportedContent> listExportedContent,
			IExporterTarget target) {

		if (listExportedContent == null) {
			throw new IllegalArgumentException("listExportedContent argument must be not null.");
		}

		/**
		 * Merge all graph of exported contents
		 */

		// Check Validity of exported contents
		for (IExportedContent exportedContent : listExportedContent) {
			if (!(exportedContent instanceof FolderExportedContent)) {
				throw new IllegalArgumentException(
						"Exported contents contained in listExportedContent must be of type FolderExportedContent.");
			}
		}

		// save links
		try {
			for (String t : this.getExporterTypes()) {
				IRepository repository = _currentBuildTarget.getRepository(t);
				repository.beginSaveLinks();
				for (IExportedContent exportedContent : listExportedContent) {
					FolderExportedContent content = (FolderExportedContent) exportedContent;
					if (!(content.getExporterType().equals(t))) {
						continue;
					}
					repository.saveLink(content.getLink());
				}
				repository.endSaveLinks();
			}
		} catch (CoreException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		HashMap<String, FolderExportedContent> targetFolders = new HashMap<String, FolderExportedContent>();

		// merge differents trees
		if (listExportedContent.size() > 0) {
			for (IExportedContent ec : listExportedContent) {
				FolderExportedContent fec = (FolderExportedContent) ec;
				if (fec.isTargetFolder()) {
					for (IExportedContent childEc : fec.getChildren()) {
						FolderExportedContent childFec = (FolderExportedContent) childEc;
						String targetFolder = childFec.getTargetFolder();
						merge(targetFolders, childFec, targetFolder);
					}
					continue;
				}
				String targetFolder = fec.getTargetFolder();
				if (targetFolder == null) {
					targetFolder = FolderExportedContent.DEFAULT_TARGET;
				}
				merge(targetFolders, fec, targetFolder);
			}
		}

		/**
		 * Manage target folder changes between builds
		 */
		if (_currentBuildTarget.targetFolderChanged()) {
			moveOldTargetFolderContent(getLastTargetFolder(), _currentBuildTarget.getTargetFolder());
		} else {
			_currentBuildTarget.saveTargetFolderRef();
		}

		/**
		 * Effective Copy
		 */
		// Add all exported content that they are not present in target + update
		// + delete
		for (Map.Entry<String, FolderExportedContent> e : targetFolders.entrySet()) {
			String targetFolder = e.getKey();
			FolderExportedContent fec = e.getValue();
			for (IExportedContent exportedContent : fec.getChildren()) {
				if (exportedContent instanceof IDeltaSetter) {
					IDeltaSetter deltaContent = (IDeltaSetter) exportedContent;
					try {
						IRepository repository = null;
						// if (exportedContent instanceof FolderExportedContent
						// &&
						// ((FolderExportedContent)exportedContent).getTargetFolder()
						// != null) {
						// repository =
						// _currentBuildTarget.getRepository(deltaContent.getExporterType()+((FolderExportedContent)exportedContent).getTargetFolder());
						// }

						repository = _currentBuildTarget.getRepository(deltaContent.getExporterType());

						performAction(repository, deltaContent, targetFolder);
					} catch (CoreException execpt) {
						// TODO Auto-generated catch block
						execpt.printStackTrace();
					}
				}
			}
		}

		// Garbage collection of non existing component item contents in target
		// content
		postGarbageCollect(context);
		_currentBuildTarget = null;
	}

	private void merge(HashMap<String, FolderExportedContent> targetFolders, FolderExportedContent fec,
			String targetFolder) {
		FolderExportedContent current = targetFolders.get(targetFolder);
		if (current != null) {
			FolderMergeUtil.merge(current, fec);
		} else {
			targetFolders.put(targetFolder, fec);
		}
	}

	private void moveOldTargetFolderContent(IContainer oldFolder, IContainer newFolder) {

		if (!_currentBuildTarget.targetFolderChangeProcessPhase1Finished()) {
			performTargetChangeProcessPhase1();
		}

		// Copy all files of temporary directory into the new target folder
		for (String exporterType : getExporterTypes()) {
			try {
				Scanner tempTargetScannner = new Scanner(_currentBuildTarget.getRepository(exporterType));
				tempTargetScannner.scan(_currentBuildTarget.getTemporaryFolder(),
						_currentBuildTarget.getTargetFolder(), true);
			} catch (CoreException e) {
				// TODO should manage it
				e.printStackTrace();
			}
		}

		// Delete temporary directory
		_currentBuildTarget.deleteTemporaryFolder();

		// Update Repository properties to tell that the move has been completly
		// performed
		_currentBuildTarget.finishTargetChangeProcess();
	}

	private void performTargetChangeProcessPhase1() {
		_currentBuildTarget.startTargetChangeProcess();

		// Copy files and folders which have been added or modified by this
		// composer in a temporary directory
		IFolder tempFold = _currentBuildTarget.createTemporaryFolder();
		for (String exporterType : getExporterTypes()) {
			try {
				Scanner oldTargetScannner = new Scanner(_currentBuildTarget.getRepository(exporterType));
				oldTargetScannner.scan(_currentBuildTarget.getLastTargetFolder(), tempFold, false);
			} catch (CoreException e) {
				// TODO should manage it
				e.printStackTrace();
			}
		}

		// Delete files and folders in old target directory which have been
		// added by this composer
		try {
			Set<IRepository> repositories = new HashSet<IRepository>();
			for (String exporterType : getExporterTypes()) {
				repositories.add(_currentBuildTarget.getRepository(exporterType));
			}

			MultiRepositoryScanner oldTargetScannner = new MultiRepositoryScanner(repositories);

			/*
			 * We must scan the folder with only one pass to be sure that each
			 * file is deleted However, some files can't be deleted due to the
			 * order example : a/b c two exporter types : type1 and type2 a and
			 * a/b are managed by the repository of type1 a/c is managed by the
			 * repository of type2
			 * 
			 * perform deletion for type1 then type2 : a is not deleted if
			 * perform deletion for type2 then type1 : there is no more files
			 */
			oldTargetScannner.scan(_currentBuildTarget.getLastTargetFolder());
		} catch (CoreException e) {
			// TODO should manage it
			e.printStackTrace();
		}

		// Delete old target folder if empty and has been created by this
		// composer
		_currentBuildTarget.deleteOldTargetFolder();

		// Save that we finished first phase
		_currentBuildTarget.finishTargetChangeProcessPhase1();
	}

	/**
	 * This class is a visitor that scans a directory and deleting all files and
	 * folders added by this composer.
	 * 
	 * @author thomas
	 * 
	 */
	private class MultiRepositoryScanner implements IResourceVisitor {

		private IContainer			_folderToScan;

		private IProgressMonitor	_monitor;

		private Set<IRepository>	_repositories;

		public MultiRepositoryScanner(Set<IRepository> repositories) {
			this._monitor = View.getDefaultMonitor();
			this._repositories = repositories;
		}

		/**
		 * This methods iterates over all files and folders of the specified
		 * folder and delete them if they have been added by this composer.
		 * 
		 * @param lastTargetFolder
		 *            the folder in which files and folder must be cleaned
		 */
		public void scan(IContainer lastTargetFolder) throws CoreException {
			this._folderToScan = lastTargetFolder;

			_folderToScan.accept(this);
		}

		/**
		 * This callback method is called to visit and delete all resource which
		 * has been added by this composer (if all its members have the same
		 * property).
		 * 
		 * @param resource
		 *            an eclipse resource
		 */
		public boolean visit(IResource resource) throws CoreException {

			IPath relPath = PathUtil.getRelativePath(_folderToScan, resource);
			boolean isFolder = FileUtil.isFolder(resource);

			if (isManaged(isFolder, relPath)) {
				if (isFolder) {
					// delete if all files and folders
					// have been added by this composer
					if (containOnlyManagedMembers((IFolder) resource)) {
						resource.delete(true, _monitor);
						return false;
					}

					return true;
				} else {
					resource.delete(true, _monitor);

					return false;
				}
			}

			return isFolder;
		}

		/**
		 * Return true if and only if all its members (recursivly) have been
		 * added by this composer (added by setted in the repository).
		 * 
		 * @param folder
		 * @return
		 * @throws CoreException
		 */
		private boolean containOnlyManagedMembers(IFolder folder) throws CoreException {

			for (IResource resource : folder.members(false)) {
				boolean isFolder = FileUtil.isFolder(resource);

				IPath relPath = PathUtil.getRelativePath(_folderToScan, resource);

				if (!isManaged(isFolder, relPath)) {
					return false;
				}

				if (isFolder && (!containOnlyManagedMembers((IFolder) resource))) {
					return false;
				}
			}

			return true;
		}

		/**
		 * Return true if the resource represented by the specified path is
		 * managed by one of the repositories.
		 * 
		 * @param isFolder
		 *            must be true if the represented resource is a folder
		 * @param relPath
		 *            the path of the resource to check
		 * @return true if the resource represented by the specified path is
		 *         managed by one of the repositories.
		 */
		private boolean isManaged(boolean isFolder, IPath relPath) {

			boolean managed = false;
			for (IRepository repository : _repositories) {
				ITargetContent targetContent = repository.getTargetContent(relPath, isFolder);

				if (targetContent == null) {
					continue;
				}

				if (targetContent.addedBy() != null) {
					managed = true;
				}
			}

			return managed;
		}
	}

	/**
	 * This class is a visitor that scans a directory copying all files and
	 * folders added or modified by this composer to a temp directory
	 * 
	 * @author thomas
	 * 
	 */
	private class Scanner implements IResourceVisitor {

		private IContainer			_folderToScan;

		private IContainer			_targetFolder;

		private IProgressMonitor	_monitor;

		private IRepository			_repository;

		// private boolean _updateRepositoryFlag;

		public Scanner(IRepository repository) {
			this._monitor = View.getDefaultMonitor();
			this._repository = repository;
		}

		/**
		 * This methods iterates over all files and folders of the specified
		 * folder and copying (according to their source) them to the target
		 * folder.
		 * 
		 * @param updateRepository
		 *            if true, the repository informations will be updated for
		 *            each new folder created.
		 * @param folderToScan
		 *            the folder to scan
		 * @param targetFolder
		 *            the folder which is used of target for the copy
		 */
		public synchronized void scan(IContainer folderToScan, IContainer targetFolder, boolean updateRepository)
				throws CoreException {
			this._folderToScan = folderToScan;
			this._targetFolder = targetFolder;
			// this._updateRepositoryFlag = updateRepository;

			_folderToScan.accept(this);
		}

		/**
		 * This callback method is called to visit and filter all the content of
		 * the output folder in the case of full copies. We consider any
		 * matching file found as an addition.
		 */
		public boolean visit(IResource resource) throws CoreException {

			IPath relPath = PathUtil.getRelativePath(_folderToScan, resource);
			boolean isFolder = FileUtil.isFolder(resource);
			ITargetContent targetContent = _repository.getTargetContent(relPath, isFolder);
			if (targetContent != null) {
				if (isFolder) {
					IFolder targetFolder = _targetFolder.getFolder(relPath);
					createFolders(relPath, targetFolder);

					return true;
				} else {
					copy((IFile) resource, relPath, targetContent);

					return false;
				}
			}

			return isFolder;
		}

		/**
		 * Create the specified folder and update the repository according to
		 * the created folders.
		 * 
		 * @param relPath
		 *            the path relative to the target folder of the folder to
		 *            create.
		 * @param targetFolder
		 *            the folder to create.
		 * @throws CoreException
		 *             if folder creation failed.
		 */
		private void createFolders(IPath relPath, IFolder targetFolder) throws CoreException {

			// Update repository informations
			for (int i = 1; i <= relPath.segmentCount(); i++) {
				IPath relPathCur = relPath.uptoSegment(i);
				IFolder folderToCheck = _targetFolder.getFolder(relPathCur);
				ITargetContent targetContentToUpdate = _repository.getTargetContent(relPathCur, true);

				Item addedByItem = targetContentToUpdate.addedBy();
				if (folderToCheck.exists()) {
					if (addedByItem != null) {
						targetContentToUpdate.setAddedBy(null);
						if (targetContentToUpdate.lastOpIsAdd()) {
							targetContentToUpdate.setUpdatedBy(addedByItem);
						}
						_repository.add(targetContentToUpdate);
					}
				} else {
					if (addedByItem == null) {
						targetContentToUpdate.setAddedBy(targetContentToUpdate.updatedBy());
						_repository.add(targetContentToUpdate);
					}
				}
			}

			// effective creation of folder
			MappingManager.createFolder(targetFolder, _monitor);
		}

		/**
		 * Copy specified file to the specified target destination path and
		 * update the repository according to file creations and updates.
		 * 
		 * @param file
		 *            file to copy.
		 * @param relativePath
		 *            the path relative to the target folder of the destination
		 *            file.
		 * @param targetContent
		 *            the target content which represents the specified relative
		 *            path.
		 * @throws CoreException
		 *             if file copy failed.
		 */
		private void copy(IFile file, IPath relativePath, ITargetContent targetContent) throws CoreException {

			IPath targetPath = _targetFolder.getFullPath().append(relativePath);
			IFile targetFile = _targetFolder.getFile(relativePath);
			if (targetFile.exists()) {
				// only copy content
				InputStream fileStream = file.getContents(true);
				targetFile.setContents(fileStream, true, true, _monitor);
				try {
					fileStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// Update repository
				Item addedByItem = targetContent.addedBy();
				if (addedByItem != null) {
					targetContent.setAddedBy(null);
					if (targetContent.lastOpIsAdd()) {
						targetContent.setUpdatedBy(addedByItem);
					}
					_repository.add(targetContent);
				}
			} else {
				// full copy
				file.copy(targetPath, true, _monitor);

				// Update repository
				Item addedByItem = targetContent.addedBy();
				if (addedByItem == null) {
					targetContent.setAddedBy(targetContent.updatedBy());
					_repository.add(targetContent);
				}
			}
		}
	}

	/**
	 * Perform action (copy content, update repository...) for the component
	 * exposed content.
	 * 
	 * @param repository
	 *            repository which reference this content
	 * @param deltaContent
	 *            the component exposed content to manage
	 * @param targetFolder
	 * @throws CoreException
	 */
	private void performAction(IRepository repository, IDeltaSetter deltaContent, String targetFolder)
			throws CoreException {
		IPath targetPath = null;
		if (deltaContent instanceof IPathable) {
			IPathable contentPathable = (IPathable) deltaContent;
			targetPath = contentPathable.getPath();
		}

		// Copy content
		IResource targetResource = getResource(_currentBuildTarget.getTargetFolder(targetFolder), targetPath,
				isFolder(deltaContent));
		// TODO manage case of file and folder coexists with same name
		try {
			manageTargetResoure(deltaContent, targetPath, targetResource);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Update repository
		boolean isFolder = (targetResource instanceof IFolder);
		ITargetContent targetContent = repository.getTargetContent(targetPath, isFolder);
		if (targetContent != null) {
			if (targetContent.getTarget() != null && !targetFolder.equals(targetContent.getTarget())) {
				// TODO move target
			}
			updateTargetContent(targetFolder, deltaContent, targetContent);

		} else {
			targetContent = constructTargetContFrom(deltaContent, targetFolder);
		}
		repository.add(targetContent);

		// Must call recursive function only when the container has been created
		if (!deltaContent.hasChildren()) {
			return;
		}

		for (IExportedContent childContent : deltaContent.getChildren()) {
			if (childContent instanceof IDeltaSetter) {
				IDeltaSetter childDeltaContent = (IDeltaSetter) childContent;
				performAction(repository, childDeltaContent, targetFolder);
			}
		}
	}

	/**
	 * Perform copy or remove action for the component exposed content. Action
	 * type depends on the delta content flags.
	 * 
	 * @param deltaContent
	 *            the component exposed content to copy or remove
	 * @param targetRelPath
	 *            relative path used to create the target resource
	 * @param targetResource
	 *            the target resource to manage (create, update or remove)
	 */
	private void manageTargetResoure(IDeltaSetter deltaContent, IPath targetRelPath, IResource targetResource)
			throws CoreException {
		if (deltaContent.isRemoved()) {
			targetResource.delete(true, View.getDefaultMonitor());
			return;
		}

		// Manage update and add cases
		if (isFolder(deltaContent)) {
			MappingManager.createFolder((IFolder) targetResource, View.getDefaultMonitor());
		} else {
			IResource srcResource = ((FileExportedContent) deltaContent).getFile();

			if (targetResource.exists()) {
				targetResource.delete(true, View.getDefaultMonitor());
			}

			IContainer parentContainer = srcResource.getParent();
			if (FileUtil.isFolder(parentContainer)) {
				MappingManager.createFolder(((IFolder) parentContainer), View.getDefaultMonitor());
			}
			if (!targetResource.exists()) {
				IPath dstPath = targetResource.getFullPath();
				srcResource.copy(dstPath, true, View.getDefaultMonitor());
				if (isSetReadOnly()) {
					ResourceAttributes attributes = new ResourceAttributes();
					attributes.setReadOnly(true);
					try {
						targetResource.setResourceAttributes(attributes);
					} catch (CoreException e) {
						// failure is not an option
					}
				}
			} else {
				System.err.println("Cannot delete file " + targetResource.getFullPath());
			}
		}
	}

	protected boolean isSetReadOnly() {
		return false;
	}

	/**
	 * Update the last operation (and the related item source) of the specified
	 * target content.
	 * 
	 * @param targetFolder
	 * 
	 * @param deltaContent
	 *            the component exposed content to copy or remove
	 * @param targetContent
	 *            the target content to update
	 */
	private void updateTargetContent(String targetFolder, IDeltaSetter deltaContent, ITargetContent targetContent) {
		Item item = deltaContent.getItem();
		if (deltaContent.isAdded()) {
			targetContent.setAddedBy(item);
		}
		if (deltaContent.isUpdated()) {
			targetContent.setUpdatedBy(deltaContent.getItem());
		}
		if (deltaContent.isRemoved()) {
			targetContent.setUpdatedBy(deltaContent.getItem());
		}
		targetContent.setTarget(targetFolder);
	}

	private ITargetContent constructTargetContFrom(IDeltaSetter deltaContent, String targetFolder) {
		Item item = deltaContent.getItem();
		String exporterType = deltaContent.getExporterType();
		boolean added = deltaContent.isAdded();
		boolean updated = deltaContent.isUpdated();
		boolean removed = deltaContent.isRemoved();

		if (isFolder(deltaContent)) {
			FolderExportedContent folderContent = (FolderExportedContent) deltaContent;
			return new FolderTargetContent(item, exporterType, folderContent.getPath(), added, updated, removed,
					targetFolder);
		} else {
			FileExportedContent fileContent = (FileExportedContent) deltaContent;
			return new FileTargetContent(fileContent.getPath(), fileContent.getFile(), item, exporterType, added,
					updated, removed, targetFolder);
		}
	}

	private static IResource getResource(IContainer folder, IPath relativePath, boolean isFolder) {
		if (isFolder) {
			return folder.getFolder(relativePath);
		} else {
			return folder.getFile(relativePath);
		}
	}

	private static boolean isFolder(IExportedContent content) {
		return (content instanceof FolderExportedContent);
	}

	private void postGarbageCollect(final IBuildingContext context) {
		garbageCollect(context, new GarbageCollectJob() {
			@Override
			public void delete(ITargetContent content, IRepository repository) throws CadseException {
				Item addedBy = content.addedBy();
				if (addedBy == null) {
					return;
				}

				if (!getItem().containsComponent(addedBy.getId())) {
					content.deleteTargetContent(context, _currentBuildTarget.getTargetFolder(), repository);
					repository.remove(content);
				}
			}
		});
	}

	private void garbageCollect(IBuildingContext context, GarbageCollectJob job) {
		for (String exporterType : getExporterTypes()) {
			try {
				IRepository repository = _currentBuildTarget.getRepository(exporterType);
				List<ITargetContent> repositoryContents = repository.getTargetContents();

				context.beginTask("Cleaning components of item " + getItem().getId(), repositoryContents.size());

				for (ITargetContent content : repositoryContents) {
					String resourceName = getTargetPath().toOSString() + File.separator;
					if (content instanceof FileExportedContent) {
						resourceName += ((FileExportedContent) content).getPath().toOSString();
					}
					if (content instanceof FolderExportedContent) {
						resourceName += ((FolderExportedContent) content).getPath().toOSString();
					}

					context.subTask("verifiying " + resourceName);

					try {
						job.delete(content, repository);
					} catch (CadseException e) {
						context.subTask("cleaning " + resourceName);
						context.report("Error in deteting the resource {0}/{1} : {2}", resourceName, exporterType, e
								.getMessage());
						e.printStackTrace();
					}
					context.worked(1);
				}
			} catch (CoreException e) {
				context.report("Error in get repository of {0} : {1}", exporterType, e.getMessage());
				e.printStackTrace();
				;
			}
			context.endTask();
		}
	}
}
