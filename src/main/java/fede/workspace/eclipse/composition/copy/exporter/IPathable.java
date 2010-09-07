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

import org.eclipse.core.runtime.IPath;

/**
 * Represents an object which is associated to a corresponding path. 
 * The interpretation of this path is not specified and depend on each implementation.
 * 
 * @author Thomas
 *
 */
public interface IPathable {

	/**
	 * Return the associated path.
	 * 
	 * @return the associated path.
	 */
	public IPath getPath();
}
