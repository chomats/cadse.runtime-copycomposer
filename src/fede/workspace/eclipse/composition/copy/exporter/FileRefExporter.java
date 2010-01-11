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
package fede.workspace.eclipse.composition.copy.exporter;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import fede.workspace.eclipse.composition.copy.composer.FileUtil;
import fede.workspace.tool.eclipse.MappingManager;
import fr.imag.adele.cadse.core.content.ContentItem;
import fr.imag.adele.cadse.core.build.IExportedContent;
import fr.imag.adele.cadse.core.build.IExporterTarget;
import fr.imag.adele.cadse.core.var.ContextVariable;
import fr.imag.adele.cadse.core.var.ContextVariableImpl;
import fr.imag.adele.cadse.core.var.Variable;
import fr.imag.adele.fede.workspace.si.view.View;

/**
 * This exporter exposes all the files contained a specified folder to the
 * composers.
 * 
 * @author Thomas
 * 
 */
public class FileRefExporter extends ProjectExporter {
	static public class RefFiles {
		
	}
	private static final String	REPO_FILE_NAME					= "repo.properties";
	private static final String	LAST_EXPORTED_FOLDER_PROP_NAME	= "lastExportedFolder";

	/**
	 * Exporter type which represents references of files and folders.
	 */
	public final static Class	FILE_REF_EXPORTER_TYPE			= RefFiles.class;
	private Variable			path;
	private Variable			pattern;
	private Matcher				matcher;

	/**
	 * Create a FileRefExporter.
	 * 
	 * @param cm
	 *            the related content manager
	 */
	public FileRefExporter(ContentItem cm, Variable path, Variable pattern, Class... exporterTypes) {
		super(cm, exporterTypes);
		this.path = path;
		this.pattern = pattern;
	}

	/**
	 * Create a FileRefExporter.
	 * 
	 * @param cm
	 *            the related content manager
	 */
	public FileRefExporter(ContentItem cm, Variable path, Variable pattern) {
		super(cm, FILE_REF_EXPORTER_TYPE);
		this.path = path;
		this.pattern = pattern;
	}

	/**
	 * Create a FileRefExporter.
	 * 
	 * @param cm
	 *            the related content manager
	 */
	public FileRefExporter(ContentItem cm) {
		super(cm, FILE_REF_EXPORTER_TYPE);
		this.path = null;
	}

	/**
	 * Exposes all files included in the specified directory that have been
	 * added, removed or updated since the last build.
	 * 
	 * Scans the specifed directory of the project related to the component and
	 * exposes all modified files to the composer.
	 * 
	 * If no resource delta is specified all the binary contents are exposed to
	 * the to the composers.
	 * 
	 * @param componentProject
	 *            the project which contain the component content.
	 * @param projectDelta
	 *            resource delta of componentProject since the last build
	 * @param monitor
	 *            the monitor of this build process
	 * @param exporterType
	 *            the exporter type of files and directory to export
	 * @throws CoreException
	 *             In case of error which cancel the build process
	 */
	@Override
	protected IExportedContent exportItem(IProject componentProject, IResourceDelta projectDelta,
			IProgressMonitor monitor, Class exporterType, IExporterTarget target, boolean fullExport)
			throws CoreException {

		/*
		 * Verify this item is actually hosted in a Project
		 */
		if (componentProject == null) {
			throw new IllegalArgumentException("componentProject does not exist.");
		}

		if (pattern != null) {
			Pattern p = Pattern.compile(pattern.compute(ContextVariableImpl.DEFAULT, getItem()));
			matcher = p.matcher("");
		} else {
			matcher = null;
		}
		// if (!(target instanceof ICompositeDescExporterTarget))
		// throw new IllegalArgumentException("target must be a
		// ICompositeDescExporterTarget.");;

		// It is a workaround because exporter are not able to know by which
		// link they export their content.
		// Item composite = ((ICompositeDescExporterTarget)
		// target).getCompositeItem();

		FolderExportedContent folderContent = new FolderExportedContent(getItem(), exporterType);

		IContainer exportedFolder = getExportedFolder();
		IFolder lastExportedFolder = getLastExportedFolder(componentProject, exporterType);
		if ((lastExportedFolder != null) && (!exportedFolder.equals(lastExportedFolder))) {

			/*
			 * Notify exposition removal of all files and folders contained in
			 * the old exported folder.
			 */

			// We scan the old directory and flag each of its file and directory
			// as removed.
			FolderExportedContent oldFolderContent = new FolderExportedContent(getItem(), exporterType);
			Scanner scanner = new Scanner(oldFolderContent);
			scanner.scanOlderFolder(lastExportedFolder, monitor);

			// We scan the old directory delta and add all removal that have
			// been performed.
			FolderExportedContent oldFolderContentDiff = new FolderExportedContent(getItem(), exporterType);
			scanner = new Scanner(oldFolderContentDiff);
			IResourceDelta oldFolderDelta = (projectDelta != null) ? projectDelta.findMember(lastExportedFolder
					.getProjectRelativePath()) : null;
			scanner.scanOlderFolder(lastExportedFolder, oldFolderDelta, monitor);

			// Merge removal of old exported folder content with its delta scan
			// result
			FolderMergeUtil.merge(oldFolderContent, oldFolderContentDiff);

			// We scan the new directory and flag each of its file and directory
			// as added.
			scanner = new Scanner(folderContent);
			scanner.scanNewFolder(exportedFolder, monitor);

			// Merge old exported folder diff and new exported folder
			// differencies
			FolderMergeUtil.merge(folderContent, oldFolderContent);

			// Update repository datas
			setLastExportedFolder(getExportedFolder(), componentProject, exporterType);

		} else {

			/*
			 * We scan the specified directory.
			 */
			Scanner scanner = new Scanner(folderContent);
			IResourceDelta outputDelta = (projectDelta != null) ? projectDelta.findMember(exportedFolder
					.getProjectRelativePath()) : null;

			// If no modification of the output location just skip it
			if (!fullExport && ((projectDelta != null) && (outputDelta == null))) {
				return folderContent;
			}

			scanner.scan(exportedFolder, outputDelta, monitor);
		}

		return folderContent;
	}

	/**
	 * Store the specified folder as the last one which has been exported by
	 * this exporter.
	 * 
	 * @param exportedFolder
	 *            the folder to expose to the composers.
	 * @param componentProject
	 *            the project which contain the component content.
	 * @param exporterType
	 *            the exporter type of files and directory to export.
	 * @throws CoreException
	 *             if an error happens during loading of the exporter
	 *             repository.
	 */
	private void setLastExportedFolder(IContainer exportedFolder, IProject componentProject, Class exporterType)
			throws CoreException {

		IFolder repoFolder = getRepoFolder(componentProject, exporterType);

		IFile repoFile = repoFolder.getFile(REPO_FILE_NAME);

		PropertyFile propFile = new PropertyFile(repoFile);
		Properties repoProps = propFile.loadProperties();
		repoProps.setProperty(LAST_EXPORTED_FOLDER_PROP_NAME, getExportedFolder().getFullPath().toPortableString());

		propFile.saveProperties(repoProps, "Used by FileRefExporter.");
	}

	/**
	 * Return the last folder (from the last build) which has been exposed to
	 * the composers. If there is no previous build, return null.
	 * 
	 * @param componentProject
	 *            the project which contain the component content.
	 * @param exporterType
	 *            the exporter type of files and directory to export.
	 * @return the last folder (from the last build) which has been exposed to
	 *         the composers.
	 * @throws CoreException
	 *             if an error happens during loading of the exporter
	 *             repository.
	 */
	public IFolder getLastExportedFolder(IProject componentProject, Class exporterType) throws CoreException {

		IFolder repoFolder = getRepoFolder(componentProject, exporterType);

		IFile repoFile = repoFolder.getFile(REPO_FILE_NAME);
		if (!repoFile.exists()) {
			return null;
		}

		Properties repoProps = loadRepoProperties(repoFile);
		String lastExportedFoldPath = repoProps.getProperty(LAST_EXPORTED_FOLDER_PROP_NAME);
		if (lastExportedFoldPath != null) {
			return componentProject.getFolder(new Path(lastExportedFoldPath));
		}

		return null;
	}

	/**
	 * Return all the properties saved in the exporter repository specified
	 * file.
	 * 
	 * @param repoFile
	 * @return all the properties saved in the exporter repository specified
	 *         file.
	 */
	private Properties loadRepoProperties(IFile repoFile) {
		PropertyFile propFile = new PropertyFile(repoFile);

		return propFile.loadProperties();
	}

	/**
	 * Return the folder which contain the exporter repository datas. This
	 * folder is created if it does not already exist.
	 * 
	 * @return the folder which contain the exporter repository datas.
	 */
	private IFolder getRepoFolder(IProject componentProject, Class exporterType) throws CoreException {
		IFolder repoFolder = componentProject.getFolder("." + FileRefExporter.class.getName()).getFolder(
				getItem().getId().toString()).getFolder(exporterType.getSimpleName());

		if (!repoFolder.exists()) {
			MappingManager.createFolder(repoFolder, View.getDefaultMonitor());
		}

		return repoFolder;
	}

	/**
	 * Return the folder which is exposed to the composers.
	 * 
	 * @return the folder which is exposed to the composers.
	 */
	protected IContainer getExportedFolder() {
		IContainer container = getItem().getMainMappingContent(IContainer.class);
		if (container == null) {
			return null;
		}
		if (path != null) {
			String value = path.compute(ContextVariableImpl.DEFAULT, getItem());
			if (value == null) {
				value = "";
			}
			Path path = new Path(value);
			if (!path.isEmpty()) {
				return container.getFolder(path);
			} else {
				return container;
			}
		}
		return container;
	}

	/**
	 * This class is a visitor that scans a exported folder.
	 * 
	 * @author thomas
	 * 
	 */
	private class Scanner implements IResourceVisitor, IResourceDeltaVisitor {

		private final FolderExportedContent	_exportedContent;

		private IContainer					_scannedFolder;
		private IProgressMonitor			_monitor;

		public Scanner(FolderExportedContent exportedContentList) {
			this._exportedContent = exportedContentList;
		}

		/**
		 * Scan a new exported folder and tag its files and directories as
		 * added.
		 * 
		 * @param exportedFolder
		 *            a new exported folder
		 * @param monitor
		 *            a builder monitor
		 * @throws CoreException
		 */
		public void scanNewFolder(IContainer exportedFolder, IProgressMonitor monitor) throws CoreException {
			this._scannedFolder = exportedFolder;
			this._monitor = monitor;

			_scannedFolder.accept(new IResourceVisitor() {

				/*
				 * We flag as added all files and directories contained in the
				 * new exported folder.
				 * 
				 * @see org.eclipse.core.resources.IResourceVisitor#visit(org.eclipse.core.resources.IResource)
				 */
				public boolean visit(IResource resource) throws CoreException {
					return added(resource);
				}
			});
		}

		public void scanOlderFolder(IFolder lastExportedFolder, IResourceDelta oldFolderDelta, IProgressMonitor monitor)
				throws CoreException {
			this._scannedFolder = lastExportedFolder;
			this._monitor = monitor;

			oldFolderDelta.accept(new IResourceDeltaVisitor() {

				/*
				 * We flag as removed all files and directories contained in the
				 * older exported folder.
				 * 
				 * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.core.resources.IResourceDelta)
				 */
				public boolean visit(IResourceDelta resourceDelta) throws CoreException {
					if (resourceDelta == null) {
						return false;
					}

					switch (resourceDelta.getKind()) {
						case IResourceDelta.ADDED: {
							return false;
						}
						case IResourceDelta.REMOVED: {
							return removed(resourceDelta.getResource());
						}
						case IResourceDelta.CHANGED: {
							return true;
						}
					}

					return true;
				}
			});
		}

		/**
		 * Scan an older exported folder.
		 * 
		 * @param lastExportedFolder
		 * @param monitor
		 * @throws CoreException
		 */
		public void scanOlderFolder(IFolder lastExportedFolder, IProgressMonitor monitor) throws CoreException {
			this._scannedFolder = lastExportedFolder;
			this._monitor = monitor;

			_scannedFolder.accept(new IResourceVisitor() {

				/*
				 * We flag as removed all files and directories contained in the
				 * older exported folder.
				 * 
				 * @see org.eclipse.core.resources.IResourceVisitor#visit(org.eclipse.core.resources.IResource)
				 */
				public boolean visit(IResource resource) throws CoreException {
					return removed(resource);
				}
			});
		}

		/**
		 * This methods iterates over all modifications of the output folder
		 * aplying them to the packaged item
		 * 
		 */
		public synchronized void scan(IContainer outputFolder, IResourceDelta outputDelta, IProgressMonitor monitor)
				throws CoreException {
			this._scannedFolder = outputFolder;
			this._monitor = monitor;

			if (outputDelta != null) {
				outputDelta.accept(this);
			} else {
				outputFolder.accept(this);
			}
		}

		/**
		 * This callback method is called to visit and filter all the content of
		 * the output folder in the case of full copies. We consider any
		 * matching file found as an addition.
		 */
		public boolean visit(IResource outputResource) throws CoreException {
			return added(outputResource);
		}

		/**
		 * This callback method is called to incrementally perform an update
		 * from the contents of the output folder
		 */
		public boolean visit(IResourceDelta outputDelta) throws CoreException {

			switch (outputDelta.getKind()) {
				case IResourceDelta.ADDED: {
					return added(outputDelta.getResource());
				}
				case IResourceDelta.REMOVED: {
					return removed(outputDelta.getResource());
				}
				case IResourceDelta.CHANGED: {
					return changed(outputDelta.getResource());
				}
			}

			return true;
		}

		private boolean removed(IResource outputResource) throws CoreException {
			IPath filePath = getRelativePath(_scannedFolder, outputResource);
			if (filePath == null) {
				return false;
			}
			if (filePath.segmentCount() == 0) {
				return true;
			}
			if (!accept(filePath, outputResource)) {
				return false;
			}
			_exportedContent.delete(outputResource, filePath, _monitor);
			return FileUtil.isFolder(outputResource);
		}

		private boolean added(IResource outputResource) throws CoreException {
			IPath filePath = getRelativePath(_scannedFolder, outputResource);
			if (filePath == null) {
				return false;
			}
			if (filePath.segmentCount() == 0) {
				return true;
			}
			if (!accept(filePath, outputResource)) {
				return false;
			}

			_exportedContent.add(outputResource, filePath, _monitor);
			return FileUtil.isFolder(outputResource);
		}

		private boolean changed(IResource outputResource) throws CoreException {
			IPath filePath = getRelativePath(_scannedFolder, outputResource);
			if (filePath == null) {
				return false;
			}
			if (filePath.segmentCount() == 0) {
				return true;
			}
			if (!accept(filePath, outputResource)) {
				return false;
			}

			_exportedContent.update(outputResource, filePath, _monitor);
			return FileUtil.isFolder(outputResource);
		}

	}

	protected boolean accept(IPath filePath, IResource outputResource) {
		if (matcher == null) {
			return true;
		}
		String path = filePath.toPortableString();
		matcher.reset(path);
		return matcher.matches();
	}

	/**
	 * Gets the path of a resource relative to another resource, both resources
	 * must be located in the project associated with this composer.
	 * 
	 * @param path
	 * @return null if not a prefix
	 * @throws CoreException
	 */
	protected final IPath getRelativePath(IResource container, IResource member) throws CoreException {
		IPath containerPath = container.getFullPath();
		IPath memberPath = member.getFullPath();

		if (containerPath.isPrefixOf(memberPath)) {
			return memberPath.removeFirstSegments(containerPath.segmentCount());
		} else {
			return null;
		}
	}

}
