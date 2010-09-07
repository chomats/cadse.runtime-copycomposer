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

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import fr.imag.adele.cadse.core.Item;
import fr.imag.adele.cadse.core.Link;
import fr.imag.adele.cadse.core.build.IBuildingContext;
import fr.imag.adele.cadse.core.build.IExportedContent;

/**
 * Repository used by composers to identify items responsible of creation, 
 * updates and removal of files and folders.
 * 
 * @author Thomas
 *
 */
public interface IRepository {
	
	/**
	 * Return true only if each target content defined in this repository doesn't exist in the target container.
	 * 
	 * @return true only if each target content defined in this repository doesn't exist in the target container.
	 */
	public boolean hasExistingTargetContent();
	
	/**
	 * Return the target content which represents the specified target path.
	 * If there is no target content which represents this path, returns null.
	 * 
	 * @param relPath 
	 * @param isFolder specify if the relPath argument represents a folder
	 * @return the target content which represents the specified target path.
	 */
	public ITargetContent getTargetContent(IPath relPath, boolean isFolder);
	
	/**
	 * Return all the target contents contained in the repository.
	 * 
	 * @return all the target contents contained in the repository.
	 */
	public List<ITargetContent> getTargetContents();
	
	/**
	 * Return all target contents which reference (addedBy, updatedBy or removedBy) the specified item.
	 * 
	 * @param item an item
	 * @return all target contents which reference (addedBy, updatedBy or removedBy) the specified item.
	 */
	public List<ITargetContent> getTargetContents(Item item);
	
	/**
	 * 
	 * 
	 * @param relativePath
	 * @param isFolder
	 * @return
	 */
	public Item getLastModifierItem(IPath relativePath, boolean isFolder);
	
	/**
	 * Add the new targetContent to this repository. 
	 * If this target content already exists, it will be replace by the new one.
	 * 
	 * @param targetContent the target content to add
	 */
	public void add(ITargetContent targetContent);
	
	/**
	 * Add all the specified target contents to this repository. 
	 * If one of these target contents already exists, it will be replace by the new one.
	 * 
	 * @param targetContents the target contents to add
	 */
	public void add(List<ITargetContent> targetContents);
	
	/**
	 * Remove the specified targetContent to this repository. 
	 * 
	 * @param targetContent the target content to remove
	 */
	public void remove(ITargetContent targetContent);
	
	/**
	 * Remove all entries which reference the specified target contents.
	 * 
	 * @param targetContents list of target contents to remove
	 */
	public void remove(List<ITargetContent> targetContents);
	
	/**
	 * Remove all entries which reference the specified item.
	 * 
	 * @param item an item
	 */
	public void removeEntriesFor(Item item);
	
	/**
	 * Return all the target contents which should exist physically (last operation is not removal).
	 * 
	 * @return all the target contents which should exist physically.
	 */
	public List<IExportedContent> getExistingContents();
	
	/**
	 * Perform cleaning operations when this repository will not be longer used.
	 */
	public void close();
	
	/**
	 * Remove all entries.
	 */
	public void reset();

	public boolean contains(Link l);

	public void beginSaveLinks();

	public void endSaveLinks();

	public void saveLink(Link link);

	public void clean(IBuildingContext context) throws CoreException;
	
}
