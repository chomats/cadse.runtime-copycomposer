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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import fr.imag.adele.cadse.core.CadseException;
import fr.imag.adele.cadse.core.CompactUUID;
import fr.imag.adele.cadse.core.Item;
import fr.imag.adele.cadse.core.Link;
import fr.imag.adele.cadse.core.build.IBuildingContext;
import fr.imag.adele.cadse.core.build.IExportedContent;

/**
 * Instance of this class represent a folder.
 * 
 * @author Thomas
 * 
 */
public class FolderExportedContent implements IDeltaSetter, IPathable {
	public static final String							DEFAULT_TARGET	= ".";

	protected transient final List<IExportedContent>	_exportedContentList;
	protected transient final Map<IPath, IPathable>		_folders;

	protected transient Item							_item;

	private String										_exporterType;

	private String										_targetFolder;

	protected transient IPath							_folderPath;

	private boolean										_added;

	private boolean										_updated;

	private boolean										_removed;
	private Link										link;

	protected FolderExportedContent() {
		this._exportedContentList = new ArrayList<IExportedContent>();
		this._folders = new HashMap<IPath, IPathable>();
	}

	/**
	 * Create a FolderExportedContent without any member.
	 * 
	 * @param item
	 *            item associated to the represented file
	 * @param exporterType
	 *            the exporter type related to this file
	 * @param folderPath
	 *            relative path
	 * @param added
	 *            added flag
	 * @param updated
	 *            updated flag
	 * @param removed
	 *            removed flag
	 */
	public FolderExportedContent(Item item, String exporterType, IPath folderPath, boolean added, boolean updated,
			boolean removed) {
		this(item, exporterType, folderPath);

		if (!(added | updated | removed)) {
			throw new IllegalArgumentException("The folder exported content must be added or updated or removed.");
		}
		if ((added & updated) | (added & removed) | (updated & removed)) {
			throw new IllegalArgumentException(
					"Only one of the following flags can be true: added, updated and removed.");
		}

		this._added = added;
		this._removed = removed;
		this._updated = updated;
	}

	/**
	 * Create a FolderExportedContent.
	 * 
	 * @param item
	 *            item associated to the represented file
	 * @param exporterType
	 *            the exporter type related to this file
	 * @param folderPath
	 *            relative path
	 * @param added
	 *            added flag
	 * @param updated
	 *            updated flag
	 * @param removed
	 *            removed flag
	 * @param exportedContents
	 *            the exported contents which represents the members of the
	 *            represented folder
	 */
	public FolderExportedContent(Item item, String exporterType, IPath folderPath, boolean added, boolean updated,
			boolean removed, IExportedContent[] exportedContents) {
		this(item, exporterType, folderPath, added, updated, removed);

		for (IExportedContent exportedContent : exportedContents) {
			addE(exportedContent);
		}
	}

	private void addE(IExportedContent exportedContent) {
		_exportedContentList.add(exportedContent);
		if (exportedContent instanceof IPathable) {
			IPathable ep = (IPathable) exportedContent;
			_folders.put(ep.getPath(), ep);
		}
	}

	/**
	 * Create a FolderExportedContent.
	 * 
	 * @param item
	 *            item associated to the represented file
	 * @param exporterType
	 *            the exporter type related to this file
	 * @param folderPath
	 *            relative path
	 */
	public FolderExportedContent(Item item, String exporterType, IPath folderPath) {
		this(item, exporterType);

		this._folderPath = folderPath;
	}

	/**
	 * Create a FolderExportedContent.
	 * 
	 * @param item
	 *            item associated to the represented file
	 * @param exporterType
	 *            the exporter type related to this file
	 */
	public FolderExportedContent(Item item, String exporterType) {
		this();

		this._item = item;
		this._exporterType = exporterType;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof FolderExportedContent) {
			FolderExportedContent folderContent = (FolderExportedContent) o;
			return _folderPath.equals(folderContent.getPath()) && _item.equals(folderContent.getItem())
					&& _exporterType.equals(folderContent.getExporterType())
					&& equals(_targetFolder, folderContent._targetFolder);
		} else {
			return false;
		}
	}

	private boolean equals(String s1, String s2) {
		if (s1 == null && s2 == null) {
			return true;
		}
		if (s1 != null && s2 != null && s1.equals(s2)) {
			return true;
		}
		return false;
	}

	public int hashcode() {
		return _folderPath.hashCode() + _item.hashCode() + _exporterType.hashCode();
	}

	public void delete(IBuildingContext context) throws CadseException {
		for (IExportedContent content : _exportedContentList) {
			content.delete(context);
		}
	}

	public boolean isAdded() {
		return _added;
	}

	public boolean isUpdated() {
		return _updated;
	}

	public boolean isRemoved() {
		return _removed;
	}

	public IExportedContent[] getChildren() {
		return _exportedContentList.toArray(new IExportedContent[_exportedContentList.size()]);
	}

	public String getExporterType() {
		return _exporterType;
	}

	public Item getItem() {
		return _item;
	}

	public String getItemDisplayName() {
		return _item.getDisplayName();
	}

	public CompactUUID getItemIdentification() {
		return _item.getId();
	}

	public boolean isMulti() {
		return true;
	}

	public IPath getPath() {
		return _folderPath;
	}

	public void delete(IResource outputResource, IPath filePath, IProgressMonitor monitor) {
		constructExportedContent(outputResource, filePath, new Path(""), false, false, true);
	}

	private void constructExportedContent(IResource outputResource, IPath filePath, IPath pathAlreadyConstructed,
			boolean added, boolean updated, boolean removed) {

		if ((filePath.segmentCount() - pathAlreadyConstructed.segmentCount()) == 0) {
			return;
		}

		FolderExportedContent folderContent = null;
		if ((outputResource.getType() == IResource.FILE)
				&& ((filePath.segmentCount() - pathAlreadyConstructed.segmentCount()) == 1)) {
			FileExportedContent fileContent = new FileExportedContent(filePath, (IFile) outputResource, _item,
					_exporterType, added, updated, removed);
			addExportedContent(fileContent);
		} else {
			IPath pathToConstruct = filePath.uptoSegment(pathAlreadyConstructed.segmentCount() + 1);
			folderContent = new FolderExportedContent(_item, _exporterType, pathToConstruct, added, updated, removed);
			addExportedContent(folderContent);
			getFolder(pathToConstruct).constructExportedContent(outputResource, filePath, pathToConstruct, added,
					updated, removed);
		}
	}

	private FolderExportedContent getFolder(IPath pathToConstruct) {
		for (IExportedContent exportedContent : _exportedContentList) {
			if (exportedContent instanceof FolderExportedContent) {
				FolderExportedContent folderContent = (FolderExportedContent) exportedContent;
				if (folderContent.getPath().equals(pathToConstruct)) {
					return folderContent;
				}
			}
		}
		return null;
	}

	private void addExportedContent(IExportedContent exportedContent) {
		int idx = childrenIdxOf(exportedContent);
		if (idx != -1) {
			IExportedContent existingContent = _exportedContentList.get(idx);

			IExportedContent contentToSet = exportedContent;
			if ((exportedContent instanceof IDeltaContent) && (existingContent instanceof IDeltaContent)) {

				IDeltaContent deltaCont = (IDeltaContent) exportedContent;
				IDeltaContent existDeltaCont = (IDeltaContent) exportedContent;
				if (!DeltaContentUtil.sameDeltaFlags(deltaCont, existDeltaCont)) {
					if (DeltaContentUtil.atLeastOneAdd(deltaCont, existDeltaCont)) {
						if (DeltaContentUtil.atLeastOneUpdate(deltaCont, existDeltaCont)) {
							contentToSet = DeltaContentUtil.transformTo(deltaCont, false, true, false);
						} else {
							// at least one is removed
							IDeltaContent cont = DeltaContentUtil.choose(deltaCont, existDeltaCont, true, false, false);
							contentToSet = DeltaContentUtil.transformTo(cont, false, true, false);
						}
					} else if (DeltaContentUtil.atLeastOneUpdate(deltaCont, existDeltaCont)
							&& DeltaContentUtil.atLeastOneRemove(deltaCont, existDeltaCont)) {
						contentToSet = DeltaContentUtil.choose(deltaCont, existDeltaCont, false, true, false);
					}
				} else {
					return; // do nothing
				}
			}

			if (contentToSet != null) {
				_exportedContentList.set(idx, contentToSet);
			} else {
				throw new IllegalStateException("exportedContent must not be null and "
						+ "be instance of FolderExportedContent or FileExportedContent.");
			}
		} else {
			_exportedContentList.add(exportedContent);
		}
	}

	private int childrenIdxOf(IExportedContent exportedContent) {
		int idx = _exportedContentList.indexOf(exportedContent);
		if (idx != -1) {
			return idx;
		}

		for (int i = 0; i < _exportedContentList.size(); i++) {
			IExportedContent content = _exportedContentList.get(i);
			if (samePath(content, exportedContent)) {
				return i;
			}
		}

		return -1;
	}

	private boolean samePath(IExportedContent content, IExportedContent exportedContent) {
		if ((content instanceof FolderExportedContent) && (exportedContent instanceof FolderExportedContent)) {
			return ((FolderExportedContent) content).getPath().equals(
					((FolderExportedContent) exportedContent).getPath());
		}

		if ((content instanceof FileExportedContent) && (exportedContent instanceof FileExportedContent)) {
			return ((FileExportedContent) content).getPath().equals(((FileExportedContent) exportedContent).getPath());
		}

		return false;
	}

	public void add(IResource outputResource, IPath filePath, IProgressMonitor monitor) {
		constructExportedContent(outputResource, filePath, new Path(""), true, false, false);
	}

	public void update(IResource outputResource, IPath filePath, IProgressMonitor monitor) {
		constructExportedContent(outputResource, filePath, new Path(""), false, true, false);
	}

	public void add(IExportedContent exportedContent) {
		_exportedContentList.add(exportedContent);
	}

	public void flagAdded() {
		setFlags(true, false, false);
	}

	private void setFlags(boolean added, boolean updated, boolean removed) {
		_added = added;
		_updated = updated;
		_removed = removed;
	}

	public void flagRemoved() {
		setFlags(false, false, true);
	}

	public void flagUpdated() {
		setFlags(false, true, false);
	}

	public boolean hasChildren() {
		return isMulti();
	}

	public void toString(StringBuilder sb, String tab) {
		sb.append(tab).append(DeltaContentUtil.toString(this)).append(
				_folderPath != null ? getPath().lastSegment() : "<no path>").append("\n");
		for (IExportedContent e : this._exportedContentList) {
			if (e instanceof FolderExportedContent) {
				((FolderExportedContent) e).toString(sb, tab + "  ");
			} else {
				sb.append(tab).append(e.toString()).append("\n");
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		toString(sb, "");
		return sb.toString();
	}

	public void setLink(Link l) {
		this.link = l;
	}

	public Link getLink() {
		return link;
	}

	public void setTargetFolder(String folder) {
		_targetFolder = folder;
	}

	public String getTargetFolder() {
		return _targetFolder;
	}

	public FolderExportedContent add(String targetFolder) {
		FolderExportedContent ret = new FolderExportedContent(getItem(), getExporterType());
		ret._targetFolder = targetFolder;

		_exportedContentList.add(ret);
		return ret;
	}

	public boolean isTargetFolder() {
		for (IExportedContent ec : _exportedContentList) {
			if (ec instanceof FolderExportedContent && ((FolderExportedContent) ec)._targetFolder != null) {
				continue;
			}
			return false;
		}
		return true;
	}
}
