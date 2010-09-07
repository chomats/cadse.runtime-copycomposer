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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaModelException;

import fr.imag.adele.cadse.core.content.ContentItem;
import fr.imag.adele.cadse.core.build.IExportedContent;
import fr.imag.adele.cadse.core.build.IExporterTarget;
import fede.workspace.eclipse.java.JavaProjectManager;

/**
 * This exporter exposes the compiled java class files to the composers.
 * 
 * @author Thomas
 *
 */
public class JavaClassRefExporter extends ProjectExporter {

	/**
	 * Exporter type which represents references of compiled java class files.
	 */
	public final static String JAVA_REF_EXPORTER_TYPE = "ref-classes";
	public final static String JAVA_SOURCE_FILE_REF_EXPORTER_TYPE = "ref-source-java";
	public final static String AJ_SOURCE_FILE_REF_EXPORTER_TYPE = "ref-source-aj";
	protected boolean ajExport;
	protected boolean sourceExport;
	
	/**
	 * Create a JavaClassRefExporter.
	 * 
	 * @param cm the related content manager
	 */
	public JavaClassRefExporter(ContentItem cm) {
		super(cm, JAVA_REF_EXPORTER_TYPE, JAVA_SOURCE_FILE_REF_EXPORTER_TYPE, AJ_SOURCE_FILE_REF_EXPORTER_TYPE);
	}

	
	/**
	 * Exposes all Java compiles java files (.class files) included in the specified directory 
	 * that have been added, removed or updated since the last build.  
	 * 
	 * Scans all output directories of the java project and exposes all modified classes to the composer.
	 * 
	 * If no resource delta is specified all the binary contents are exposed to the composers.
	 *
	 * @param componentProject the project which contain the component content.
	 * @param projectDelta     resource delta of componentProject since the last build
	 * @param monitor          the monitor of this build process
	 * @param exporterType     the exporter type of files and directory to export
	 * @throws CoreException   In case of error which cancel the build process
	 */
	@Override
	protected IExportedContent exportItem(IProject componentProject, IResourceDelta projectDelta, IProgressMonitor monitor, String exporterType, IExporterTarget target, boolean fullExport) throws CoreException {

		/*
		 * Verify this item is actually hosted in a Java Project
		 */
		if (!JavaProjectManager.isJavaProject(componentProject))
			throw new IllegalArgumentException("componentProject is not a Java project.");
		
		/*
		 * TODO	We scan all output directories, we should only scan the output directory associated with the
		 * item.
		 * 
		 * We need to handle mapping variants in which there are many composites in a single java project,
		 * this is the case for example when a composite has parts that are themselves java composites.
		 */
		FolderExportedContent folderContent = new FolderExportedContent(getItem(), exporterType);
		
		if (exporterType.equals(JAVA_REF_EXPORTER_TYPE)) {
			this.sourceExport = false;
			this.ajExport = false;
		} else {
			this.sourceExport = true;
			this.ajExport = exporterType.equals(AJ_SOURCE_FILE_REF_EXPORTER_TYPE);
		}
		
		Map<IPath, FolderExportedContent> outputLocations = findLocations(folderContent, exporterType);
			
		if (fullExport)
			projectDelta = null;
		
		
		for (Map.Entry<IPath, FolderExportedContent> outputPath : outputLocations.entrySet()) {
			IFolder outputRoot			= getFolder(outputPath.getKey());
			
			exportFolder(monitor, outputPath.getValue(), outputRoot, projectDelta);
		}
		
		return folderContent;
	}


	protected Map<IPath, FolderExportedContent> findLocations(FolderExportedContent folderContent, String exporterType) throws JavaModelException, CoreException {
		IJavaProject javaProject = JavaProjectManager.getJavaProject(getItem());
		
		Map<IPath, FolderExportedContent> outputLocations = new HashMap<IPath, FolderExportedContent>();
		IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
		
		if (exporterType.equals(JAVA_REF_EXPORTER_TYPE)) {
			for (IClasspathEntry entry : rawClasspath) {
				if (entry.getEntryKind() != IClasspathEntry.CPE_SOURCE) continue;
	
				
				if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					IPath outputPath = entry.getOutputLocation();
					if (outputPath == null) 
						outputPath = javaProject.getOutputLocation();
					
					outputLocations.put(getRelativePath(outputPath),folderContent);
				} else if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
					IPath outputPath = entry.getPath();
					if (outputPath != null) 
						outputLocations.put(getRelativePath(outputPath),folderContent);
				}
			}
		} else {
			for (IClasspathEntry entry : rawClasspath) {
				if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					IPath outputPath = entry.getPath();
					if (outputPath != null) 
						outputLocations.put(getRelativePath(outputPath),folderContent);
				} else if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
					IPath outputPath = entry.getSourceAttachmentPath();
					if (outputPath != null) 
						outputLocations.put(getRelativePath(outputPath),folderContent);
				}
			}
		}
		return outputLocations;
	}


	protected void exportFolder(IProgressMonitor monitor, FolderExportedContent folderContent, IFolder outputRoot, IResourceDelta projectDelta) throws CoreException {
		IResourceDelta outputDelta 	= (projectDelta != null) ? projectDelta.findMember(outputRoot.getProjectRelativePath()): null;
		
		// If no modification of the output location just skip it
		if ((projectDelta != null) && (outputDelta == null))
			return;
		
		Scanner scanner = new Scanner(folderContent);
		scanner.scan(outputRoot, outputDelta, monitor);
	}
	


	/**
	 * This class is a visitor that scans a binary output directory copying all modified
	 * files to a packaged item
	 * 
	 * @author vega
	 *
	 */
	protected class Scanner implements IResourceVisitor, IResourceDeltaVisitor {
		
		private final FolderExportedContent _exportedContent;

		private IFolder 			_outputFolder;
		private IProgressMonitor	_monitor;
		
		public Scanner(FolderExportedContent exportedContentList) {
			this._exportedContent	= exportedContentList;
		}

		/**
		 * This methods iterates over all modifications of the output folder aplying them to the
		 * packaged item
		 * 
		 */
		public synchronized void scan(IFolder outputFolder, IResourceDelta outputDelta, IProgressMonitor monitor) throws CoreException {
			this._outputFolder	= outputFolder;
			this._monitor		= monitor;
			
			if (outputDelta != null)
				outputDelta.accept(this);
			else
				outputFolder.accept(this);
		}
		
		/**
		 * This callback method is called to visit and filter all the content of the output
		 * folder in the case of full copies. We consider any matching file found as an addition.
		 */
		public boolean visit(IResource outputResource) throws CoreException {
			return added(outputResource);
		}

		/**
		 * This callback method is called to incrementally perform an update from the contents of
		 * the output folder
		 */
		public boolean visit(IResourceDelta outputDelta) throws CoreException {
			
			switch(outputDelta.getKind()) {
				case IResourceDelta.ADDED: {
					return added(outputDelta.getResource());
				}
				case IResourceDelta.REMOVED: {
					return removed(outputDelta.getResource());
				}
				case IResourceDelta.CHANGED: {
					return changed(outputDelta.getResource());
				}
			}
			
			return true;
		}
		
		private boolean removed(IResource outputResource) throws CoreException {
			if (filter(outputResource))
				return false;
			IPath filePath = getRelativePath(_outputFolder, outputResource);
			_exportedContent.delete(outputResource, filePath, _monitor);
			return isFolder(outputResource);
		}
		
		private boolean added(IResource outputResource) throws CoreException {
			if (filter(outputResource))
				return false;
			
			IPath filePath = getRelativePath(_outputFolder, outputResource);
			if (filePath.segmentCount() == 0)
				return true;
			
			_exportedContent.add(outputResource, filePath, _monitor);
			return isFolder(outputResource);
		}

		private boolean changed(IResource outputResource) throws CoreException {
			if (filter(outputResource))
				return false;
			IPath filePath	= getRelativePath(_outputFolder,outputResource);
			_exportedContent.update(outputResource, filePath, _monitor);
			return isFolder(outputResource);
		}

	}
	
	protected static final boolean isFolder(IResource outputResource) {
		return (outputResource.getType() == IResource.FOLDER);
	}
	
	
	public boolean filter(IResource outputResource) {
		if (sourceExport) {
			if (ajExport) {
				return "java".equals(outputResource.getFileExtension());
			} else {
				return "aj".equals(outputResource.getFileExtension());
			}	
		}
		if (outputResource.getName().equals(".svn"))
			return true;
		return false;
	}


	/**
	 * Gets a path relative to the project associated with this composer.
	 * 
	 * @param path
	 * @return
	 * @throws CoreException 
	 */
	protected final IPath getRelativePath(IPath path) throws CoreException {
		IProject javaProject = JavaProjectManager.getProject(getItem());
		if (javaProject.getFullPath().isPrefixOf(path))
			path = path.removeFirstSegments(javaProject.getFullPath().segmentCount());
		return path;
	}

	/**
	 * Gets the path of a resource relative to another resource, both resources must be
	 * located in the project associated with this composer.
	 * 
	 * @param path
	 * @return
	 * @throws CoreException 
	 */
	protected final IPath getRelativePath(IResource container, IResource member) throws CoreException {
		IPath containerPath = getRelativePath(container.getFullPath());
		IPath memberPath	= getRelativePath(member.getFullPath());
		
		if (containerPath.isPrefixOf(memberPath))
			memberPath = memberPath.removeFirstSegments(containerPath.segmentCount());
		
		return memberPath;
	}
	
	/**
	 * Gets a folder in the project associated with this composer
	 *
	 * @param relativePath
	 * @return
	 * @throws CoreException 
	 */
	protected final IFolder getFolder(IPath relativePath) throws CoreException {
		return JavaProjectManager.getProject(getItem()).getFolder(relativePath);
	}

}
