package dtool.parser;

import static dtool.util.NewUtils.replaceRegexFirstOccurrence;
import static melnorme.utilbox.core.Assert.AssertNamespace.assertNotNull;
import static melnorme.utilbox.core.Assert.AssertNamespace.assertTrue;

import java.util.ArrayList;

import dtool.ast.ASTCommonSourceRangeChecker.ASTAssertChecker;
import dtool.ast.ASTNeoNode;
import dtool.ast.NodeList2;
import dtool.ast.definitions.Module;
import dtool.parser.DeeParserSourceBasedTest.NamedNodeElement;
import dtool.parser.Token.ErrorToken;
import dtool.tests.CommonTestUtils;


public class DeeParserTest extends CommonTestUtils {
	
	public static void runParserTest______________________(String parseSource, String expectedGenSource, 
		NamedNodeElement[] expectedStructure, ArrayList<ParserError> expectedErrors, boolean allowAnyErrors) {
		
		DeeParserResult result = DeeParser.parse(parseSource);
		
		Module module = result.module;
		assertNotNull(module);
		
		if(expectedStructure != null) {
			checkExpectedStructure(module, expectedStructure);
		}
		
		if(expectedGenSource != null) {
			checkSourceEquality(module, expectedGenSource);
		}
		
		if(result.errors.size() == 0) {
			assertTrue(expectedErrors.size() == 0);
			checkSourceEquality(module, parseSource);
		} else if(allowAnyErrors == false) {
			checkParserErrors(result.errors, expectedErrors);
		}
		
		checkSourceRanges(parseSource, result);
	}
	
	public static void checkExpectedStructure(Module module, NamedNodeElement[] expectedStructure) {
		ASTNeoNode[] children = module.getChildren();
		checkExpectedStructure(children, module, expectedStructure, true);
	}
	
	public static void checkExpectedStructure(ASTNeoNode[] children, ASTNeoNode parent, 
		NamedNodeElement[] expectedStructure, boolean flattenNodeList) {
		
		if(flattenNodeList && children.length == 1 && children[0] instanceof NodeList2) {
			parent = children[0];
			children = parent.getChildren();
		}
		
		assertTrue(children.length == expectedStructure.length);
		
		for (int i = 0; i < expectedStructure.length; i++) {
			NamedNodeElement namedElement = expectedStructure[i];
			ASTNeoNode astNode = children[i];
			assertTrue(astNode.getParent() == parent);
			
			if(namedElement.name == NamedNodeElement.IGNORE_ALL) {
				continue;
			}
			if(namedElement.name != NamedNodeElement.IGNORE_NAME) {
				String expectedName = replaceRegexFirstOccurrence(namedElement.name, "(Def)(Var)", 1, "Definition");
				assertEquals(astNode.getClass().getSimpleName(), expectedName);
			}
			checkExpectedStructure(astNode.getChildren(), astNode, namedElement.children, true);
		}
	}
	
	public static void checkSourceEquality(ASTNeoNode node, String expectedGenSource) {
		String generatedSource = node.toStringAsCode();
		CheckSourceEquality.check(generatedSource, expectedGenSource, false);
	}
	
	public static class CheckSourceEquality {
		
		public static void check(String source, String expectedSource, boolean ignoreUT) {
			DeeLexer generatedSourceLexer = new DeeLexer(source);
			DeeLexer expectedSourceLexer = new DeeLexer(expectedSource);
			
			while(true) {
				Token tok = getContentToken(generatedSourceLexer, true, ignoreUT);
				Token tokExp = getContentToken(expectedSourceLexer, true, ignoreUT);
				assertEquals(tok.type, tokExp.type);
				assertEquals(tok.tokenSource, tokExp.tokenSource);
				
				if(tok.type == DeeTokens.EOF) {
					break;
				}
			}
		}
		
		public static Token getContentToken(DeeLexer lexer, boolean ignoreComments, boolean ignoreUT) {
			while(true) {
				Token token = lexer.next();
				if((token.type.isParserIgnored && (ignoreComments || !isCommentToken(token))) 
					|| (ignoreUT && isUnknownToken(token))) {
					continue;
				}
				return token;
			}
		}
		
		public static boolean isUnknownToken(Token token) {
			if(token instanceof ErrorToken) {
				ErrorToken errorToken = (ErrorToken) token;
				if(errorToken.originalToken == DeeTokens.ERROR) {
					return true;
				}
			}
			return false;
		}
		
		public static boolean isCommentToken(Token token) {
			return 
				token.type == DeeTokens.COMMENT_LINE ||
				token.type == DeeTokens.COMMENT_MULTI ||
				token.type == DeeTokens.COMMENT_NESTED;
		}
		
	}
	
	public static void checkParserErrors(ArrayList<ParserError> resultErrors, ArrayList<ParserError> expectedErrors) {
		for (int i = 0; i < resultErrors.size(); i++) {
			ParserError error = resultErrors.get(i);
			
			assertTrue(i < expectedErrors.size());
			ParserError expError = expectedErrors.get(i);
			assertEquals(error.errorType, expError.errorType);
			assertEquals(error.sourceRange, expError.sourceRange);
			assertEquals(error.msgErrorSource, expError.msgErrorSource);
			assertAreEqual(safeToString(error.msgObj2), safeToString(expError.msgObj2));
		}
		assertTrue(resultErrors.size() == expectedErrors.size());
	}
	
	public static void checkSourceRanges(String parseSource, DeeParserResult result) {
		Module module = result.module;
		
		// Check of source ranges
		module.accept(new ASTSourceRangeChecker(parseSource, result.errors));
		// Next one should not fail if previous one passed.
		ASTAssertChecker.checkConsistency(module);
	}
	
}