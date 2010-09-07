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
 *
 * Copyright (C) 2006-2010 Adele Team/LIG/Grenoble University, France
 */
package fede.workspace.eclipse.composition.copy.composer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import fede.workspace.eclipse.composition.copy.exporter.IPathable;
import fede.workspace.tool.eclipse.MappingManager;
import fr.imag.adele.cadse.core.Item;
import fr.imag.adele.cadse.core.Link;
import fr.imag.adele.cadse.core.build.IBuildingContext;
import fr.imag.adele.cadse.core.build.IExportedContent;
import fr.imag.adele.fede.workspace.si.view.View;

/**
 * This repository uses one java serialized file for each entry. The datas are
 * saved into the specified folder.
 * 
 * Be carefull, make sure that there is nobody which uses the specified folder !
 * 
 * @author Thomas
 * 
 */
public class FilesystemRepository implements IRepository {

	private static final String	TMP_FILE_EXTENSION		= "tmp";
	private static final String	TMP_FULL_FILE_EXTENSION	= "." + TMP_FILE_EXTENSION;
	private static final String	SER_FILE_EXTENSION		= "ser";
	private static final String	SER_FULL_FILE_EXTENSION	= "." + SER_FILE_EXTENSION;
	private static final String	LINK_INFO_SER			= ".link_info.info-ser";
	private static final String	LINK_INFO_TMP			= ".link_info.info-tmp";

	private IFolder				_repoFolder;

	/**
	 * Create a repository into the specified folder.
	 * 
	 * @param repoFolder
	 *            the folder in which the repository will save datas.
	 */
	public FilesystemRepository(IFolder repoFolder) {
		this._repoFolder = repoFolder;

		if (!_repoFolder.exists()) {
			reset();
		}
	}

	public void close() {
		// do nothing
	}

	public List<IExportedContent> getExistingContents() {
		List<IExportedContent> existingContents = new ArrayList<IExportedContent>();
		for (ITargetContent targetContent : getTargetContents()) {
			if (!targetContent.lastOpIsRemove()) {
				existingContents.add((IExportedContent) targetContent);
			}
		}

		return existingContents;
	}

	public Item getLastModifierItem(IPath relativePath, boolean isFolder) {
		ITargetContent targetContent = getTargetContent(relativePath, isFolder);
		if (targetContent == null) {
			return null;
		}

		if (targetContent.lastOpIsAdd()) {
			return targetContent.addedBy();
		}
		if (targetContent.lastOpIsUpdate()) {
			return targetContent.updatedBy();
		}
		if (targetContent.lastOpIsRemove()) {
			return targetContent.removedBy();
		}

		// should not be reach
		throw new IllegalStateException("Unreachable Code.");
	}

	public List<ITargetContent> getTargetContents() {
		List<ITargetContent> targetContents = new ArrayList<ITargetContent>();
		findTargetContents(_repoFolder, targetContents);
		return targetContents;
	}

	public List<ITargetContent> getTargetContents(Item item) {
		List<ITargetContent> targetContents = new ArrayList<ITargetContent>();
		for (ITargetContent targetContent : getTargetContents()) {
			if (item.equals(targetContent.addedBy()) || item.equals(targetContent.updatedBy())
					|| item.equals(targetContent.removedBy())) {
				targetContents.add(targetContent);
			}
		}

		return targetContents;
	}

	public boolean hasExistingTargetContent() {
		for (ITargetContent targetContent : getTargetContents()) {
			if (!targetContent.lastOpIsRemove()) {
				return true;
			}
		}

		return false;
	}

	public void removeEntriesFor(Item item) {
		remove(getTargetContents(item));
	}

	private void findTargetContents(IFolder folder, List<ITargetContent> targetContents) {
		try {
			IResource[] resources = folder.members(false);
			for (IResource resource : resources) {
				if (resource instanceof IFile) {
					IFile file = (IFile) resource;
					String fileExtension = file.getFileExtension();
					if (fileExtension == null) {
						continue;
					}
					if (!(fileExtension.equalsIgnoreCase(SER_FILE_EXTENSION) || fileExtension
							.equalsIgnoreCase(TMP_FILE_EXTENSION))) {
						continue; // ??? return; // file not managed by this
						// repository, so ignore it
					}

					ITargetContent targetContent = getTargetContent(getTargetPath(_repoFolder, file),
							representsFolder(file));
					if (targetContent != null) {
						targetContents.add(targetContent);
					}
				} else if (resource instanceof IFolder) {
					IFolder subFolder = (IFolder) resource;
					findTargetContents(subFolder, targetContents);
				}
			}
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Return true only if the specified java serialized file represents a
	 * folder.
	 * 
	 * @param file
	 *            a serialized file
	 * @return true only if the specified java serialized file represents a
	 *         folder.
	 */
	private static boolean representsFolder(IFile file) {
		IPath filePath = file.getFullPath();

		return filePath.lastSegment().equalsIgnoreCase(SER_FULL_FILE_EXTENSION)
				|| filePath.lastSegment().equalsIgnoreCase(TMP_FULL_FILE_EXTENSION);
	}

	/**
	 * Return the relative path between the specified folder and the specified
	 * file path with .ser or .tmp suffix removed.
	 * 
	 * @param folder
	 *            a folder
	 * @param file
	 *            a file contained into the specified folder
	 * @return the relative path between the specified folder and the specified
	 *         file path with .ser or .tmp suffix removed.
	 */
	private IPath getTargetPath(IFolder folder, IFile file) {
		IPath relPath = PathUtil.getRelativePath(folder, file);

		return removeSerSuffix(relPath);
	}

	/**
	 * Return the specified path without the suffix used by serialized files.
	 * 
	 * @param path
	 *            a path which represents a serialized file
	 * @return the specified path without the suffix used by serialized files.
	 */
	private IPath removeSerSuffix(IPath path) {
		String fileName = path.lastSegment();
		for (String suffix : new String[] { SER_FULL_FILE_EXTENSION, TMP_FULL_FILE_EXTENSION }) {
			int fileNameLength = fileName.length();
			if (fileNameLength < suffix.length()) {
				continue;
			}

			int suffixIdx = fileName.length() - suffix.length();
			String endStr = fileName.substring(suffixIdx, fileName.length());
			if (endStr.equalsIgnoreCase(suffix)) {
				return path.removeLastSegments(1).append(fileName.substring(0, suffixIdx));
			}
		}

		return path;
	}

	public void reset() {
		this.links = null;
		try {
			if (_repoFolder.exists()) {
				_repoFolder.delete(true, true, View.getDefaultMonitor());
			}
			MappingManager.createFolder(_repoFolder, View.getDefaultMonitor());
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public ITargetContent getTargetContent(IPath relPath, boolean isFolder) {
		IFile serFile = _repoFolder.getFile(getSerFilePath(relPath, SER_FULL_FILE_EXTENSION, isFolder));

		// load the backup if the file does not exist
		if (!serFile.exists()) {
			serFile = _repoFolder.getFile(getSerFilePath(relPath, TMP_FULL_FILE_EXTENSION, isFolder));
		}
		try {
			FileInputStream fis = new FileInputStream(getFileFor(serFile));
			ObjectInputStream ois = new ObjectInputStream(fis);
			ITargetContent targetContent = (ITargetContent) ois.readObject();
			targetContent.setModel(fr.imag.adele.cadse.core.impl.CadseCore.getLogicalWorkspace());
			return targetContent;
		} catch (FileNotFoundException e) {
			// do nothing, not an error if target content not exists in the
			// repository
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (java.lang.ClassCastException e) {
			if (serFile != null) {
				try {
					serFile.delete(true, View.getDefaultMonitor());
				} catch (CoreException ignored) {
				}
			}
			e.printStackTrace();
		}

		return null;
	}

	private void saveTargetContent(ITargetContent targetContent) {
		IPath targetPath = ((IPathable) targetContent).getPath();
		IFile serFile = _repoFolder
				.getFile(getSerFilePath(targetPath, SER_FULL_FILE_EXTENSION, isFolder(targetContent)));
		IFile serTmpFile = _repoFolder.getFile(getSerFilePath(targetPath, TMP_FULL_FILE_EXTENSION,
				isFolder(targetContent)));
		try {
			// save a copy
			if (serFile.exists()) {
				serFile.copy(serTmpFile.getFullPath(), true, View.getDefaultMonitor());
			}

			// save the target content
			MappingManager.createEmptyFile(serFile, View.getDefaultMonitor());
			FileOutputStream fos = new FileOutputStream(getFileFor(serFile));
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(targetContent);
			oos.close();

			// delete backup
			serTmpFile.delete(true, View.getDefaultMonitor());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	static private IPath getSerFilePath(IPath targetPath, String suffix, boolean isFolder) {
		// test if it represents a folder
		if (isFolder) {
			return targetPath.append(suffix);
		} else {
			String fileName = targetPath.lastSegment() + suffix;
			return targetPath.removeLastSegments(1).append(fileName);
		}
	}

	static private File getFileFor(IFile iFile) {
		return iFile.getLocation().makeAbsolute().toFile();
	}

	public void add(ITargetContent targetContent) {
		saveTargetContent(targetContent);
	}

	public void add(List<ITargetContent> targetContents) {
		for (ITargetContent targetContent : targetContents) {
			add(targetContent);
		}
	}

	public void remove(ITargetContent targetContent) {
		IPath targetPath = ((IPathable) targetContent).getPath();
		IFile serFile = _repoFolder
				.getFile(getSerFilePath(targetPath, SER_FULL_FILE_EXTENSION, isFolder(targetContent)));
		IFile serTmpFile = _repoFolder.getFile(getSerFilePath(targetPath, TMP_FULL_FILE_EXTENSION,
				isFolder(targetContent)));
		try {
			if (serFile.exists()) {
				serFile.delete(true, View.getDefaultMonitor());
			}

			// delete backup
			if (serTmpFile.exists()) {
				serTmpFile.delete(true, View.getDefaultMonitor());
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	static private boolean isFolder(ITargetContent targetContent) {
		return (targetContent instanceof FolderTargetContent);
	}

	public void remove(List<ITargetContent> targetContents) {
		for (ITargetContent targetContent : targetContents) {
			remove(targetContent);
		}
	}

	HashSet<String>	links		= null;
	HashSet<String>	copylinks	= null;

	String toStringKey(Link l) {
		return l.getSource().getId().toString() + l.getLinkType().getName()
				+ l.getDestination().getId().toString();
	}

	public boolean contains(Link l) {
		loadLinksInfo();
		return links.contains(toStringKey(l));
	}

	public void beginSaveLinks() {
		copylinks = new HashSet<String>();
	}

	public void endSaveLinks() {
		links = copylinks;
		copylinks = null;
		saveLinksInfo();
	}

	public void saveLink(Link link) {
		copylinks.add(toStringKey(link));
	}

	private void loadLinksInfo() {
		if (links != null) {
			return;
		}

		IFile serFile = _repoFolder.getFile(LINK_INFO_SER);

		// load the backup if the file does not exist
		if (!serFile.exists()) {
			serFile = _repoFolder.getFile(LINK_INFO_TMP);
		}
		try {
			FileInputStream fis = new FileInputStream(getFileFor(serFile));
			ObjectInputStream ois = new ObjectInputStream(fis);
			HashSet<String> readInfo = (HashSet<String>) ois.readObject();
			links = readInfo;
			if (links != null) {
				return;
				// create an empyt set
			}
		} catch (FileNotFoundException e) {
			// do nothing, not an error if target content not exists in the
			// repository
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		links = new HashSet<String>();
	}

	private void saveLinksInfo() {
		IFile serFile = _repoFolder.getFile(LINK_INFO_SER);
		IFile serTmpFile = _repoFolder.getFile(LINK_INFO_TMP);
		try {
			// save a copy
			if (serFile.exists()) {
				serFile.copy(serTmpFile.getFullPath(), true, View.getDefaultMonitor());
			}

			// save the target content
			MappingManager.createEmptyFile(serFile, View.getDefaultMonitor());
			FileOutputStream fos = new FileOutputStream(getFileFor(serFile));
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(links);
			oos.close();

			// delete backup
			serTmpFile.delete(true, View.getDefaultMonitor());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void clean(IBuildingContext context) throws CoreException {
		IFile serFile = _repoFolder.getFile(LINK_INFO_SER);
		if (serFile.exists()) {
			serFile.delete(true, View.getDefaultMonitor());
		}
		if (_repoFolder.exists()) {
			_repoFolder.delete(true, false, View.getDefaultMonitor());
		}
	}
}
