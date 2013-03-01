/*******************************************************************************
 * Copyright (c) 2013, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bruno Medeiros - initial API and implementation
 *******************************************************************************/
package dtool.parser;

import static dtool.util.NewUtils.assertNotNull_;
import static melnorme.utilbox.core.Assert.AssertNamespace.assertFail;
import static melnorme.utilbox.core.Assert.AssertNamespace.assertNotNull;
import static melnorme.utilbox.core.Assert.AssertNamespace.assertTrue;
import static melnorme.utilbox.core.Assert.AssertNamespace.assertUnreachable;

import java.util.ArrayList;

import melnorme.utilbox.core.CoreUtil;
import melnorme.utilbox.misc.ArrayUtil;
import dtool.ast.ASTDefaultVisitor;
import dtool.ast.ASTNeoNode;
import dtool.ast.SourceRange;
import dtool.ast.expressions.ExpArrayLength;
import dtool.ast.expressions.ExpAssert;
import dtool.ast.expressions.ExpCall;
import dtool.ast.expressions.ExpCast;
import dtool.ast.expressions.ExpCastQual;
import dtool.ast.expressions.ExpCastQual.CastQualifiers;
import dtool.ast.expressions.ExpConditional;
import dtool.ast.expressions.ExpImportString;
import dtool.ast.expressions.ExpIndex;
import dtool.ast.expressions.ExpInfix;
import dtool.ast.expressions.ExpInfix.InfixOpType;
import dtool.ast.expressions.ExpLiteralArray;
import dtool.ast.expressions.ExpLiteralBool;
import dtool.ast.expressions.ExpLiteralChar;
import dtool.ast.expressions.ExpLiteralFloat;
import dtool.ast.expressions.ExpLiteralInteger;
import dtool.ast.expressions.ExpLiteralMapArray;
import dtool.ast.expressions.ExpLiteralMapArray.MapArrayLiteralKeyValue;
import dtool.ast.expressions.ExpLiteralString;
import dtool.ast.expressions.ExpMixinString;
import dtool.ast.expressions.ExpNew;
import dtool.ast.expressions.ExpNull;
import dtool.ast.expressions.ExpParentheses;
import dtool.ast.expressions.ExpPostfix;
import dtool.ast.expressions.ExpPostfix.PostfixOpType;
import dtool.ast.expressions.ExpPrefix;
import dtool.ast.expressions.ExpPrefix.PrefixOpType;
import dtool.ast.expressions.ExpReference;
import dtool.ast.expressions.ExpSlice;
import dtool.ast.expressions.ExpSuper;
import dtool.ast.expressions.ExpThis;
import dtool.ast.expressions.ExpTypeId;
import dtool.ast.expressions.Expression;
import dtool.ast.expressions.MissingExpression;
import dtool.ast.expressions.Resolvable;
import dtool.ast.expressions.Resolvable.IQualifierNode;
import dtool.ast.expressions.Resolvable.ITemplateRefNode;
import dtool.ast.references.RefIdentifier;
import dtool.ast.references.RefIndexing;
import dtool.ast.references.RefModuleQualified;
import dtool.ast.references.RefPrimitive;
import dtool.ast.references.RefQualified;
import dtool.ast.references.RefTemplateInstance;
import dtool.ast.references.RefTypeDynArray;
import dtool.ast.references.RefTypePointer;
import dtool.ast.references.Reference;
import dtool.parser.ParserError.ParserErrorTypes;


public class DeeParser_RefOrExp extends AbstractParser {
	
	public DeeParser_RefOrExp(String source) {
		super(new DeeLexer(source));
	}
	
	public DeeParser_RefOrExp(DeeLexer deeLexer) {
		super(deeLexer);
	}
	
	/* ----------------------------------------------------------------- */
	
	public DeeTokens lookAheadGrouped() {
		return lookAheadToken().type.getGroupingToken();
	}
	
	public String idTokenToString(LexElement id) {
		return id.isMissingElement() ? null : id.token.source;
	}
	
	/* --------------------  reference parsing  --------------------- */
	
	protected RefIdentifier parseRefIdentifier() {
		LexElement id = tryConsumeIdentifier();
		return connect(new RefIdentifier(idTokenToString(id), sr(id.token)));
	}
	
	protected RefIdentifier createMissingRefIdentifier() {
		LexElement id = createExpectedToken(DeeTokens.IDENTIFIER);
		return connect(new RefIdentifier(idTokenToString(id), sr(id.token)));
	}
	
	protected RefPrimitive parseRefPrimitive(DeeTokens primitiveType) {
		Token token = consumeLookAhead(primitiveType);
		return connect(new RefPrimitive(token, sr(token)));
	}
	
	protected RefModuleQualified parseRefModuleQualified() {
		int startPos = consumeLookAhead(DeeTokens.DOT).getStartPos();
		return connect(new RefModuleQualified(parseRefIdentifier(), srToCursor(startPos)));
	}
	
	protected static class RefParseResult { 
		public final Reference ref;
		public final boolean balanceBroken;
		
		public RefParseResult(boolean balanceBroken, Reference ref) {
			this.ref = ref;
			this.balanceBroken = balanceBroken;
		}
		public RefParseResult(Reference ref) {
			this(false, ref);
		}
	}
	
	public static String REFERENCE_RULE = "Reference";
	
	public Reference parseReference() {
		return parseReference_begin(false).ref;
	}
	
	public Reference parseReference_WithMissing(boolean reportMissingError) {
		Reference ref = parseReference();
		if(ref == null) {
			if(reportMissingError) {
				reportErrorExpectedRule(REFERENCE_RULE);
			}
			return createMissingRefIdentifier();
		}
		return ref;
	}
	
	public Reference parseReference(boolean expressionContext) {
		return parseReference_begin(expressionContext).ref;
	}
	
	protected RefParseResult parseReference_begin(boolean parsingExp) {
		DeeTokens la = lookAheadGrouped();
		
		switch (la) {
		case DOT: return parseReference_ReferenceStart(parseRefModuleQualified(), parsingExp);
		case IDENTIFIER: return parseReference_ReferenceStart(parseRefIdentifier(), parsingExp);
		case PRIMITIVE_KW: return parseReference_ReferenceStart(parseRefPrimitive(lookAhead()), parsingExp);
		
		default:
		return new RefParseResult(null);
		}
	}
	
	protected RefParseResult parseReference_ReferenceStart(Reference leftRef, boolean parsingExp) {
		boolean balanceBroken = false;
		
		// Star is multiply infix operator, dont parse as pointer ref
		if(lookAhead() == DeeTokens.DOT) {
			if(leftRef instanceof IQualifierNode == false) {
				addError(ParserErrorTypes.INVALID_QUALIFIER, leftRef.getSourceRange(), null);
				return new RefParseResult(leftRef);
			}
			consumeLookAhead();
			IQualifierNode qualifier = (IQualifierNode) leftRef;
			RefIdentifier qualifiedId = parseRefIdentifier();
			leftRef = connect(new RefQualified(qualifier, qualifiedId, srToCursor(leftRef.getStartPos())));
			balanceBroken = qualifiedId.name == null;
			
		} else if(lookAhead() == DeeTokens.NOT && leftRef instanceof ITemplateRefNode){ // template instance
			consumeLookAhead();
			
			ITemplateRefNode tplRef = (ITemplateRefNode) leftRef;
			ArrayList<Resolvable> tplArgs = null;
			Resolvable singleArg = null;
			
			if(tryConsume(DeeTokens.OPEN_PARENS)) {
				ArgumentListParseResult<Resolvable> argList = 
					parseArgumentList(true, DeeTokens.COMMA, DeeTokens.CLOSE_PARENS);
				tplArgs = argList.list;
				balanceBroken = !argList.properlyTerminated;
			} else {
				if(leftRef instanceof RefTemplateInstance) {
					RefTemplateInstance refTplInstance = (RefTemplateInstance) leftRef;
					if(refTplInstance.isSingleArgSyntax()) {
						addError(ParserErrorTypes.NO_CHAINED_TPL_SINGLE_ARG, refTplInstance.getSourceRange(), null);
					}
				}
				
				if(lookAheadGrouped() == DeeTokens.PRIMITIVE_KW) {
					singleArg = parseRefPrimitive(lookAhead());	
				} else if(lookAheadGrouped() == DeeTokens.IDENTIFIER) { 
					singleArg = parseRefIdentifier();
				} else {
					singleArg = parseSimpleLiteral();
					if(singleArg == null) {
						singleArg = createMissingExpression(true, TEMPLATE_SINGLE_ARG); 
					}
				}
			}
			leftRef = connect(new RefTemplateInstance(tplRef, singleArg, arrayView(tplArgs), srToCursor(leftRef)));
			
		} else if(!parsingExp && tryConsume(DeeTokens.STAR)) {
			leftRef = connect(new RefTypePointer(leftRef, srToCursor(leftRef.getStartPos())));
			
		} else if(!parsingExp && tryConsume(DeeTokens.OPEN_BRACKET)) {
			Resolvable resolvable = parseReferenceOrExpression(true);
			balanceBroken = consumeExpectedToken(DeeTokens.CLOSE_BRACKET) == null;
			
			if(resolvable == null) {
				leftRef = connect(new RefTypeDynArray(leftRef, srToCursor(leftRef.getStartPos())));
			} else {
				leftRef = connect(new RefIndexing(leftRef, resolvable, srToCursor(leftRef.getStartPos())));
			}
			
		} else {
			return new RefParseResult(leftRef);
		}
		if(balanceBroken)
			return new RefParseResult(true, leftRef);
		return parseReference_ReferenceStart(leftRef, parsingExp);
	}
	
	/* ----------------------------------------- */
	
	public static String EXPRESSION_RULE = "Expression";
	public static String REF_OR_EXP_RULE = "Reference or Expression";
	
	public static String TEMPLATE_SINGLE_ARG = "TemplateSingleArgument";
	
	public static int ANY_OPERATOR = 0;

	public Expression parseExpression() {
		return parseExpression(ANY_OPERATOR);
	}
	
	protected Expression parseExpression(int precedenceLimit) {
		return parseReferenceStartOrExpression(precedenceLimit, false, true).getExp_NoRuleContinue();
	}
	
	public Expression parseExpression_ToMissing(boolean reportMissingExpError) {
		return nullExpToMissing(parseExpression(), reportMissingExpError);
	}
	
	public Expression parseAssignExpression() {
		return parseExpression(InfixOpType.ASSIGN.precedence);
	}
	
	public Expression parseAssignExpression_toMissing(boolean reportMissingExpError) {
		return nullExpToMissing(parseAssignExpression(), reportMissingExpError);
	}
	
	public Resolvable parseReferenceOrExpression(boolean ambiguousToRef) {
		return parseReferenceOrExpression(ANY_OPERATOR, ambiguousToRef);
	}
	
	public Resolvable parseReferenceOrAssignExpression(boolean ambiguousToRef) {
		return parseReferenceOrExpression(InfixOpType.ASSIGN.precedence, ambiguousToRef);
	}
	
	public Resolvable parseReferenceOrExpression(int precedenceLimit, boolean ambiguousToRef) {
		RefOrExpFullResult refOrExp = parseReferenceOrExpression_full(precedenceLimit);
		if(refOrExp.mode == RefOrExpMode.REF_OR_EXP) {
			if(ambiguousToRef) {
				return convertRefOrExpToReference(refOrExp.getExpression());
			} else {
				return convertRefOrExpToExpression(refOrExp.getExpression());
			}
		}
		return refOrExp.resolvable;
	}
	
	protected Expression nullExpToMissing(Expression exp, boolean reportMissingExpError) {
		return exp != null ? exp : createMissingExpression(reportMissingExpError, EXPRESSION_RULE);
	}
	protected Resolvable nullRoEToMissing(Resolvable exp, boolean reportMissingExpError) {
		return exp != null ? exp : createMissingExpression(reportMissingExpError, REF_OR_EXP_RULE);
	}
	
	protected Expression createMissingExpression(boolean reportMissingExpError, String expectedRule) {
		if(reportMissingExpError) {
			reportError(ParserErrorTypes.EXPECTED_RULE, expectedRule, false);
		}
		int nodeStart = lastLexElement.getEndPos();
		return connect(new MissingExpression(srToCursor(nodeStart)));
	}
	
	protected RefOrExpFullResult parseReferenceOrExpression_full(int precedenceLimit) {
		// canBeRef will indicate whether the expression parsed so far could also have been parsed as a reference.
		// It is essential that every function call checks and updates the value of this variable before
		// consuming additional tokens from the stream.
		
		RefOrExpParse refOrExp = parseReferenceStartOrExpression(precedenceLimit, true, true);
		if(refOrExp.mode == null)
			return new RefOrExpFullResult(null, null);
		
		if(refOrExp.mode == RefOrExpMode.EXP) {
			return new RefOrExpFullResult(RefOrExpMode.EXP, refOrExp.getExp());
		} else if(refOrExp.mode == RefOrExpMode.REF ) {
			// The expression we parse should actually have been parsed as a reference, so convert it:
			Reference startRef = convertRefOrExpToReference(refOrExp.getExp_NoRuleContinue());
			// And resume parsing as ref
			Reference ref = parseReference_ReferenceStart(startRef, false).ref;
			return new RefOrExpFullResult(RefOrExpMode.REF, ref);
		} else {
			// Ambiguous RoE must not leave refs ahead (otherwise it should have been part of ambiguous)
			assertTrue(parseReference_ReferenceStart(null, false).ref == null); 
			return new RefOrExpFullResult(RefOrExpMode.REF_OR_EXP, refOrExp.getExp());
		}
	}
	
	public static enum RefOrExpMode { REF, EXP, REF_OR_EXP }
	
	protected static class RefOrExpParse {
		
		public final RefOrExpMode mode;
		public final boolean breakRule;
		private final Expression exp;
		
		public RefOrExpParse(RefOrExpMode mode, boolean breakRule, Expression exp) {
			this.mode = mode;
			this.breakRule = breakRule;
			this.exp = exp;
			assertTrue((mode == null) == (exp == null));
			if(exp != null) {
				assertTrue((exp.getData() == PARSED_STATUS) == (mode == RefOrExpMode.EXP));
			}
		}
		
		public boolean canBeRef() {
			assertTrue(mode != null && mode != RefOrExpMode.REF);
			return mode == RefOrExpMode.REF_OR_EXP;
		}
		
		public boolean shouldStopRule() {
			return mode == null || mode == RefOrExpMode.REF || breakRule;
		}
		
		public Expression getExp() {
			assertTrue(mode != null && mode != RefOrExpMode.REF);
			return exp;
		}
		
		/** Using this method means the RoE parsing will not continue consuming more tokens 
		 * (unless the mode is checked again). */
		public Expression getExp_NoRuleContinue() {
			return exp;
		}
		
	}
	
	protected RefOrExpParse refOrExp(RefOrExpMode mode, boolean breakRule, Expression exp) {
		return new RefOrExpParse(mode, breakRule, exp);
	}
	
	protected RefOrExpParse refOrExp(boolean canBeRef, boolean breakRule, Expression exp) {
		return refOrExp(canBeRef ? RefOrExpMode.REF_OR_EXP : RefOrExpMode.EXP, breakRule, exp);
	}
	
	protected RefOrExpParse expConnect(Expression exp) {
		return refOrExpConnect(RefOrExpMode.EXP, exp, null);
	}
	
	protected RefOrExpParse refConnect(Expression exp) {
		return refOrExpConnect(RefOrExpMode.REF, exp, null);
	}
	
	protected RefOrExpParse refOrExpConnect(boolean canBeRef, Expression exp) {
		return refOrExpConnect(canBeRef ? RefOrExpMode.REF_OR_EXP : RefOrExpMode.EXP, exp, null);
	}
	
	protected RefOrExpParse refOrExpConnect(RefOrExpMode mode, Expression exp, LexElement afterStarOp) {
		assertNotNull(mode);
		if(mode == RefOrExpMode.EXP) {
			if(exp.getData() != DeeParser_Decls.PARSED_STATUS) {
				exp = connect(exp);
			}
		} else { // This means the node must go through conversion process
			if(afterStarOp != null) {
				exp.setData(afterStarOp);
			} else {
				exp.setData(mode);
			}
		}
		return new RefOrExpParse(mode, false, exp);
	}
	
	protected boolean updateRefOrExpToExpression(boolean canBeRef, Expression leftExp, boolean newCanBeRef) {
		if(canBeRef && newCanBeRef == false && leftExp != null) {
			convertRefOrExpToExpression(leftExp);
		}
		return newCanBeRef;
	}
	
	protected class RefOrExpFullResult {
		public final RefOrExpMode mode;
		public final Resolvable resolvable;
		
		public RefOrExpFullResult(RefOrExpMode mode, Resolvable resolvable) {
			this.mode = mode;
			this.resolvable = resolvable;
			if(resolvable != null) {
				assertTrue((resolvable.getData() == PARSED_STATUS) == (mode != RefOrExpMode.REF_OR_EXP));
			}
		}
		
		public boolean isReference() {
			return mode == RefOrExpMode.REF;
		}
		
		public Expression getExpression() {
			assertTrue(mode != RefOrExpMode.REF);
			return (Expression) resolvable;
		}
		
		public Reference getReference() {
			assertTrue(isReference());
			return (Reference) resolvable;
		}
	}
	
	protected RefOrExpParse parseReferenceStartOrExpression_notAtStart(int precedenceLimit, boolean canBeRef) {
		return parseReferenceStartOrExpression(precedenceLimit, canBeRef, false);
	}
	
	protected RefOrExpParse parseReferenceStartOrExpression(int precedenceLimit, boolean canBeRef, boolean isAtStart) {
		RefOrExpParse refOrExp = parseUnaryExpression(canBeRef, isAtStart);
		if(refOrExp.shouldStopRule()) {
			return refOrExp;
		}
		
		return parseReferenceStartOrExpression_RoEStart(precedenceLimit, refOrExp.getExp(), refOrExp.canBeRef());
	}
	
	public Expression parseUnaryExpression() {
		return parseUnaryExpression(false).exp;
	}
	
	protected RefOrExpParse parseUnaryExpression(boolean canBeRef) {
		return parseUnaryExpression(canBeRef, false);
	}
	
	protected RefOrExpParse parseUnaryExpression(boolean canBeRef, boolean isRefOrExpStart) {
		RefOrExpParse prefixExp = parsePrefixExpression(canBeRef, isRefOrExpStart);
		if(prefixExp.shouldStopRule())
			return prefixExp;
		
		return parsePostfixExpression(prefixExp.getExp(), prefixExp.canBeRef());
	}
	
	protected RefOrExpParse parsePostfixExpression(Expression exp, boolean canBeRef) {
		switch (lookAheadGrouped()) {
		case DECREMENT:
		case INCREMENT: {
			updateRefOrExpToExpression(canBeRef, exp, false);
			RefOrExpParse refOrExp = expConnect(parsePostfixExpression(exp));
			return parsePostfixExpression(refOrExp.getExp(), false);
		}
		case OPEN_PARENS: {
			updateRefOrExpToExpression(canBeRef, exp, false);
			RefOrExpParse refOrExp = expConnect(parseCallExpression(exp));
			return parsePostfixExpression(refOrExp.getExp(), false);
		} case OPEN_BRACKET: {
			RefOrExpParse refOrExp = parseBracketList(exp, canBeRef);
			if(refOrExp.mode == RefOrExpMode.REF) {
				return refOrExp;
			}
			return parsePostfixExpression(refOrExp.getExp(), refOrExp.canBeRef());
		}
		case POW: {
			updateRefOrExpToExpression(canBeRef, exp, false);
			return parseInfixOperator(exp, InfixOpType.POW, InfixOpType.NULL, false);
		}
		case DOT: {
			assertTrue(canBeRef == false); // Because exp argument should be unambiguously an expression
			consumeLookAhead();
			RefIdentifier qualifiedId = parseRefIdentifier();
			// TODO: remove cast
			Reference ref = connect(new RefQualified((IQualifierNode)exp, qualifiedId, srToCursor(exp.getStartPos())));
			ref = parseReference_ReferenceStart(ref, true).ref; // continue parsing exp even with balance broken
			return parsePostfixExpression(connect(new ExpReference(ref, ref.getSourceRange())), false);
		}
		default:
			return refOrExp(canBeRef, false, exp);
		}
	}
	
	public Expression parseSimpleLiteral() {
		switch (lookAheadGrouped()) {
		case KW_TRUE: case KW_FALSE:
			Token token = consumeLookAhead();
			return connect(new ExpLiteralBool(token.type == DeeTokens.KW_TRUE, srToCursor(lastLexElement)));
		case KW_THIS:
			consumeLookAhead();
			return connect(new ExpThis(srToCursor(lastLexElement)));
		case KW_SUPER:
			consumeLookAhead();
			return connect(new ExpSuper(srToCursor(lastLexElement)));
		case KW_NULL:
			consumeLookAhead();
			return connect(new ExpNull(srToCursor(lastLexElement)));
		case DOLLAR:
			consumeLookAhead();
			return connect(new ExpArrayLength(srToCursor(lastLexElement)));
			
		case KW___LINE__:
			return connect(new ExpLiteralInteger(consumeLookAhead(), srToCursor(lastLexElement)));
		case KW___FILE__:
			return connect(new ExpLiteralString(consumeLookAhead(), srToCursor(lastLexElement)));
		case INTEGER:
			return connect(new ExpLiteralInteger(consumeLookAhead(), srToCursor(lastLexElement)));
		case CHARACTER: 
			return connect(new ExpLiteralChar(consumeLookAhead(), srToCursor(lastLexElement)));
		case FLOAT:
			return connect(new ExpLiteralFloat(consumeLookAhead(), srToCursor(lastLexElement)));
		case STRING:
			return parseStringLiteral();
		default:
			return null;
		}
	}
	
	protected RefOrExpParse parsePrefixExpression(boolean canBeRef, boolean isRefOrExpStart) {
		Expression simpleLiteral = parseSimpleLiteral();
		if(simpleLiteral != null) {
			return expConnect(simpleLiteral);
		}
		
		switch (lookAheadGrouped()) {
		case AND:
		case INCREMENT:
		case DECREMENT:
		case STAR:
		case MINUS:
		case PLUS:
		case NOT:
		case CONCAT:
		case KW_DELETE:
			Token prefixExpToken = consumeLookAhead();
			PrefixOpType prefixOpType = PrefixOpType.tokenToPrefixOpType(prefixExpToken.type);
			
			if(prefixExpToken.type == DeeTokens.STAR && canBeRef && !isRefOrExpStart) {
				
				LexElement data = lookAheadElement();
				RefOrExpParse refOrExp = parseUnaryExpression(canBeRef);
				RefOrExpMode mode = refOrExp.mode;
				
				if(refOrExp.mode == null) {
					mode = RefOrExpMode.REF;
				}
				
				Expression exp = refOrExp.getExp_NoRuleContinue();
				return refOrExpConnect(mode, new ExpPrefix(prefixOpType, exp, srToCursor(prefixExpToken)), data);
				
			} else {
				canBeRef = false;
				RefOrExpParse refOrExp = parseUnaryExpression(canBeRef);
				if(refOrExp.mode == null) {
					reportErrorExpectedRule(EXPRESSION_RULE);
				}
				
				return expConnect(new ExpPrefix(prefixOpType, refOrExp.exp, srToCursor(prefixExpToken)));
			}
			
		case OPEN_PARENS:
			return expConnect(parseParenthesesExp());
		case OPEN_BRACKET:
			return parseArrayLiteral(canBeRef);
		case KW_ASSERT:
			return expConnect(parseAssertExpression());
		case KW_MIXIN:
			return expConnect(parseMixinExpression());
		case KW_IMPORT:
			return expConnect(parseImportExpression());
		case KW_TYPEID:
			return expConnect(parseTypeIdExpression());
		case KW_NEW:
			return parseNewExpression();
		case KW_CAST:
			return parseCastExpression();
		default:
			Reference ref = parseReference(true); /*BUG here breakRule from ref*/
			if(ref == null) {
				return refOrExp(null, false, null);
			}
			
			if(isBuiltinTypeRef(ref)) {
				if(isRefOrExpStart && canBeRef) {
					return refConnect(new ExpReference(ref, ref.getSourceRange()));
				}
				addError(ParserErrorTypes.TYPE_USED_AS_EXP_VALUE, ref.getSourceRange(), null);
			}
			return refOrExpConnect(canBeRef && isRefOrExpStart, new ExpReference(ref, ref.getSourceRange()));
		}
	}
	
	protected static boolean isBuiltinTypeRef(Reference ref) {
		switch (ref.getNodeType()) {
		case REF_PRIMITIVE:
			return true;
		case REF_TYPE_DYN_ARRAY:
		case REF_INDEXING:
		case REF_TYPE_POINTER:
			throw assertFail();
		default:
			return false;
		}
	}
	
	public Expression parseArrayLiteral() {
		return parseArrayLiteral(false).getExp();
	}
	protected RefOrExpParse parseArrayLiteral(boolean canBeRef) {
		return parseBracketList(null, canBeRef);
	}
	
	protected RefOrExpParse parseBracketList(Expression calleeExp, boolean canBeRef) {
		if(tryConsume(DeeTokens.OPEN_BRACKET) == false)
			return refOrExp(null, false, null);
		int nodeStart = lastLexElement.getStartPos();
		
		final boolean isExpIndexing = calleeExp != null;
		final DeeTokens secondLA = isExpIndexing ? DeeTokens.DOUBLE_DOT : DeeTokens.COLON;
		
		ArrayList<Expression> elements = new ArrayList<Expression>();
		ArrayList<MapArrayLiteralKeyValue> mapElements = null;
		
		boolean firstElement = true;
		Expression firstExp = null;
		
		while(true) {
			Expression exp1;
			Expression exp2 = null;
			
			if(firstElement) {
				if(canBeRef) {
					RefOrExpFullResult refOrExp = parseReferenceOrExpression_full(InfixOpType.ASSIGN.precedence);
					if(refOrExp.isReference()) {
						elements.add(refConnect(new ExpReference(refOrExp.getReference(), null)).exp);
						consumeExpectedToken(DeeTokens.CLOSE_BRACKET);
						
						return refConnect(createBracketListNode(calleeExp, elements, nodeStart));
					} else {
						firstExp = exp1 = refOrExp.getExpression();
					}
				} else {
					exp1 = parseAssignExpression();
				}
				
				if(exp1 == null && lookAhead() != DeeTokens.COMMA && lookAhead() != secondLA) {
					consumeExpectedToken(DeeTokens.CLOSE_BRACKET);
					if(isExpIndexing) {
						return refOrExpConnect(canBeRef, new ExpSlice(calleeExp, srToCursor(calleeExp)));
					}
					break;
				}
				exp1 = nullExpToMissing(exp1, true);
				
				if(!isExpIndexing && tryConsume(DeeTokens.COLON)) {
					canBeRef = updateRefOrExpToExpression(canBeRef, firstExp, false);
					exp2 = parseAssignExpression_toMissing(true);
					mapElements = new ArrayList<MapArrayLiteralKeyValue>();
				} else if(isExpIndexing && tryConsume(DeeTokens.DOUBLE_DOT)) {
					updateRefOrExpToExpression(canBeRef, calleeExp, false);
					canBeRef = updateRefOrExpToExpression(canBeRef, firstExp, false);
					exp2 = parseAssignExpression_toMissing(true);
					
					consumeExpectedToken(DeeTokens.CLOSE_BRACKET);
					return refOrExpConnect(false, new ExpSlice(calleeExp, exp1, exp2, srToCursor(calleeExp)));
				}
			} else {
				exp1 = parseAssignExpression_toMissing(true);
				
				if(mapElements != null ) {
					assertTrue(canBeRef == false);
					if(consumeExpectedToken(DeeTokens.COLON) != null) {
						exp2 = parseAssignExpression_toMissing(true);
					}
				}
			}
			firstElement = false;
			
			if(mapElements == null ) {
				elements.add(exp1);
			} else {
				mapElements.add(connect(new MapArrayLiteralKeyValue(exp1, exp2, srToCursor(exp1.getStartPos()))));
			}
			
			if(tryConsume(DeeTokens.COMMA)) {
				updateRefOrExpToExpression(canBeRef, calleeExp, false);
				canBeRef = updateRefOrExpToExpression(canBeRef, firstExp, false);
				continue;
			}
			consumeExpectedToken(DeeTokens.CLOSE_BRACKET);
			break;
		}
		
		if(mapElements != null ) {
			return expConnect(new ExpLiteralMapArray(arrayView(mapElements), srToCursor(nodeStart)));
		}
		return refOrExpConnect(canBeRef, createBracketListNode(calleeExp, elements, nodeStart));
	}
	
	public Expression createBracketListNode(Expression calleeExp, ArrayList<Expression> elements, int nodeStart) {
		return calleeExp != null ?
			new ExpIndex(calleeExp, arrayView(elements), srToCursor(calleeExp)) :
			new ExpLiteralArray(arrayView(elements), srToCursor(nodeStart));
	}
	
	public ExpPostfix parsePostfixExpression(Expression exp) {
		Token op = consumeLookAhead();
		return new ExpPostfix(exp, PostfixOpType.tokenToPrefixOpType(op.type), srToCursor(exp));
	}
	
	protected RefOrExpParse parseReferenceStartOrExpression_RoEStart(int precedenceLimit, final Expression leftExp, 
		boolean canBeRef) {
		DeeTokens gla = lookAheadGrouped();
		
		InfixOpType infixOpLA = InfixOpType.tokenToInfixOpType(gla);
		if(lookAhead() == DeeTokens.NOT) {
			if(lookAheadElement(1).getType() == DeeTokens.KW_IS) {
				infixOpLA = InfixOpType.NOT_IS;
			} else if(lookAheadElement(1).getType() == DeeTokens.KW_IN) {
				infixOpLA = InfixOpType.NOT_IN;
			}
		}
		
		if(infixOpLA == null) {
			return refOrExp(canBeRef, false, leftExp);
		}
		
		// If lower precedence it can't be parsed to right expression, 
		// instead this expression must become left children of new parent
		if(infixOpLA.precedence < precedenceLimit) 
			return refOrExp(canBeRef, false, leftExp);
		
		if(infixOpLA != InfixOpType.MUL) {
			canBeRef = updateRefOrExpToExpression(canBeRef, leftExp, false);
		}
		
		Expression newLeftExp = null;
		switch (infixOpLA.category) {
		case COMMA:
			newLeftExp = parseInfixOperator(leftExp, infixOpLA, InfixOpType.COMMA);
			break;
		case ASSIGN:
			newLeftExp = parseInfixOperator(leftExp, infixOpLA, InfixOpType.ASSIGN);
			break;
		case CONDITIONAL:
			newLeftExp = parseInfixOperator(leftExp, infixOpLA, InfixOpType.CONDITIONAL);
			break;
		case LOGICAL_OR:
			newLeftExp = parseInfixOperator(leftExp, infixOpLA, InfixOpType.LOGICAL_AND);
			break;
		case LOGICAL_AND: 
			newLeftExp = parseInfixOperator(leftExp, infixOpLA, InfixOpType.OR);
			break;
		case OR:
			newLeftExp = parseInfixOperator(leftExp, infixOpLA, InfixOpType.XOR);
			break;
		case XOR:
			newLeftExp = parseInfixOperator(leftExp, infixOpLA, InfixOpType.AND);
			break;
		case AND:
			newLeftExp = parseInfixOperator(leftExp, infixOpLA, InfixOpType.EQUALS);
			break;
		case EQUALS:
			newLeftExp = parseInfixOperator(leftExp, infixOpLA, InfixOpType.SHIFT);
			break;
		case SHIFT:
			newLeftExp = parseInfixOperator(leftExp, infixOpLA, InfixOpType.ADD);
			break;
		case ADD:
			newLeftExp = parseInfixOperator(leftExp, infixOpLA, InfixOpType.MUL);
			break;
		case MUL:
			RefOrExpParse refOrExp = parseInfixOperator(leftExp, infixOpLA, InfixOpType.NULL, canBeRef);
			if(refOrExp.mode == RefOrExpMode.REF) {
				return refOrExp;
			}
			canBeRef = refOrExp.canBeRef();
			
			newLeftExp = refOrExp.getExp();
			break;
		default:
			assertUnreachable();
		}
		assertTrue(newLeftExp != null);
		return parseReferenceStartOrExpression_RoEStart(precedenceLimit, newLeftExp, canBeRef);
	}
	
	public Expression parseInfixOperator(Expression leftExp, InfixOpType op, InfixOpType rightExpLimitToken) {
		return parseInfixOperator(leftExp, op, rightExpLimitToken, false).getExp();
	}
	
	public RefOrExpParse parseInfixOperator(Expression leftExp, InfixOpType op, InfixOpType rightExpLimit,
		final boolean canBeRef) {
		
		consumeLookAhead();
		
		LexElement afterStarOp = null;
		if(canBeRef) {
			assertTrue(lastLexElement.getType() == DeeTokens.STAR);
			afterStarOp = lookAheadElement();
		}
		
		if(op == InfixOpType.NOT_IS || op == InfixOpType.NOT_IN) {
			consumeLookAhead(); // consume second infix token
		}
		checkValidAssociativity(leftExp, op, canBeRef);
		
		Expression middleExp = null;
		Expression rightExp = null;
		RefOrExpMode mode = null;
		
		parsing: {
			if(op == InfixOpType.CONDITIONAL) {
				middleExp = parseExpression();
				if(middleExp == null) {
					reportErrorExpectedRule(EXPRESSION_RULE);
				}
				if(consumeExpectedToken(DeeTokens.COLON) == null) {
					break parsing;
				}
			}
			
			RefOrExpParse rightExpResult 
				= parseReferenceStartOrExpression_notAtStart(rightExpLimit.precedence, canBeRef);
			mode = rightExpResult.mode;
			
			if(rightExpResult.mode == null) {
				if(canBeRef) {
					mode = RefOrExpMode.REF;
				} else {
					mode = RefOrExpMode.EXP;
					reportErrorExpectedRule(EXPRESSION_RULE);
				}
				break parsing;
			}
			updateRefOrExpToExpression(canBeRef, leftExp, mode != RefOrExpMode.EXP);
			
			rightExp = rightExpResult.getExp_NoRuleContinue();
			checkValidAssociativity(rightExp, op, canBeRef);
		}
		
		if(op == InfixOpType.CONDITIONAL) {
			return expConnect(new ExpConditional(leftExp, middleExp, rightExp, srToCursor(leftExp)));
		}
		
		return refOrExpConnect(mode, new ExpInfix(leftExp, op, rightExp, srToCursor(leftExp)), afterStarOp);
	}
	
	protected void checkValidAssociativity(Expression exp, InfixOpType op, boolean canBeRef) {
		// Check for some syntax situations which are technically not allowed by the grammar:
		switch (op.category) {
		case OR: case XOR: case AND: case EQUALS:
			if(exp instanceof ExpInfix) {
				assertTrue(canBeRef == false);
				if(((ExpInfix) exp).kind.category == InfixOpType.EQUALS) {
					addError(ParserErrorTypes.EXP_MUST_HAVE_PARENTHESES, exp.getSourceRange(), op.sourceValue);
				}
			}
		default: break;
		}
	}
	
	protected Expression convertRefOrExpToExpression(Expression exp) {
		exp.accept(new ASTDefaultVisitor() {
			@Override
			public boolean preVisit(ASTNeoNode node) {
				if(node.getData() == PARSED_STATUS) {
					return false;
				}
				switch (node.getNodeType()) {
				case EXP_INFIX:
				case EXP_PREFIX:
					node.removeData(LexElement.class);
					break;
				case EXP_REFERENCE:
				case EXP_LITERAL_ARRAY:
				case EXP_SLICE:
				case EXP_INDEX:
					node.removeData(RefOrExpMode.class);
					break;
				default:
					throw assertFail();
				}
				node.setData(DeeParser_Decls.PARSED_STATUS);
				return true;
			}
		});
		return exp;
	}
	
	protected Reference convertRefOrExpToReference(Expression exp) {
		return convertRefOrExpToReference(null, exp);
	}
	
	protected Reference convertRefOrExpToReference(Reference leftRef, Expression exp) {
		if(exp == null) {
			return leftRef;
		}
		
		switch (exp.getNodeType()) {
		case EXP_REFERENCE: {
			assertTrue(leftRef == null);
			
			exp.resetData(null);
			Reference ref = ((ExpReference) exp).ref;
			ref.detachFromParent();
			return ref;
		}
		case EXP_INFIX: {
			ExpInfix expInfix = (ExpInfix) exp;
			assertTrue(leftRef == null);
			
			assertTrue(expInfix.kind == InfixOpType.MUL);
			leftRef = convertRefOrExpToReference(expInfix.leftExp);
			
			assertTrue(expInfix.getData() instanceof LexElement);
			LexElement afterStarToken = (LexElement) expInfix.getData();
			SourceRange sr = srNodeStart(leftRef, afterStarToken.getFullRangeStartPos());
			
			leftRef = connect(new RefTypePointer(leftRef, sr));
			
			return convertRefOrExpToReference(leftRef, expInfix.rightExp);
		}
		case EXP_INDEX: {
			ExpIndex expIndex = (ExpIndex) exp;
			assertTrue(expIndex.args.size() == 1);
			
			leftRef = convertRefOrExpToReference(leftRef, expIndex.indexee);
			return convertToRefIndexing(leftRef, expIndex, expIndex.args.get(0));
		}
		case EXP_SLICE: {
			ExpSlice expSlice = (ExpSlice) exp;
			assertTrue(expSlice.from == null && expSlice.to == null);
			
			leftRef = convertRefOrExpToReference(leftRef, expSlice.slicee);
			return connect(new RefTypeDynArray(leftRef, srNodeStart(leftRef, expSlice.getEndPos())));
		}
		case EXP_PREFIX: {
			assertTrue(leftRef != null);
			ExpPrefix expPrefix = (ExpPrefix) exp;
			assertTrue(expPrefix.kind == PrefixOpType.REFERENCE);
			
			LexElement afterStarToken = assertNotNull_((LexElement) expPrefix.getData());
			SourceRange sr = srNodeStart(leftRef, afterStarToken.getFullRangeStartPos());
			
			leftRef = connect(new RefTypePointer(leftRef, sr));
			return convertRefOrExpToReference(leftRef, expPrefix.exp);
		}
		case EXP_LITERAL_ARRAY: {
			assertTrue(leftRef != null);
			ExpLiteralArray expLiteralArray = (ExpLiteralArray) exp;
			assertTrue(expLiteralArray.getData() instanceof RefOrExpMode);
			
			assertTrue(expLiteralArray.elements.size() <= 1);
			if(expLiteralArray.elements.size() == 0) {
				return connect(new RefTypeDynArray(leftRef, srNodeStart(leftRef, exp.getEndPos())));
			} else {
				return convertToRefIndexing(leftRef, expLiteralArray, expLiteralArray.elements.get(0));
			}
		}
		
		default:
			throw assertFail();
		}
	}
	
	public Reference convertToRefIndexing(Reference leftRef, Expression exp, Expression indexArgExp) {
		Resolvable indexArg;
		if(indexArgExp.getData() == RefOrExpMode.REF) {
			// argument can only be interpreted as reference
			indexArg = ((ExpReference) indexArgExp).ref;
			indexArg.detachFromParent();
		} else if(indexArgExp.getData() == DeeParser_Decls.PARSED_STATUS) {
			// argument can only be interpreted as expression
			indexArg = indexArgExp;
			indexArg.detachFromParent();
		} else if(indexArgExp.getData() == RefOrExpMode.REF_OR_EXP || indexArgExp.getData() instanceof LexElement) {
			// argument is ambiguous, so convert it to reference
			indexArg = convertRefOrExpToReference(indexArgExp);
		} else {
			throw assertFail();
		}
		
		return connect(new RefIndexing(leftRef, indexArg, srNodeStart(leftRef, exp.getEndPos())));
	}
	
	public Expression parseStringLiteral() {
		ArrayList<Token> stringTokens = new ArrayList<Token>();
		
		while(lookAheadGrouped() == DeeTokens.STRING) {
			Token string = consumeLookAhead();
			stringTokens.add(string);
		}
		Token[] tokenStrings = ArrayUtil.createFrom(stringTokens, Token.class);
		return connect(new ExpLiteralString(tokenStrings, srToCursor(tokenStrings[0])));
	}
	
	protected ExpCall parseCallExpression(Expression callee) {
		consumeLookAhead(DeeTokens.OPEN_PARENS);
		
		ArrayList<Expression> args = parseExpArgumentList(DeeTokens.CLOSE_PARENS).list;
		return connect(new ExpCall(callee, arrayView(args), srToCursor(callee)));
	}
	
	public static class ArgumentListParseResult<T> {
		public final ArrayList<T> list;
		public final boolean properlyTerminated;
		
		public ArgumentListParseResult(ArrayList<T> argList, boolean properlyTerminated) {
			this.list = argList;
			this.properlyTerminated = properlyTerminated;
		}
	}
	
	protected ArgumentListParseResult<Expression> parseExpArgumentList(DeeTokens tokenLISTCLOSE) {
		return CoreUtil.blindCast(parseArgumentList(false, DeeTokens.COMMA, tokenLISTCLOSE));
	}
	protected ArgumentListParseResult<Resolvable> parseArgumentList(boolean parseRefOrExp, 
		DeeTokens tokenSEPARATOR, DeeTokens tokenLISTCLOSE) {
		
		ArrayList<Resolvable> args = new ArrayList<Resolvable>();
		
		boolean first = true;
		while(true) {
			Resolvable arg = parseRefOrExp ? parseReferenceOrAssignExpression(true) : parseAssignExpression();
			
			if(first && arg == null && lookAhead() != tokenSEPARATOR) {
				break;
			}
			arg = parseRefOrExp ? nullRoEToMissing(arg, true) : nullExpToMissing((Expression) arg, true);
			args.add(arg);
			first = false;
			
			if(tryConsume(tokenSEPARATOR)) {
				continue;
			}
			break;
		}
		boolean properlyTerminated = consumeExpectedToken(tokenLISTCLOSE) != null;
		return new ArgumentListParseResult<Resolvable>(args, properlyTerminated);
	}
	
	public Expression parseParenthesesExp() {
		if(!tryConsume(DeeTokens.OPEN_PARENS))
			return null;
		int nodeStart = lastLexElement.getStartPos();
		
		Resolvable resolvable = parseReferenceOrExpression(false);
		if(resolvable == null) {
			resolvable = nullExpToMissing((Expression) resolvable, true);
		}
		
		if(consumeExpectedToken(DeeTokens.CLOSE_PARENS) != null) {
			if(resolvable instanceof Reference && lookAhead() != DeeTokens.DOT) {
				addError(ParserErrorTypes.TYPE_USED_AS_EXP_VALUE, resolvable.getSourceRange(), null);
			}
		}
		
		return connect(new ExpParentheses(resolvable, srToCursor(nodeStart)));
	}
	
	public ExpAssert parseAssertExpression() {
		if(tryConsume(DeeTokens.KW_ASSERT) == false)
			return null;
		
		int nodeStart = lastLexElement.getStartPos();
		Expression exp = null;
		Expression msg = null;
		if(consumeExpectedToken(DeeTokens.OPEN_PARENS) != null) {
			exp = parseAssignExpression_toMissing(true);
			if(tryConsume(DeeTokens.COMMA)) {
				msg = parseAssignExpression_toMissing(true);
			}
			consumeExpectedToken(DeeTokens.CLOSE_PARENS);
		}
		
		return connect(new ExpAssert(exp, msg, srToCursor(nodeStart)));
	}
	
	public ExpImportString parseImportExpression() {
		if(tryConsume(DeeTokens.KW_IMPORT) == false)
			return null;
		
		int nodeStart = lastLexElement.getStartPos();
		Expression exp = parseAssignExpressionWithParens();
		return connect(new ExpImportString(exp, srToCursor(nodeStart)));
	}
	
	public ExpMixinString parseMixinExpression() {
		if(tryConsume(DeeTokens.KW_MIXIN) == false)
			return null;
		
		int nodeStart = lastLexElement.getStartPos();
		Expression exp = parseAssignExpressionWithParens();
		return connect(new ExpMixinString(exp, srToCursor(nodeStart)));
	}
	
	protected Expression parseAssignExpressionWithParens() {
		Expression exp = null;
		if(consumeExpectedToken(DeeTokens.OPEN_PARENS) != null) {
			exp = parseAssignExpression_toMissing(true);
			consumeExpectedToken(DeeTokens.CLOSE_PARENS);
		}
		return exp;
	}
	
	public ExpTypeId parseTypeIdExpression() {
		if(tryConsume(DeeTokens.KW_TYPEID) == false)
			return null;
		
		int nodeStart = lastLexElement.getStartPos();
		Reference ref = null;
		Expression exp = null;
		if(consumeExpectedToken(DeeTokens.OPEN_PARENS) != null) {
			Resolvable resolvable = nullRoEToMissing(parseReferenceOrExpression(true), true);
			if(resolvable instanceof Reference) {
				ref = (Reference) resolvable;
			} else {
				exp = (Expression) resolvable;
			}
			consumeExpectedToken(DeeTokens.CLOSE_PARENS);
		}
		if(ref != null) {
			return connect(new ExpTypeId(ref, srToCursor(nodeStart)));
		}
		return connect(new ExpTypeId(exp, srToCursor(nodeStart)));
	}
	
	public RefOrExpParse parseNewExpression() {
		if(!tryConsume(DeeTokens.KW_NEW))
			return null;
		
		int nodeStart = lastLexElement.getStartPos();
		
		ArrayList<Expression> allocArgs = null;
		Reference type = null;
		ArrayList<Expression> args = null;
		
		parsing: {
			if(tryConsume(DeeTokens.OPEN_PARENS)) {
				ArgumentListParseResult<Expression> allocArgsResult = parseExpArgumentList(DeeTokens.CLOSE_PARENS);
				allocArgs = allocArgsResult.list;
				if(!allocArgsResult.properlyTerminated) {
					break parsing;
				}
			}
			type = parseReference_WithMissing(true);
			if(lastLexElement.isMissingElement()) {
				break parsing;
			}
			if(tryConsume(DeeTokens.OPEN_PARENS)) {
				args = parseExpArgumentList(DeeTokens.CLOSE_PARENS).list;
			}
		}
		
		return expConnect(new ExpNew(arrayView(allocArgs), type, arrayView(args), srToCursor(nodeStart)));
	}
	
	public RefOrExpParse parseCastExpression() {
		if(!tryConsume(DeeTokens.KW_CAST))
			return null;
		
		int nodeStart = lastLexElement.getStartPos();
		
		Reference type = null;
		CastQualifiers qualifier = null;
		Expression exp = null;
		
		parsing: {
			if(consumeExpectedToken(DeeTokens.OPEN_PARENS) == null)
				break parsing;
			
			qualifier = parseQualifier();
			if(qualifier == null) {
				type = parseReference_WithMissing(false);
			}
			if(consumeExpectedToken(DeeTokens.CLOSE_PARENS) == null)
				break parsing;
			
			exp = nullExpToMissing(parseUnaryExpression(), true);
		}
		
		if(qualifier != null) {
			return expConnect(new ExpCastQual(qualifier, exp, srToCursor(nodeStart)));
		} else {
			return expConnect(new ExpCast(type, exp, srToCursor(nodeStart)));
		}
	}
	
	public CastQualifiers parseQualifier() {
		switch (lookAhead()) {
		case KW_CONST:
			consumeInput();
			if(tryConsume(DeeTokens.KW_SHARED))
				return CastQualifiers.CONST_SHARED;
			return CastQualifiers.CONST;
		case KW_INOUT:
			consumeInput();
			if(tryConsume(DeeTokens.KW_SHARED))
				return CastQualifiers.INOUT_SHARED;
			return CastQualifiers.INOUT;
		case KW_SHARED:
			consumeInput();
			if(tryConsume(DeeTokens.KW_CONST))
				return CastQualifiers.SHARED_CONST;
			if(tryConsume(DeeTokens.KW_INOUT))
				return CastQualifiers.SHARED_INOUT;
			return CastQualifiers.SHARED;
		case KW_IMMUTABLE:
			consumeInput();
			return CastQualifiers.IMMUTABLE;
		default:
			return null;
		}
	}
	
}