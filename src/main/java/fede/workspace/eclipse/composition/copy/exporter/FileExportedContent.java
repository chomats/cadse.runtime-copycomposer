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
package fede.workspace.eclipse.composition.copy.exporter;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;

import java.util.UUID;
import fr.imag.adele.cadse.core.Item;
import fr.imag.adele.cadse.core.Link;
import fr.imag.adele.cadse.core.build.IBuildingContext;
import fr.imag.adele.cadse.core.build.IExportedContent;

/**
 * Instance of this class represent a file.
 * 
 * @author Thomas
 * 
 */
public class FileExportedContent implements IDeltaSetter, IPathable {

	private boolean				_added;

	private boolean				_updated;

	private boolean				_removed;

	protected transient Item	_item;

	private String				_exporterType;

	protected transient IFile	_file;

	protected transient IPath	_relativePath;

	private Link				link;

	protected FileExportedContent() {
		// used by serialization of subclasses
	}

	/**
	 * Create a FileExportedContent.
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
	 */
	public FileExportedContent(IPath relativePath, IFile file, Item item, String exporterType, boolean added,
			boolean updated, boolean removed) {

		if ((file == null) || (item == null) || (exporterType == null) || (relativePath == null)) {
			throw new IllegalArgumentException("All arguments must be not null.");
		}

		this._item = item;
		this._exporterType = exporterType;
		this._file = file;
		this._relativePath = relativePath;

		if (!(added | updated | removed)) {
			throw new IllegalArgumentException("The file exported content must be added or updated or removed.");
		}
		if ((added & updated) | (added & removed) | (updated & removed)) {
			throw new IllegalArgumentException(
					"Only one of the following flags can be true: added, updated and removed.");
		}

		this._added = added;
		this._removed = removed;
		this._updated = updated;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof FileExportedContent) {
			FileExportedContent fileContent = (FileExportedContent) o;
			return getPath().equals(fileContent.getPath()) && getItem().equals(fileContent.getItem())
					&& getExporterType().equals(fileContent.getExporterType());
		} else {
			return false;
		}
	}

	public IPath getPath() {
		return _relativePath;
	}

	public int hashcode() {
		return _relativePath.hashCode() + _item.hashCode() + _exporterType.hashCode();
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

	/**
	 * Return the file represented by this object.
	 * 
	 * @return the file represented by this object.
	 */
	public IFile getFile() {
		return _file;
	}

	public IExportedContent[] getChildren() {
		return null;
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

	public UUID getItemIdentification() {
		return _item.getId();
	}

	public boolean isMulti() {
		return false;
	}

	public void delete(IBuildingContext context) {
		// do nothing because there is no intermediate result
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

	@Override
	public String toString() {
		return DeltaContentUtil.toString(this) + " " + getPath().lastSegment();
	}

	public void setLink(Link l) {
		this.link = l;
	}

	public Link getLink() {
		return link;
	}
}
