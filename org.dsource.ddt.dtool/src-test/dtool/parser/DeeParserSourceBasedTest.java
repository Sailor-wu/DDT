/*******************************************************************************
 * Copyright (c) 2012, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bruno Medeiros - initial API and implementation
 *******************************************************************************/
package dtool.parser;

import static dtool.parser.DeeParserTest.runParserTest______________________;
import static dtool.tests.DToolTestResources.getTestResource;
import static melnorme.utilbox.core.Assert.AssertNamespace.assertFail;
import static melnorme.utilbox.core.Assert.AssertNamespace.assertNotNull;
import static melnorme.utilbox.core.Assert.AssertNamespace.assertTrue;
import static melnorme.utilbox.core.CoreUtil.areEqual;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import melnorme.utilbox.core.Predicate;
import melnorme.utilbox.misc.ArrayUtil;
import melnorme.utilbox.misc.StringUtil;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import dtool.ast.SourceRange;
import dtool.parser.DeeParserTest.NamedNodeElement;
import dtool.parser.ParserError.EDeeParserErrors;
import dtool.sourcegen.AnnotatedSource;
import dtool.sourcegen.AnnotatedSource.MetadataEntry;
import dtool.sourcegen.TemplatedSourceProcessor2;
import dtool.tests.SimpleParser;
import dtool.util.NewUtils;

@RunWith(Parameterized.class)
public class DeeParserSourceBasedTest extends DeeSourceBasedTest {
	
	protected static final String TESTFILESDIR = "dtool.parser/parser-tests";
	
	protected static TemplatedSourceProcessor2 commonDefinitions = new TemplatedSourceProcessor2();;
	
	@Parameters
	public static Collection<Object[]> filesToParse() throws IOException {
		ArrayList<File> commonHeaderFileList = getDeeModuleList(getTestResource(TESTFILESDIR+"/common"), true);
		
		for (File headerFile : commonHeaderFileList) {
			TemplatedSourceProcessor2 tsp = new TestsTemplateSourceProcessor();
			tsp.processSource_unchecked("#", readStringFromFileUnchecked(headerFile));
			commonDefinitions.addGlobalExpansions(tsp.getGlobalExpansions());
		}
		
		return toFnParameterList(getDeeModuleList(getTestResource(TESTFILESDIR), true), new Predicate<File>() {
			@Override
			public boolean evaluate(File file) {
				if(file.getName().endsWith("_TODO") || file.getParentFile().getName().equals("common"))
					return true;
				return false;
			}
		});
	}
	
	protected final File file;
	
	public DeeParserSourceBasedTest(File file) {
		this.file = file;
	}
	
	@Test
	public void runSourceBasedTests() throws Exception { runSourceBasedTests$(); }
	public void runSourceBasedTests$() throws Exception {
		AnnotatedSource[] sourceBasedTests = getSourceBasedTests(file, commonDefinitions);
		for (AnnotatedSource testCase : sourceBasedTests) {
			testsLogger.println(">> ----------- Parser source test: ----------- <<");
			testsLogger.println(testCase.source);
			runSourceBasedTest(testCase);
		}
	}
	
	public void runSourceBasedTest(AnnotatedSource testSource) {
		String parseSource = testSource.source;
		String parseRule = null;
		String expectedGenSource = parseSource;
		NamedNodeElement[] expectedStructure = null;
		boolean allowAnyErrors = false;
		boolean ignoreFurtherErrorMDs = false;
		
		ArrayList<ParserError> expectedErrors = new ArrayList<ParserError>();
		
		for (MetadataEntry mde : testSource.metadata) {
			if(mde.name.equals("AST_EXPECTED") || mde.name.equals("AST_SOURCE_EXPECTED")) {
				assertTrue(expectedGenSource == parseSource);
				expectedGenSource = mde.associatedSource;
				ignoreFurtherErrorMDs = true;
			} else if(mde.name.equals("AST_STRUCTURE_EXPECTED")) {
				assertTrue(expectedStructure == null);
				expectedStructure = processExpectedStructure(mde.associatedSource);
			} else if(mde.name.equals("error")){
				if(!ignoreFurtherErrorMDs) {
					expectedErrors.add(decodeError(parseSource, mde));
				}
			} else if(mde.name.equals("parser") && mde.value.equals("AllowAnyErrors")){
				allowAnyErrors = true;
			} else if(mde.name.equals("parser") && mde.value.equals("DontCheckSourceEquality")){
				expectedGenSource = null;
			} else if(mde.name.equals("PARSE")){
				parseRule = mde.value;
			} else{
				// TODO remove todo flag
				if(!(areEqual(mde.value, "flag") || areEqual(mde.name, "TODO")))
					assertFail("Unknown metadata");
			}
		}
		
		runParserTest______________________(
			parseSource, parseRule, expectedGenSource, expectedStructure, expectedErrors, allowAnyErrors);
	}
	
	public ParserError decodeError(String parseSource, MetadataEntry mde) {
		String errorType = StringUtil.upUntil(mde.value, "_");
		String errorParam = NewUtils.fromIndexOf("_", mde.value);
		
		DeeLexer deeLexer = new DeeLexer(parseSource);
		
		SourceRange errorRange = mde.getSourceRange();
		
		 if(errorType.equals("ITC")) {
			return new ParserError(EDeeParserErrors.INVALID_TOKEN_CHARACTERS, errorRange, mde.associatedSource, null);
		} else if(errorType.equals("MT")) {
			// TODO errorParam
			return new ParserError(EDeeParserErrors.MALFORMED_TOKEN, errorRange, null, null);
		} else if(errorType.equals("EXP")) {
			String expectedTokenStr = DeeLexerSourceBasedTest.transformTokenNameAliases(errorParam);
			return createErrorToken(EDeeParserErrors.EXPECTED_TOKEN, mde, deeLexer, true, expectedTokenStr);
		} else if(errorType.equals("EXPRULE")) {
			errorParam = getExpectedRuleName(errorParam);
			return createErrorToken(EDeeParserErrors.EXPECTED_RULE, mde, deeLexer, true, errorParam);
		} else if(errorType.equals("SE")) {
			errorParam = getExpectedRuleName(errorParam);
			return createErrorToken(EDeeParserErrors.SYNTAX_ERROR, mde, deeLexer, false, errorParam);
		} else if(mde.value.equals("BAD_LINKAGE_ID")) {
			return createErrorToken(EDeeParserErrors.INVALID_EXTERN_ID, mde, deeLexer, true, null);
		} else {
			throw assertFail();
		}
	}
	
	public ParserError createErrorToken(EDeeParserErrors errorTypeTk, MetadataEntry mde, DeeLexer deeLexer,
		boolean tokenBefore, String errorParam) {
		String errorSource = mde.associatedSource;
		SourceRange errorRange = mde.getSourceRange();
		
		if(mde.associatedSource == null) {
			Token lastToken = tokenBefore 
				? findLastEffectiveTokenBeforeOffset(mde.offset, deeLexer)
				: findNextEffectiveTokenAfterOffset(mde.offset, deeLexer);
			errorRange = DeeParser.sr(lastToken);
			errorSource = lastToken.tokenSource;
		}
		return new ParserError(errorTypeTk, errorRange, errorSource, errorParam);
	}
	
	public String getExpectedRuleName(String errorParam) {
		if(errorParam.equals("decl")) {
			errorParam = DeeParser.DECLARATION_RULE;
		} else if(errorParam.equals("exp")) {
			errorParam = DeeParser.EXPRESSION_RULE;
		}
		return errorParam;
	}
	
	public Token findLastEffectiveTokenBeforeOffset(int offset, DeeLexer deeLexer) {
		assertTrue(offset <= deeLexer.source.length());
		
		Token lastNonIgnoredToken = null;
		while(true) {
			Token token = deeLexer.next();
			if(token.getStartPos() >= offset || token.getEndPos() > offset) {
				assertNotNull(lastNonIgnoredToken);
				deeLexer.reset(lastNonIgnoredToken.startPos);
				break;
			}
			if(token.type.isParserIgnored) {
				continue;
			}
			lastNonIgnoredToken = token;
		}
		return lastNonIgnoredToken;
	}
	
	public Token findNextEffectiveTokenAfterOffset(int offset, DeeLexer deeLexer) {
		assertTrue(offset <= deeLexer.source.length());
		
		while(true) {
			Token token = deeLexer.next();
			if(token.type == DeeTokens.EOF) {
				assertFail();
			}
			if(token.type.isParserIgnored) {
				continue;
			}
			if(token.getStartPos() >= offset) {
				return token;
			}
			assertTrue(token.getEndPos() <= offset);
		}
	}
	
	protected NamedNodeElement[] processExpectedStructure(String source) {
		SimpleParser parser = new SimpleParser(source);
		NamedNodeElement[] namedElements = readNamedElementsList(parser);
		assertTrue(parser.lookaheadIsEOF());
		return namedElements;
	}
	
	public static NamedNodeElement[] readNamedElementsList(SimpleParser parser) {
		ArrayList<NamedNodeElement> elements = new ArrayList<NamedNodeElement>();
		
		while(true) {
			String id;
			NamedNodeElement[] children = null;
			
			parser.seekWhiteSpace();
			if(parser.tryConsume("*")) {
				id = NamedNodeElement.IGNORE_ALL;
			} else {
				if(parser.tryConsume("?")) {
					id = NamedNodeElement.IGNORE_NAME;
				} else {
					id = parser.consumeAlphaNumericUS(true);
					if(id.isEmpty()) {
						break;
					}
					parser.seekWhiteSpace();
				}
				if(parser.tryConsume("(")) {
					children = readNamedElementsList(parser);
					parser.seekWhiteSpace().consume(")");
				} else {
					children = new NamedNodeElement[0];
				}
			}
			elements.add(new NamedNodeElement(id, children));
		}
		assertTrue(parser.lookaheadIsEOF() || parser.lookAhead() == ')');
		
		return ArrayUtil.createFrom(elements, NamedNodeElement.class);
	}
	
}