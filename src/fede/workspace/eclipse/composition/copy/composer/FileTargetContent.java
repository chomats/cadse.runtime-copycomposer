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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import fede.workspace.eclipse.composition.CompositeBuildingContext;
import fede.workspace.eclipse.composition.copy.exporter.FileExportedContent;
import fr.imag.adele.cadse.core.CadseException;
import java.util.UUID;
import fr.imag.adele.cadse.core.LogicalWorkspace;
import fr.imag.adele.cadse.core.Item;
import fr.imag.adele.cadse.core.build.IBuildingContext;

public class FileTargetContent extends FileExportedContent implements ITargetContent {

	private static final long			serialVersionUID	= -5290458375498583370L;

	private UUID					_itemId;

	private UUID					_addedItemId;

	private UUID					_updatedItemId;

	private UUID					_removedItemId;

	private transient LogicalWorkspace	_model;

	private boolean						_lastOpIsRemove;

	private boolean						_lastOpIsUpdate;

	private boolean						_lastOpIsAdd;

	private String						_target;

	private FileTargetContent() {
		super();
		// used by serialization mechanism
	}

	/**
	 * Create a FileTargetContent.
	 * 
	 * @param relativePath
	 *            relative path
	 * @param file
	 *            the represented file
	 * @param item
	 *            item associated to the represented file
	 * @param exporterType
	 *            the exporter type related to this file
	 * @param added
	 *            added flag
	 * @param updated
	 *            updated flag
	 * @param removed
	 *            removed flag
	 * @param targetFolder
	 */
	public FileTargetContent(IPath relativePath, IFile file, Item item, Class exporterType, boolean added,
			boolean updated, boolean removed, String target) {
		super(relativePath, file, item, exporterType, added, updated, removed);

		_itemId = item.getId();

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

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
		out.writeObject(_relativePath.toPortableString());
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		_relativePath = Path.fromPortableString((String) in.readObject());
	}

	public void setModel(LogicalWorkspace model) {
		this._model = model;
		this._item = model.getItem(_itemId);
	}

	public void deleteTargetContent(IBuildingContext context, IContainer targetFolder, IRepository repository)
			throws CadseException {
		context.subTask("deleting packaged item " + getPath()); // getFile() can
		// be null
		try {
			getFile(targetFolder).delete(true, false, ((CompositeBuildingContext) context).getMonitor());
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		context.worked(1);
	}

	/**
	 * Return the represented file.
	 * 
	 * @param targetFolder
	 * @return the represented file.
	 */
	private IFile getFile(IContainer targetFolder) {

		return targetFolder.getFile(getPath());
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

	public String getTarget() {
		return _target;
	}

	public void setTarget(String target) {
		_target = (target);
	}
}
