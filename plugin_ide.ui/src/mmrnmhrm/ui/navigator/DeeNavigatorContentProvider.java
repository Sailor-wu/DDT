/*******************************************************************************
 * Copyright (c) 2015, 2015 Bruno Medeiros and other Contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bruno Medeiros - initial API and implementation
 *******************************************************************************/
package mmrnmhrm.ui.navigator;

import static melnorme.utilbox.core.Assert.AssertNamespace.assertUnreachable;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;

import dtool.dub.BundlePath;
import melnorme.lang.ide.core.project_model.IProjectModelListener;
import melnorme.lang.ide.core.project_model.UpdateEvent;
import melnorme.lang.ide.core.utils.EclipseUtils;
import melnorme.lang.ide.ui.navigator.NavigatorElementsSwitcher;
import melnorme.lang.ide.ui.views.AbstractNavigatorContentProvider;
import melnorme.utilbox.collections.ArrayList2;
import melnorme.utilbox.collections.Indexable;
import mmrnmhrm.core.DeeCore;
import mmrnmhrm.core.dub_model.DeeBundleModelManager.DeeBundleModel;
import mmrnmhrm.core.dub_model.DubBundleInfo;
import mmrnmhrm.core.workspace.viewmodel.DubDepSourceFolderElement;
import mmrnmhrm.core.workspace.viewmodel.DubDependenciesContainer;
import mmrnmhrm.core.workspace.viewmodel.DubDependencyElement;
import mmrnmhrm.core.workspace.viewmodel.DubErrorElement;
import mmrnmhrm.core.workspace.viewmodel.DubRawDependencyElement;
import mmrnmhrm.core.workspace.viewmodel.IDubElement;
import mmrnmhrm.core.workspace.viewmodel.StdLibContainer;

public class DeeNavigatorContentProvider extends AbstractNavigatorContentProvider {
	
	public static DeeBundleModel getBundleModel() {
		return DeeCore.getDeeBundleModel();
	}
	
	protected final DubNavigatorModelListener listener = new DubNavigatorModelListener();
	
	@Override
	protected void viewerInitialized() {
		super.viewerInitialized();
		
		getBundleModel().addListener(listener);
	}
	
	@Override
	public void dispose() {
		getBundleModel().removeListener(listener);
		
		super.dispose();
	}
	
	protected class DubNavigatorModelListener extends NavigatorModelListener 
		implements IProjectModelListener<DubBundleInfo> {
		
		@Override
		public void notifyUpdateEvent(UpdateEvent<DubBundleInfo> updateEvent) {
			viewerRefreshThrottleJob.scheduleRefreshJob();
		}
		
		@Override
		protected Indexable<Object> getElementsToRefresh() {
			ArrayList2<Object> elementsToRefresh = new ArrayList2<>();
			for(String projectName : getBundleModel().getModelProjects()) {
				IProject project = EclipseUtils.getWorkspaceRoot().getProject(projectName);
				elementsToRefresh.add(project);
			}
			return elementsToRefresh;
		}
		
	}
	
	/* -----------------  ----------------- */
	
	@Override
	protected LangNavigatorSwitcher_HasChildren hasChildren_switcher() {
		return new LangNavigatorSwitcher_HasChildren() {
			
			@Override
			public Boolean visitProject(IProject project) {
				return project.isAccessible() && getBundleModel().getProjectInfo(project) != null;
			}
			
			@Override
			public Boolean visitDubElement(IDubElement dubElement) {
				return dubElement.hasChildren();
			}
		};
	}
	
	@Override
	protected LangNavigatorSwitcher_GetChildren getChildren_switcher() {
		return new LangNavigatorSwitcher_GetChildren() {
			@Override
			public Object[] visitDubElement(IDubElement dubElement) {
				return dubElement.getChildren();
			}
			
			@Override
			public void addFirstProjectChildren(IProject project, ArrayList2<Object> projectChildren) {
				DubBundleInfo projectInfo = getBundleModel().getProjectInfo(project);
				if(projectInfo != null) {
					DubDependenciesContainer dubContainer = projectInfo.getDubContainer(project);
					projectChildren.add(dubContainer);
					projectChildren.add(new StdLibContainer(projectInfo.getCompilerInstall(), project));
				}
			}
		};
	}
	
	@Override
	protected LangNavigatorSwitcher_GetParent getParent_switcher() {
		return new LangNavigatorSwitcher_GetParent() {
			@Override
			public Object visitDubElement(IDubElement dubElement) {
				return dubElement.getParent();
			}
		};
	}
	
	/* ----------------- specific switcher ----------------- */
	
	public static interface DeeNavigatorAllElementsSwitcher<RET> extends NavigatorElementsSwitcher<RET> {
		
		@Override
		default RET visitDubElement(IDubElement element) {
			switch (element.getElementType()) {
			case DUB_DEP_CONTAINER: return visitDepContainer((DubDependenciesContainer) element);
			case DUB_STD_LIB: return visitStdLibContainer((StdLibContainer) element);
			case DUB_RAW_DEP: return visitRawDepElement((DubRawDependencyElement) element);
			case DUB_ERROR_ELEMENT: return visitErrorElement((DubErrorElement) element);
			case DUB_RESOLVED_DEP: return visitDepElement((DubDependencyElement) element);
			case DUB_DEP_SRC_FOLDER: return visitDepSourceFolderElement((DubDepSourceFolderElement) element);
			}
			throw assertUnreachable();
		}
		
		public abstract RET visitDepContainer(DubDependenciesContainer element);
		public abstract RET visitStdLibContainer(StdLibContainer element);
		public abstract RET visitRawDepElement(DubRawDependencyElement element);
		public abstract RET visitErrorElement(DubErrorElement element);
		public abstract RET visitDepElement(DubDependencyElement element);
		public abstract RET visitDepSourceFolderElement(DubDepSourceFolderElement element);
		
		@Override
		default RET visitOther(Object element) {
			if(isDubManifestFile(element)) {
				return visitDubManifestFile((IFile) element);
			}
			if(isDubCacheFolder(element)) {
				return visitDubCacheFolder((IFolder) element);
			}
			if(isDubSourceFolder(element)) {
				return visitDubSourceFolder((IFolder) element);
			}
			return null;
		}
		
		public abstract RET visitDubManifestFile(IFile element);
		
		public abstract RET visitDubCacheFolder(IFolder element);
		
		public abstract RET visitDubSourceFolder(IFolder element);
		
	}
	
	public static boolean isDubManifestFile(Object element) {
		if(element instanceof IFile) {
			IFile file = (IFile) element;
			if(file.getProjectRelativePath().equals(new Path(BundlePath.DUB_MANIFEST_FILENAME))) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean isDubCacheFolder(Object element) {
		if(!(element instanceof IFolder)) {
			return false;
		} 
		IFolder folder = (IFolder) element;
		if(folder.getProjectRelativePath().equals(new Path(".dub"))) {
			return true;
		}
		return false;
	}
	
	public static boolean isDubSourceFolder(Object element) {
		if(!(element instanceof IFolder)) {
			return false;
		} 
		IFolder folder = (IFolder) element;
		IProject project = folder.getProject();
		DubBundleInfo projectInfo = getBundleModel().getProjectInfo(project);
		if(projectInfo == null) {
			return false;
		}
		
		java.nio.file.Path[] sourceFolders = projectInfo.getMainBundle().getEffectiveSourceFolders();
		for (java.nio.file.Path srcFolderPath : sourceFolders) {
			if(folder.getProjectRelativePath().toFile().toPath().equals(srcFolderPath)) {
				return true;
			}
		}
		return false;
	}
	
}