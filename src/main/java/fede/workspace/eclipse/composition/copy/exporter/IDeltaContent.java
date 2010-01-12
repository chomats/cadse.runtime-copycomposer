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
 * Represents a content which defines a differency between a previous state.
 * 
 * @author Thomas
 *
 */
public interface IDeltaContent extends IExportedContent {

	
	/**
	 * Return true only if this content has been added since the previous state.
	 * 
	 * @return true only if this content has been added since the previous state.
	 */
	public boolean isAdded();
	
	/**
	 * Return true only if this content has been updated since the previous state.
	 * 
	 * @return true only if this content has been updated since the previous state.
	 */
	public boolean isUpdated();
	
	/**
	 * Return true only if this content has been removed since the previous state.
	 * 
	 * @return true only if this content has been removed since the previous state.
	 */
	public boolean isRemoved();
}
