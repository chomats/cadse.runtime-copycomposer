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

import fr.imag.adele.cadse.core.build.IExportedContent;

/**
 * This class groups a collection of methods to merge different folder delta trees.
 * 
 * @author Thomas
 *
 */
public class FolderMergeUtil {
	
	/**
	 * Merge the two tree into the exportedFolder argument.
	 * 
	 * @param exportedFolder
	 * @param folderToMerge
	 */
	public static void merge(FolderExportedContent exportedFolder, FolderExportedContent folderToMerge) {
		if (folderToMerge == null)
			return;
		
		FolderExportedContent current =  exportedFolder;
		FolderExportedContent currentToMerge =  folderToMerge;
		IExportedContent[] childrens = current.getChildren();
		IExportedContent[] childrensToMerge = currentToMerge.getChildren();
		for (IExportedContent contentToMerge : childrensToMerge) {
			IExportedContent content = find(contentToMerge, childrens);
			if (content == null)
				current.add(contentToMerge);
			else {
				merge(content, contentToMerge);
			}
		}
		
		// change flags of the folder node
		mergeFlags(current, currentToMerge);
	}

	private static void mergeFlags(IDeltaSetter current, IDeltaSetter currentToMerge) {
		boolean added = current.isAdded() && currentToMerge.isAdded();
		boolean updated = DeltaContentUtil.atLeastOneUpdate(current, currentToMerge) || 
		                  (DeltaContentUtil.atLeastOneAdd(current, currentToMerge) && 
		                		  DeltaContentUtil.atLeastOneRemove(current, currentToMerge));
		boolean removed = current.isRemoved() && currentToMerge.isRemoved();
		DeltaContentUtil.modify(current, added, updated, removed);
	}
	
	@SuppressWarnings("unused")
	private static void merge(FileExportedContent exportedFile, FileExportedContent fileToMerge) {
		mergeFlags(exportedFile, fileToMerge);
	}

	private static void merge(IExportedContent content, IExportedContent contentToMerge) {
		// do nothing for non compatible contents
		throw new IllegalArgumentException("content and contentToMerge must be of same type (FileExportedContent or FolderExportedContent).");
	}

	private static IExportedContent find(IExportedContent contentToMerge, IExportedContent[] childrens) {
		for (IExportedContent content : childrens) {
			if (contentToMerge.equals(content))
				return content;
		}
		
		return null;
	}
}
