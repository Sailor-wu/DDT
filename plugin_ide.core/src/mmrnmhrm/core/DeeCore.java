/*******************************************************************************
 * Copyright (c) 2014, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bruno Medeiros - initial API and implementation
 *******************************************************************************/
package mmrnmhrm.core;

import melnorme.lang.ide.core.LangCore;
import mmrnmhrm.core.engine_client.DToolClient;
import mmrnmhrm.core.engine_client.DubProcessManager;
import mmrnmhrm.core.workspace.DubWorkspaceModel;
import mmrnmhrm.core.workspace.DubModelManager;

import org.osgi.framework.BundleContext;

public class DeeCore extends LangCore {
	
	protected static DToolClient dtoolClient;
	protected static final DubWorkspaceModel dubModel = new DubWorkspaceModel();
	protected static final DubModelManager modelManager = new DubModelManager(dubModel);
	
	public static DubProcessManager getDubProcessManager() {
		return getWorkspaceModelManager().getProcessManager();
	}
	
	public static DToolClient getDToolClient() {
		return dtoolClient;
	}
	
	public static DubWorkspaceModel getWorkspaceModel() {
		return dubModel;
	}
	
	public static DubModelManager getWorkspaceModelManager() {
		return modelManager;
	}
	
	@Override
	protected void doCustomStart(BundleContext context) {
		dtoolClient = DToolClient.initializeNew();
	}
	
	@Override
	public void doInitializeAfterUIStart() {
		modelManager.startManager(); // Start this after UI, to allow UI listener to register.
	}
	
	@Override
	protected void doCustomStop(BundleContext context) {
		modelManager.shutdownManager();
		dtoolClient.shutdown();
	}
	
}