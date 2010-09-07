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

import fr.imag.adele.cadse.core.build.IExportedContent;

/**
 * It is an helper class to manipulate IDeltaContent.
 * 
 * @author Thomas
 *
 */
public class DeltaContentUtil {

	/**
	 * Return deltaContent if and only if deltaContent has expected flags else return existDeltaContent.
	 * 
	 * @param deltaCont a delta content
	 * @param existDeltaCont a delta content
	 * @param added expected added flag
	 * @param updated expected updated flag
	 * @param removed expected removed flag
	 * @return deltaContent if and only if deltaContent has expected flags else return existDeltaContent.
	 */
	public static final IDeltaContent choose(IDeltaContent deltaCont, IDeltaContent existDeltaCont, 
			boolean added, boolean updated, boolean removed) {
		if ((deltaCont.isAdded() == added) &&
			(deltaCont.isUpdated() == updated) &&
			(deltaCont.isRemoved() == removed))
				return deltaCont;
		else
			return existDeltaCont;
	}

	/**
	 * Return a copy of deltaContent with the specified flags.  
	 * If deltaContent is not instance of FileExportedContent or FolderExportedContent, it returns null.
	 * 
	 * @param deltaCont a delta content
	 * @param added wanted added flag
	 * @param updated wanted updated flag
	 * @param removed wanted removed flag
	 * @return a copy of deltaContent with the specified flags or null if deltaContent type is not manage by this method.
	 */
	public static final IExportedContent transformTo(IDeltaContent deltaCont,
			boolean added, boolean updated, boolean removed) {
		
		if ((deltaCont.isAdded() == added) &&
			(deltaCont.isUpdated() == updated) &&
			(deltaCont.isRemoved() == removed))
			return deltaCont;

		if (deltaCont instanceof FileExportedContent) {
			FileExportedContent fileContent = (FileExportedContent) deltaCont;
			return new FileExportedContent(fileContent.getPath(),
					fileContent.getFile(), fileContent.getItem(), fileContent
							.getExporterType(), added, updated, removed);
		}

		if (deltaCont instanceof FolderExportedContent) {
			FolderExportedContent folderContent = (FolderExportedContent) deltaCont;
			return new FolderExportedContent(folderContent.getItem(),
					folderContent.getExporterType(), folderContent
							.getPath(), added, updated, removed, folderContent.getChildren());
		}

		return null;
	}
	
	/**
	 * Update deltaContent flags to specified ones.
	 * 
	 * @param deltaCont a delta content
	 * @param added wanted added flag
	 * @param updated wanted updated flag
	 * @param removed wanted removed flag
	 */
	public static final void modify(IDeltaContent deltaCont,
			boolean added, boolean updated, boolean removed) {
		
		//TODO check that exactly one set to true
		
		if (deltaCont instanceof IDeltaSetter) {
			IDeltaSetter content = (IDeltaSetter) deltaCont;
			if (added)
				content.flagAdded();
			if (updated)
				content.flagUpdated();
			if (removed)
				content.flagRemoved();
		}
	}
	
	/**
	 * Return true only if at least one of the delta contents has the remove flag set to true.
	 * 
	 * @param deltaCont a delta content
	 * @param existDeltaCont a delta content
	 * @return true only if at least one of the delta contents has the remove flag set to true.
	 */
	public static final boolean atLeastOneRemove(IDeltaContent deltaCont,
			IDeltaContent existDeltaCont) {
		return (deltaCont.isRemoved() || existDeltaCont.isRemoved());
	}

	/**
	 * Return true only if at least one of the delta contents has the update flag set to true.
	 * 
	 * @param deltaCont a delta content
	 * @param existDeltaCont a delta content
	 * @return true only if at least one of the delta contents has the update flag set to true.
	 */
	public static final boolean atLeastOneUpdate(IDeltaContent deltaCont,
			IDeltaContent existDeltaCont) {
		return (deltaCont.isUpdated() || existDeltaCont.isUpdated());
	}

	/**
	 * Return true only if at least one of the delta contents has the add flag set to true.
	 * 
	 * @param deltaCont a delta content
	 * @param existDeltaCont a delta content
	 * @return true only if at least one of the delta contents has the add flag set to true.
	 */
	public static final boolean atLeastOneAdd(IDeltaContent deltaCont,
			IDeltaContent existDeltaCont) {
		return (deltaCont.isAdded() || existDeltaCont.isAdded());
	}

	/**
	 * Return true only if the delta contents have exactly the same delta flag values.
	 * 
	 * @param deltaCont a delta content
	 * @param existDeltaCont a delta content
	 * @return true only if the delta contents have exactly the same delta flag values.
	 */
	public static final boolean sameDeltaFlags(IDeltaContent deltaCont,
			IDeltaContent existDeltaCont) {
		return (deltaCont.isAdded() == existDeltaCont.isAdded())
				&& (deltaCont.isUpdated() == existDeltaCont.isUpdated())
				&& (deltaCont.isRemoved() == existDeltaCont.isRemoved());
	}
	
	public static final String toString(IDeltaContent deltaContent) {
		char[] sb = new char[5];
		sb[0] = '[';
		if (deltaContent.isAdded())
			sb[1] = 'A';
		else
			sb[1] = ' ';
		if (deltaContent.isUpdated())
			sb[2] = 'U';
		else
			sb[2] = ' ';
		if (deltaContent.isRemoved())
			sb[3] = 'R';
		else
			sb[3] = ' ';
		sb[4] = ']';
		return new String(sb);
	}
}
