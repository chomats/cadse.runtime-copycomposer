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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import fr.imag.adele.cadse.core.Item;
import fr.imag.adele.cadse.core.content.ContentItem;
import fr.imag.adele.cadse.core.build.Exporter;
import fr.imag.adele.cadse.core.build.IBuildingContext;
import fr.imag.adele.cadse.core.build.IExportedContent;
import fr.imag.adele.cadse.core.build.IExporterTarget;
import fede.workspace.eclipse.MelusineProjectManager;
import fede.workspace.eclipse.composition.CompositeBuilder;
import fede.workspace.eclipse.composition.CompositeBuildingContext;

public abstract class ProjectExporter extends Exporter {

	protected ProjectExporter(Item contentManager, String... exporterTypes) {
		super(contentManager, exporterTypes);
	}

	@Override
	public IExportedContent exportItem(IBuildingContext context, IExporterTarget target, String exporterType,
			boolean fullExport) {

		IProgressMonitor monitor = ((CompositeBuildingContext) context).getMonitor();
		CompositeBuilder builder = ((CompositeBuildingContext) context).getBuilder();

		/*
		 * Get the packaged item in the target repository, create it if needed.
		 */
		IProject componentProject = MelusineProjectManager.getProject(getItem());
		IResourceDelta componentUpdate = builder.getDelta(componentProject);
		if (fullExport) {
			componentUpdate = null;
		}

		IExportedContent eclipseExportedContent = null;
		try {
			// // export all if diff is not available
			// if (componentUpdate == null) {
			// List<IFile> files = findFiles(componentProject);
			// for (IFile file : files) {
			// //TODO
			// }
			// }

			/*
			 * package folder content
			 */
			eclipseExportedContent = exportItem(componentProject, componentUpdate, monitor, exporterType, target,
					fullExport);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return eclipseExportedContent;
	}

	// private List<IFile> findFiles(IProject componentProject) throws CoreException
	// {
	// List<IFile> files = new ArrayList<IFile>();
	// for (IResource resource : componentProject.members()) {
	// if (resource instanceof IFile) {
	// IFile file = (IFile) resource;
	// files.add(file);
	// } else if (resource instanceof IFolder) {
	// IFolder folder = (IFolder) resource;
	// findFiles(folder, files);
	// }
	// }
	// return files;
	// }
	//
	// private static void findFiles(IFolder folder, List/*<IFile>*/ exportedFiles)
	// throws CoreException {
	// IResource[] resources = folder.members(false);
	// for (int i = 0; i < resources.length; i++) {
	// IResource resource = resources[i];
	// if (resource instanceof IFile) {
	// IFile file = (IFile) resource;
	// exportedFiles.add(file);
	// } else if (resource instanceof IFolder) {
	// IFolder subFolder = (IFolder) resource;
	// findFiles(subFolder, exportedFiles);
	// }
	// }
	// }

	protected abstract IExportedContent exportItem(IProject componentProject, IResourceDelta componentUpdate,
			IProgressMonitor monitor, String exporterType, IExporterTarget target, boolean fullExport)
			throws CoreException;
}
