/*******************************************************************************
 * Copyright (c) 2016 Bruno Medeiros and other Contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bruno Medeiros - initial API and implementation
 *******************************************************************************/
package melnorme.lang.ide.ui.text;

import org.eclipse.core.resources.IProject;

import melnorme.lang.ide.core.LangCore;
import melnorme.lang.ide.core.utils.ResourceUtils;
import melnorme.lang.ide.ui.editor.hover.AbstractDocDisplayInfoSupplier;
import melnorme.lang.tooling.LANG_SPECIFIC;
import melnorme.lang.tooling.ast.SourceRange;
import melnorme.lang.tooling.common.ISourceBuffer;
import melnorme.lang.tooling.common.ops.IOperationMonitor;
import melnorme.lang.tooling.toolchain.ops.AbstractToolOperation;
import melnorme.lang.tooling.toolchain.ops.OperationSoftFailure;
import melnorme.lang.tooling.toolchain.ops.SourceOpContext;
import melnorme.utilbox.concurrency.OperationCancellation;
import melnorme.utilbox.core.CommonException;
import melnorme.utilbox.misc.Location;
import mmrnmhrm.core.engine.DeeLanguageEngine;
import mmrnmhrm.ui.editor.hover.HoverUtil;

@LANG_SPECIFIC
public class DocDisplayInfoSupplier extends AbstractDocDisplayInfoSupplier {
	
	public DocDisplayInfoSupplier(ISourceBuffer sourceBuffer, int offset) {
		super(sourceBuffer, offset);
	}
	
	@Override
	public String doGetDocumentation(IOperationMonitor om) {
		String info = super.doGetDocumentation(om);
		
		if(info != null) {
			return HoverUtil.getCompleteHoverInfo(info, getCSSStyles());
		}
		return null;
	}
	
	protected String getCSSStyles() {
		return HoverUtil.getDDocPreparedCSS();
	}
	
	@Override
	protected String escapeToHTML(String rawDocumentation) {
		// don't escape, DDoc has HTML already
		return rawDocumentation;
	}
	
	@Override
	protected AbstractToolOperation<String> getFindDocOperation(ISourceBuffer sourceBuffer, int offset) {
		SourceOpContext opContext = sourceBuffer.getSourceOpContext(new SourceRange(offset, 0));
		
		AbstractToolOperation<String> findDocOp = new AbstractToolOperation<String>() {
			
			@Override
			public String executeToolOperation(IOperationMonitor om)
					throws CommonException, OperationCancellation, OperationSoftFailure {
				IProject project = ResourceUtils.getProjectFromMemberLocation(sourceBuffer.getLocation_opt());
				String dubPath = LangCore.settings().SDK_LOCATION.getValue(project).toString();
				Location fileLocation = opContext.getFileLocation();
				
				int offset = opContext.getOffset();
				return DeeLanguageEngine.getDefault().
						new FindDDocViewOperation(fileLocation, offset, -1, dubPath).runEngineOperation(om);
			}
		};
		
		return findDocOp;
	}
	
}