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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;

import fede.workspace.eclipse.composition.copy.composer.FileUtil;
import fede.workspace.tool.eclipse.MappingManager;
import fr.imag.adele.fede.workspace.si.view.View;

/**
 * Helper class to manage property files. Save and load process are fault
 * tolerant.
 * 
 * @author Thomas
 * 
 */
public class PropertyFile {

	private static final String	TMP_FILE_EXTENSION	= "tmp";

	private IFile				_propFile;

	private IFile				_tmpPropFile;

	/**
	 * Create a property file helper to manage properties saving and loading.
	 * 
	 * @param propFile
	 *            the property file which will be used to load and save the
	 *            properties.
	 */
	public PropertyFile(IFile propFile) {
		if (propFile == null) {
			throw new IllegalArgumentException("The property file cannot be null.");
		}

		this._propFile = propFile;
		this._tmpPropFile = FileUtil.getFile(_propFile, TMP_FILE_EXTENSION);
	}

	/**
	 * Save all the properties in the property file.
	 */
	public void saveProperties(Properties repoProps, String comment) {
		IProgressMonitor monitor = View.getDefaultMonitor();

		try {
			if (_propFile.exists()) {
				// make a backup of previous property file
				if (_tmpPropFile.exists()) {
					// only copy content
					InputStream fileStream = _propFile.getContents(true);
					_tmpPropFile.setContents(fileStream, true, true, monitor);
					try {
						fileStream.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					// full copy
					_propFile.copy(_tmpPropFile.getFullPath(), true, monitor);
				}
			}

			// save the properties in the property file
			MappingManager.createEmptyFile(_propFile, monitor);
			repoProps.storeToXML(new FileOutputStream(FileUtil.getFile(_propFile)), comment);

			// delete backup
			if (_tmpPropFile.exists()) {
				_tmpPropFile.delete(true, monitor);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Return all the properties saved in the property file. If the property
	 * file cannot be read, return an empty Properties object.
	 * 
	 * @return all the properties saved in the property file.
	 */
	public Properties loadProperties() {
		Properties lastProps = new Properties();
		if (_tmpPropFile.exists()) {
			try {
				lastProps.loadFromXML(new FileInputStream(FileUtil.getFile(_tmpPropFile)));

				return lastProps;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if (_propFile.exists()) {
			Properties props = new Properties();
			try {
				props.loadFromXML(new FileInputStream(FileUtil.getFile(_propFile)));

				return props;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return new Properties();
	}

}
