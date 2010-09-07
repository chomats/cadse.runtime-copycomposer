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

/**
 * Represents a delta content which have differency state that can be modified.
 * 
 * @author Thomas
 *
 */
public interface IDeltaSetter extends IDeltaContent {

	/**
	 * Flag this content as added from the previous status.
	 */
	public void flagAdded();
	
	/**
	 * Flag this content as updated from the previous status.
	 */
	public void flagUpdated();
	
	/**
	 * Flag this content as removed from the previous status.
	 */
	public void flagRemoved();
}
