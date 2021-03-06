package mmrnmhrm.ui.editor.folding;

import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IRegion;

import _org.eclipse.dltk.ui.text.folding.DelegatingFoldingStructureProvider.FoldingContent;
import _org.eclipse.dltk.ui.text.folding.IFoldingBlockKind;
import _org.eclipse.dltk.ui.text.folding.IFoldingBlockProvider;
import _org.eclipse.dltk.ui.text.folding.PartitioningFoldingBlockProvider;
import dtool.ast.definitions.Module;
import melnorme.lang.ide.core.TextSettings_Actual.LangPartitionTypes;
import melnorme.lang.tooling.structure.SourceFileStructure;

public class DeeCommentFoldingBlockProvider extends PartitioningFoldingBlockProvider implements IFoldingBlockProvider {
	
	public DeeCommentFoldingBlockProvider() {
		super();
	}
	
	protected boolean fStringFolding;
	protected boolean fInitCollapseStrings;
	protected int offsetForModuleDeclaration; // Used to determine header comments

	
	@Override
	public void initializePreferences(IPreferenceStore preferenceStore) {
		super.initializePreferences(preferenceStore);
		fStringFolding = preferenceStore.getBoolean(DeeFoldingPreferenceConstants.EDITOR_FOLDING_INIT_STRINGS);
		fInitCollapseStrings = preferenceStore.getBoolean(DeeFoldingPreferenceConstants.EDITOR_FOLDING_INIT_STRINGS);
	}
	
	public boolean isCollapseStrings() {
		return fInitCollapseStrings;
	}
	
	@Override
	public void computeFoldableBlocks(FoldingContent content, SourceFileStructure sourceFileStructure) {
		offsetForModuleDeclaration = -1;
		
		if(isFoldingComments()) {
			
			// With changes in the parser perhaps this code could be simplified.
			Module deeModule = sourceFileStructure.parsedModule.module; 
			if (deeModule != null && deeModule.md != null) {
				offsetForModuleDeclaration = deeModule.md.getOffset();
			}
			
			computeBlocksForPartitionType(content,
					LangPartitionTypes.DEE_MULTI_COMMENT.getId(), DeeFoldingBlockKind.COMMENT, isCollapseComments());
			computeBlocksForPartitionType(content,
					LangPartitionTypes.DEE_NESTED_COMMENT.getId(), DeeFoldingBlockKind.COMMENT, isCollapseComments());
		}
		if(isFoldingDocs()) {
			computeBlocksForPartitionType(content,
					LangPartitionTypes.DEE_MULTI_DOCCOMMENT.getId(), DeeFoldingBlockKind.DOCCOMMENT, isCollapseDocs());
			computeBlocksForPartitionType(content,
					LangPartitionTypes.DEE_NESTED_DOCCOMMENT.getId(), DeeFoldingBlockKind.DOCCOMMENT, isCollapseDocs());
		}
		if(fStringFolding) {
			computeBlocksForPartitionType(content,
					LangPartitionTypes.DEE_STRING.getId(), DeeFoldingBlockKind.MULTILINESTRING, isCollapseStrings());
			computeBlocksForPartitionType(content,
					LangPartitionTypes.DEE_RAW_STRING.getId(), DeeFoldingBlockKind.MULTILINESTRING, isCollapseStrings());
			computeBlocksForPartitionType(content,
					LangPartitionTypes.DEE_DELIM_STRING.getId(), DeeFoldingBlockKind.MULTILINESTRING, isCollapseStrings());
		}
	}
	
	@Override
	protected void reportRegions(Document document, List<IRegion> regions, IFoldingBlockKind kind, boolean collapse)
			throws BadLocationException {
//		super.reportRegions(document, regions, kind, collapse);
		
//		// XXX: DLTK 3.0 copied/modified code
//		for (IRegion region : regions) {
//			// TODO
//			Object element = null;
//			requestor.acceptBlock(region.getOffset(), region.getOffset()
//					+ region.getLength(), kind, element, collapse);
//		}
		
		for (IRegion region : regions) {
			Object element = null;
			
			boolean effectiveCollapse = collapse;
			if(kind.isComment() && offsetForModuleDeclaration != -1 && region.getOffset() < offsetForModuleDeclaration) {
				effectiveCollapse = isCollapseHeaderComment();
			}
			
			requestor.acceptBlock(region.getOffset(), region.getOffset() + region.getLength(), 
					kind, element, effectiveCollapse);
		}
	}
}
