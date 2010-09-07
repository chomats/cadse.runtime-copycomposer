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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * It is an helper class for managing eclipse files and folders. 
 * 
 * @author Thomas
 *
 */
public class FileUtil {

	/**
	 * Return true only if the specified resource is a folder.
	 * 
	 * @param outputResource an eclipse resource
	 * @return true only if the specified resource is a folder.
	 */
	public static final boolean isFolder(IResource outputResource) {
		return (outputResource.getType() == IResource.FOLDER);
	}

	/**
	 * Return the file which is represented by the Eclipse file item.
	 * 
	 * @return the file which is represented by the Eclipse file item.
	 */
	public static final File getFile(IFile iFile) {
		return iFile.getLocation().makeAbsolute().toFile();
	}
	
	/**
	 * Return the file corresponding to the file path appendended with '.<extension>'.
	 * 
	 * @param iFile a file
	 * @param extToAdd the extension to add.
	 * @return the file corresponding to the file path appendended with '.<extension>'.
	 */
	public static final IFile getFile(IFile iFile, String extToAdd) {
		IPath filePath = iFile.getFullPath();
		String fileName = filePath.lastSegment() + "." + extToAdd;
		return iFile.getParent().getFile(new Path(fileName));
	}
}
