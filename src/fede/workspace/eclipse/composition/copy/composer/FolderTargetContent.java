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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import fede.workspace.eclipse.composition.CompositeBuildingContext;
import fede.workspace.eclipse.composition.copy.exporter.FolderExportedContent;
import fr.imag.adele.cadse.core.CadseException;
import java.util.UUID;
import fr.imag.adele.cadse.core.LogicalWorkspace;
import fr.imag.adele.cadse.core.Item;
import fr.imag.adele.cadse.core.build.IBuildingContext;

public class FolderTargetContent extends FolderExportedContent implements ITargetContent {

	private static final long			serialVersionUID	= -6786362507675970304L;

	private UUID					_itemId;

	private UUID					_addedItemId;

	private UUID					_updatedItemId;

	private UUID					_removedItemId;

	private transient LogicalWorkspace	_model;

	private boolean						_lastOpIsRemove;

	private boolean						_lastOpIsUpdate;

	private boolean						_lastOpIsAdd;

	private FolderTargetContent() {
		super();
		// used by serialization mechanism
	}

	/**
	 * Create a FolderTargetContent without any member.
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
	 * @param targetFolder
	 */
	public FolderTargetContent(Item item, Class exporterType, IPath folderPath, boolean added, boolean updated,
			boolean removed, String target) {
		super(item, exporterType, folderPath, added, updated, removed);
		if (added) {
			setAddedBy(item);
		}
		if (updated) {
			setUpdatedBy(item);
		}
		if (removed) {
			setRemovedBy(item);
		}
		setTarget(target);
	}

	public Item addedBy() {
		return _model.getItem(_addedItemId);
	}

	public boolean lastOpIsAdd() {
		return _lastOpIsAdd;
	}

	public boolean lastOpIsRemove() {
		return _lastOpIsRemove;
	}

	public boolean lastOpIsUpdate() {
		return _lastOpIsUpdate;
	}

	public Item removedBy() {
		return _model.getItem(_removedItemId);
	}

	public Item updatedBy() {
		return _model.getItem(_updatedItemId);
	}

	public void setModel(LogicalWorkspace model) {
		this._model = model;
		this._item = model.getItem(_itemId);
	}

	public void deleteTargetContent(IBuildingContext context, IContainer targetFolder, IRepository repository)
			throws CadseException {
		context.subTask("deleting packaged item " + getPath());

		// remove it only if there is no file.
		IFolder folderToRem = getFolder(targetFolder);
		try {
			if (containsNoFile(folderToRem, targetFolder, repository)) {
				folderToRem.delete(true, false, ((CompositeBuildingContext) context).getMonitor());
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		context.worked(1);
	}

	private static boolean containsNoFile(IFolder folderToRem, IContainer targetFolder, IRepository repository) {

		if (!folderToRem.exists()) {
			return false;
		}

		IResource[] resources = null;
		try {
			resources = folderToRem.members(false);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false; // considers that potentially existing files
		}

		for (IResource resource : resources) {
			if (resource instanceof IFile) {
				IFile file = (IFile) resource;

				ITargetContent targetContent = repository.getTargetContent(
						PathUtil.getRelativePath(targetFolder, file), false);
				if ((targetContent == null) || (targetContent.addedBy() == null)) {
					return false;
				}

			} else if (resource instanceof IFolder) {
				IFolder subFolder = (IFolder) resource;

				if (!containsNoFile(subFolder, targetFolder, repository)) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Return the represented folder.
	 * 
	 * @param targetFolder
	 * @return the represented folder.
	 */
	private IFolder getFolder(IContainer targetFolder) {
		return targetFolder.getFolder(getPath());
	}

	public void setAddedBy(Item item) {
		flagAdded();
		_addedItemId = item.getId();
	}

	public void setRemovedBy(Item item) {
		flagRemoved();
		_removedItemId = item.getId();
	}

	public void setUpdatedBy(Item item) {
		flagUpdated();
		_updatedItemId = item.getId();
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
		out.writeObject(_folderPath.toPortableString());
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		_folderPath = Path.fromPortableString((String) in.readObject());
	}

	public String getTarget() {
		return getTargetFolder();
	}

	public void setTarget(String target) {
		setTargetFolder(target);
	}

}
