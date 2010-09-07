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

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import fede.workspace.eclipse.composition.copy.exporter.JavaClassRefExporter;
import fede.workspace.eclipse.java.JavaProjectManager;
import fr.imag.adele.cadse.core.Item;
import fr.imag.adele.cadse.core.build.IBuildingContext;
import fr.imag.adele.cadse.core.build.IExportedContent;
import fr.imag.adele.cadse.core.build.IExporterTarget;

public class JavaClassesCopyComposer extends JavaCopyComposer {
	String	_srcPath;

	public JavaClassesCopyComposer(Item contentManager, String targetPath, String srcPath) {
		super(contentManager, JavaClassRefExporter.JAVA_REF_EXPORTER_TYPE, targetPath);
		_srcPath = srcPath;
	}

	public JavaClassesCopyComposer(Item contentManager, String targetPath) {
		this(contentManager, targetPath, COMPONENTS_SOURCES);
	}

	public JavaClassesCopyComposer(Item contentManager) {
		this(contentManager, COMPONENTS_CLASSES, COMPONENTS_SOURCES);
	}

	@Override
	protected void postCompose(IBuildingContext context, List<IExportedContent> listExportedContent,
			IExporterTarget target) {
		super.postCompose(context, listExportedContent, target);
		try {
			IJavaProject javaProject = JavaProjectManager.getJavaProject(getItem());
			IFolder f = javaProject.getProject().getFolder(getTargetPath());
			IFolder fSources = javaProject.getProject().getFolder(_srcPath);
			IClasspathEntry ce = JavaCore.newLibraryEntry(f.getFullPath(), fSources.exists() ? fSources.getFullPath()
					: null, null, true);

			JavaProjectManager.addProjectClasspath(javaProject, ce, null, false);
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
