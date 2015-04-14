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
package mmrnmhrm.ui.editor.codeassist;

import static melnorme.utilbox.core.Assert.AssertNamespace.assertFail;

import java.util.List;

import melnorme.lang.ide.core.operations.TimeoutProgressMonitor;
import melnorme.lang.ide.ui.text.completion.LangCompletionProposalComputer;
import melnorme.lang.ide.ui.text.completion.LangContentAssistInvocationContext;
import melnorme.lang.tooling.completion.LangCompletionResult;
import melnorme.lang.tooling.ops.OperationSoftFailure;
import melnorme.lang.tooling.symbols.INamedElement;
import melnorme.utilbox.collections.ArrayList2;
import melnorme.utilbox.concurrency.OperationCancellation;
import melnorme.utilbox.core.CommonException;
import melnorme.utilbox.misc.Location;
import mmrnmhrm.core.engine_client.DToolClient;
import mmrnmhrm.core.model_elements.DefElementDescriptor;
import mmrnmhrm.ui.DeeImages;
import mmrnmhrm.ui.DeeUIPreferenceConstants.ElementIconsStyle;
import mmrnmhrm.ui.views.DeeElementImageProvider;
import mmrnmhrm.ui.views.DeeElementLabelProvider;
import mmrnmhrm.ui.views.DeeModelElementLabelProvider;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;

import dtool.engine.operations.DeeCompletionSearchResult;
import dtool.engine.operations.DeeCompletionSearchResult.DeeCompletionProposal;

public class DeeCompletionProposalComputer extends LangCompletionProposalComputer {
	
	protected DToolClient dtoolclient = DToolClient.getDefault();
	
	public DeeCompletionProposalComputer() {
	}
	
	@Override
	protected List<ICompletionProposal> doComputeCompletionProposals(LangContentAssistInvocationContext context,
			int offset) throws CoreException, CommonException, OperationSoftFailure {
		
		IDocument document = context.getViewer().getDocument();
		Location editoInputFile = context.getEditorInputLocation();
		
		DeeCompletionSearchResult completionResult = dtoolclient.performCompletionOperation(
			editoInputFile.path, offset, document.get(), 5000);
		ArrayList2<DeeCompletionProposal> proposals = completionResult.getAdaptedResults();
		
		ArrayList2<ICompletionProposal> result = new ArrayList2<>();
		for (DeeCompletionProposal proposal : proposals) {
			result.add(adaptProposal(proposal));
		}
		
		return result;
	}
	
	@Override
	protected LangCompletionResult doComputeProposals(LangContentAssistInvocationContext context, int offset,
			TimeoutProgressMonitor pm) throws CoreException, CommonException, OperationCancellation {
		throw assertFail();
	}
	
	@Override
	public List<IContextInformation> computeContextInformation(LangContentAssistInvocationContext context) {
		return super.computeContextInformation(context);
	}
	
	@Override
	protected void handleExceptionInUI(CommonException ce) {
		if(DToolClient.compilerPathOverride == null) {
			super.handleExceptionInUI(ce);;
		} else {
			// We are in tests mode
		}
	}
	
	/* -----------------  ----------------- */
	
	protected final static char[] VAR_TRIGGER = { ' ', '=', ';' };
	
	protected final DeeModelElementLabelProvider modelElementLabelProvider = new DeeModelElementLabelProvider();
	
	protected static char[] getVarTrigger() {
		return VAR_TRIGGER;
	}
	
	public DeeContentAssistProposal adaptProposal(DeeCompletionProposal proposal) {
		INamedElement namedElement = proposal.getExtraInfo();
		
		String replaceString = proposal.getReplaceString();
		int repStart = proposal.getReplaceStart();
		int repLength = proposal.getReplaceLength();
		Image image = createImage(proposal);
		
		String displayString = DeeElementLabelProvider.getLabelForContentAssistPopup(namedElement);
		
		DeeContentAssistProposal completionProposal = new DeeContentAssistProposal(replaceString, repStart, repLength,
				image, displayString, namedElement, null);
		completionProposal.setTriggerCharacters(getVarTrigger());
		return completionProposal;
	}
	
	protected Image createImage(DeeCompletionProposal proposal) {
		ImageDescriptor imageDescriptor = createImageDescriptor(proposal);
		return DeeImages.getImageDescriptorRegistry().get(imageDescriptor); 
	}
	
	public ImageDescriptor createImageDescriptor(DeeCompletionProposal proposal) {
		ElementIconsStyle iconStyle = DeeElementImageProvider.getIconStylePreference();
		
		DefElementDescriptor defDescriptor = null;
		
		if(proposal.getExtraInfo() instanceof INamedElement) {
			INamedElement defElement = proposal.getExtraInfo();
			defDescriptor = new DefElementDescriptor(defElement);
		} 
		
		if(defDescriptor != null) {
			return DeeElementImageProvider.getDefUnitImageDescriptor(defDescriptor, iconStyle);
		}
		// Return no image
		return null;
	}
	
}