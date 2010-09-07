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

import java.io.Serializable;

import org.eclipse.core.resources.IContainer;

import fr.imag.adele.cadse.core.CadseException;
import fr.imag.adele.cadse.core.LogicalWorkspace;
import fr.imag.adele.cadse.core.Item;
import fr.imag.adele.cadse.core.build.IBuildingContext;

public interface ITargetContent extends Serializable {

	/**
	 * Return the last item which has updated the represented content. If this
	 * information is not available, it returns null.
	 * 
	 * @return the last item which has updated the represented content.
	 */
	public Item updatedBy();

	/**
	 * Return the last item which has added the represented content. If this
	 * information is not available, it returns null.
	 * 
	 * @return the last item which has added the represented content.
	 */
	public Item addedBy();

	/**
	 * Return the last item which has removed the represented content. If this
	 * information is not available, it returns null.
	 * 
	 * @return the last item which has removed the represented content.
	 */
	public Item removedBy();

	/**
	 * Return true only if the last operation performed on the represented
	 * content is addition.
	 * 
	 * @return true only if the last operation performed on the represented
	 *         content is addition.
	 */
	public boolean lastOpIsAdd();

	/**
	 * Return true only if the last operation performed on the represented
	 * content is removal.
	 * 
	 * @return true only if the last operation performed on the represented
	 *         content is removal.
	 */
	public boolean lastOpIsRemove();

	/**
	 * Return true only if the last operation performed on the represented
	 * content is modification.
	 * 
	 * @return true only if the last operation performed on the represented
	 *         content is modification.
	 */
	public boolean lastOpIsUpdate();

	/**
	 * Flag the last operation as addition and associate the specified item to
	 * this operation.
	 * 
	 * @param item
	 *            an item which performed an addition
	 */
	public void setAddedBy(Item item);

	/**
	 * Flag the last operation as modification and associate the specified item
	 * to this operation.
	 * 
	 * @param item
	 *            an item which performed an modification
	 */
	public void setUpdatedBy(Item item);

	/**
	 * Flag the last operation as removal and associate the specified item to
	 * this operation.
	 * 
	 * @param item
	 *            an item which performed an removal
	 */
	public void setRemovedBy(Item item);

	/**
	 * Set the workspace model which contain all item referenced by this target
	 * content.
	 * 
	 * @param model
	 *            the workspace model which contain all item referenced by this
	 *            target content
	 */
	public void setModel(LogicalWorkspace model);

	/**
	 * Delete the content represented by this object.
	 * 
	 * @param context
	 *            the building context
	 * @param targetFolder
	 *            the root folder of the represented resource
	 * @param reporsitory
	 *            the repository which manages this target folder
	 * @throws MelusineException
	 *             in case of error during deletion
	 */
	public void deleteTargetContent(IBuildingContext context, IContainer targetFolder, IRepository repository)
			throws CadseException;

	/**
	 * 
	 * @return the target where is copied ...
	 */
	public String getTarget();

	/**
	 * set the target. the target is where the resources is copier or created...
	 * 
	 * @param target
	 *            a relatif path form the parent container...
	 */
	public void setTarget(String target);

}
