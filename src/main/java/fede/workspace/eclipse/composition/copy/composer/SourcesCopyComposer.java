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
package fede.workspace.eclipse.composition.copy.composer;

import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import fr.imag.adele.cadse.core.Item;
import fr.imag.adele.cadse.core.build.IBuildingContext;
import fr.imag.adele.cadse.core.build.IExportedContent;
import fr.imag.adele.cadse.core.build.IExporterTarget;
import fede.workspace.eclipse.java.JavaProjectManager;

public class SourcesCopyComposer extends JavaCopyComposer {

	
	private boolean createSourceEntry;


	public SourcesCopyComposer(Item contentManager, String type, boolean createSourceEntry, String targetPath) {
		super(contentManager, type, targetPath);
		this.createSourceEntry = createSourceEntry;
	}


	public SourcesCopyComposer(Item contentManager, String type, boolean createSourceEntry) {
		this(contentManager, type, createSourceEntry, COMPONENTS_SOURCES);
	}
	
	
	@Override
	protected void postCompose(IBuildingContext context, List<IExportedContent> listExportedContent, IExporterTarget target) {
		super.postCompose(context, listExportedContent, target);
		
		if (createSourceEntry) {
			try {
				IJavaProject javaProject = JavaProjectManager.getJavaProject(getItem());
				IFolder fSources = javaProject.getProject().getFolder(getTargetPath());
				IPath path = fSources.getFullPath();
				IClasspathEntry ce = JavaCore.newSourceEntry(path , new IPath[] {}, new IPath[] {}, null);
				
				JavaProjectManager.addProjectClasspath(javaProject, ce, null,false);
			} catch (JavaModelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	@Override
	protected boolean isSetReadOnly() {
		return true;
	}

}
