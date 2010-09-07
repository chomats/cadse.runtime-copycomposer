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


import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

/**
 * It is an helper class for managing paths.
 * 
 * @author Thomas
 *
 */
public class PathUtil {
	
	/**
	 * Gets the path of a resource relative to another resource. 
	 * Container one must contain the member one.
	 * 
	 * @param container one of the container resource of the member
	 * @param member a resource
	 * @return the path of a resource relative to another resource.
	 */
	public static final IPath getRelativePath(IResource container, IResource member) {
		IPath containerPath = container.getFullPath();
		IPath memberPath	= member.getFullPath();
		
		return getRelativePath(containerPath, memberPath);
	}
	
	/**
	 * Gets the path of a resource relative to another resource.
	 * 
	 * @param containerPath path of the member container resource
	 * @param memberPath a resource path
	 * @return the path of a resource relative to another resource.
	 */
	public static final IPath getRelativePath(IPath containerPath, IPath memberPath) {
		if (containerPath.isPrefixOf(memberPath))
			memberPath = memberPath.removeFirstSegments(containerPath.segmentCount());
		
		return memberPath;
	}
}
