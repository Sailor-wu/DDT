Ⓗ▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂ 

#@TYPE_AS_EXP《#?STRUCTURE_EXPECTED!【int●ExpReference(RefPrimitive)】》
 #@INT_AS_EXP《#?STRUCTURE_EXPECTED!【int●ExpReference(RefPrimitive)】》

#@EXP_PRIMARY_SIMPLE《
  ►#?STRUCTURE_EXPECTED!【this●ExpThis】●
  ►#?STRUCTURE_EXPECTED!【false●ExpLiteralBool】●
  ►#?STRUCTURE_EXPECTED!【$●ExpArrayLength】●
  ►#?STRUCTURE_EXPECTED!【'"'●ExpLiteralChar】●
  ►#?STRUCTURE_EXPECTED!【12●Integer】●
  ►#?STRUCTURE_EXPECTED!【0x25_AD_3FP+1●Float】●
  ►#?STRUCTURE_EXPECTED!【123.0F●Float】●
  ►#?STRUCTURE_EXPECTED!【r"inline"`sfds`q{ TOKEN string }●String】●
  
  ►#?STRUCTURE_EXPECTED!【__FILE__●ExpLiteralString】●
  ►#?STRUCTURE_EXPECTED!【__LINE__●ExpLiteralInteger】●
¤》

#@EXP_POSTFIX《
  ►#@EXP_PRIMARY_SIMPLE●
  
  ►#?STRUCTURE_EXPECTED!【#EXP_ARRAY_LITERAL(flag) [123] ●ExpLiteralArray(Integer)】●
  ►#?STRUCTURE_EXPECTED!【#EXP_ARRAY_LITERAL(flag) [1, .456E12, 0x25_AD_3FP+1] ●ExpLiteralArray(Integer Float Float)】●
  ►#?STRUCTURE_EXPECTED!【#EXP_ARRAY_LITERAL(flag) [12345 : true, 66.6 : false, "asdfd" : "false"]●
  ExpLiteralMapArray(MapEntry(Integer Bool) MapEntry(Float Bool) MapEntry(String String))】●
  
  ►#?STRUCTURE_EXPECTED!【(123 ,"asd")●ExpParentheses(ExpInfix(Integer String))】●
  
  ►#?STRUCTURE_EXPECTED!【assert(2 + 2 == true, "error")●ExpAssert(* String)】●
  ►#?STRUCTURE_EXPECTED!【mixin("2 + " ~ "2") #MIXIN_EXP(flag)●ExpMixinString(*)】●
  ►#?STRUCTURE_EXPECTED!【import("testdata/samples.txt") ●ExpImportString(String)】●
  
  ►#?STRUCTURE_EXPECTED!【function(int a) { return; } ●ExpFunctionLiteral(* *) 】●
  ►#?STRUCTURE_EXPECTED!【{ ; } #BLOCK_ST_LIKE_EXP(flag) ●ExpFunctionLiteral( BlockStatement(EmptyStatement) )】●
  ►#?STRUCTURE_EXPECTED!【(int a, ...) { return a; }●ExpFunctionLiteral(* * BlockStatement(StatementReturn(*)))】●
  
  
  ►#?STRUCTURE_EXPECTED!【foo++●                 ExpPostfix(#@ExpIdentifier)】●
  ►#?STRUCTURE_EXPECTED!【typeof(foo)(1, 3)●             ExpCall(ExpReference(RefTypeof(#@ExpIdentifier)) Integer Integer)】●

  ►#?STRUCTURE_EXPECTED!【is(foo : bar[])●ExpIs(RefIdentifier RefTypeDynArray(RefIdentifier))】●
  ►#?STRUCTURE_EXPECTED!【__traits(isStaticFunction, myfn, int, 123)●ExpTraits(Symbol RefIdentifier RefPrimitive Integer)】●
  
  ►#?STRUCTURE_EXPECTED!【 (int[]).init●ExpReference(RefQualified(ExpParentheses(RefTypeDynArray(*)) ?))】●
  ►#?STRUCTURE_EXPECTED!【 (.foo).bar.xxx●ExpReference(RefQualified(?(RefQualified(ExpParentheses(*) ?)) ?))】●
  
  
  ►#?STRUCTURE_EXPECTED!【foo[[123]]#_EXP_AMBIG(flag)●ExpIndex(#@ExpIdentifier ExpLiteralArray(?))】●
  
  ►#?STRUCTURE_EXPECTED!【 foo().init●ExpReference(RefQualified(ExpCall(#@ExpIdentifier) RefIdentifier))】●
  
  ►#?STRUCTURE_EXPECTED!【 foz++.func!(blah)● #?TPL_NO_EXPREF_WRAP!【ExpReference(】
    RefTemplateInstance( RefQualified(ExpPostfix(#@ExpIdentifier) RefIdentifier) RefIdentifier) 
  #?TPL_NO_EXPREF_WRAP!【)】】●
  
  ►#@EXP_UNARY_REFS●  
¤》
#@EXP_POSTFIX__NO_POSSIBLE_REFS《 #@EXP_POSTFIX #?_EXP_AMBIG{#:DISCARD_CASE}》


EXP__NO_PENDING_MISSING : exclude cases that could have been parsed further if an EXP was appended
_EXP_AMBIG : marker for cases that can be cleanly parsed as refs

#@EXP_UNARY_REFS《
  ►#?STRUCTURE_EXPECTED!【#_EXP_AMBIG(flag) foo ● ExpReference(RefIdentifier)】●
  ►#?STRUCTURE_EXPECTED!【#_EXP_AMBIG(flag) #@INT_AS_EXP ● #@INT_AS_EXP 】●

  ►#?STRUCTURE_EXPECTED!【#_EXP_AMBIG(flag) .foo ● ExpReference(RefModuleQualified(RefIdentifier))】●
  ►#?STRUCTURE_EXPECTED!【#_EXP_AMBIG(flag)  .foo.bar ● ExpReference(RefQualified(RefModuleQualified(?) RefIdentifier))】●
  
  ►#?STRUCTURE_EXPECTED!【#_EXP_AMBIG(flag) foo!(int)●ExpReference(RefTemplateInstance(RefIdentifier RefPrimitive) )】●
  ►#?STRUCTURE_EXPECTED!【#_EXP_AMBIG(flag) foo[]● ExpIndex(#@ExpIdentifier)】●
  
  ►#?STRUCTURE_EXPECTED!【#_EXP_AMBIG(flag) int[]   ●ExpIndex(#@INT_AS_EXP)】●
  ►#?STRUCTURE_EXPECTED!【#_EXP_AMBIG(flag) foo[int]●ExpIndex(#@ExpIdentifier #@INT_AS_EXP)】●
  ►#?STRUCTURE_EXPECTED!【#_EXP_AMBIG(flag) int[foo]●ExpIndex(#@INT_AS_EXP #@ExpIdentifier)】●
¤》

#@EXP_NON_UNARY_REFS《
  ►#?STRUCTURE_EXPECTED!【#_EXP_AMBIG(flag) foo * #@NO_EXP #EXP_RULE_BROKEN(flag)● 
    ExpInfix(ExpReference(RefIdentifier) #@NO_EXP)】●

  ►#?STRUCTURE_EXPECTED!【#_EXP_AMBIG(flag) float * #@NO_EXP #EXP_RULE_BROKEN(flag)● 
    ExpInfix(ExpReference(RefPrimitive) #@NO_EXP)】●
  
  ►#?STRUCTURE_EXPECTED!【#_EXP_AMBIG(flag) foo**[bar* #@NO_EXP] ● ExpInfix(#@ExpIdentifier ExpPrefix(*))】●
¤》

#@EXP_REFS《
  ►#@EXP_UNARY_REFS●
  ►#@EXP_NON_UNARY_REFS●
  ►#?STRUCTURE_EXPECTED!【foo[] #@_EXP_REFS_BREAK function(bar) ● ExpIndex(#@ExpIdentifier) #@BREAK】●
¤》

#@EXP_UNARY《
  ►#@EXP_POSTFIX●
  
  ►#?STRUCTURE_EXPECTED!【123 ^^ exp●            ExpInfix(Integer #@ExpIdentifier)】●
  ►#?STRUCTURE_EXPECTED!【new(123, foo) .bar.Foo(+1, foo[123])●ExpNew(? #@ExpIdentifier RefQualified(* *) * *)】●
  ►#?STRUCTURE_EXPECTED!【cast(Foo) foo●         ExpCast(RefIdentifier #@ExpIdentifier)】●

  ►#?STRUCTURE_EXPECTED!【*foo[]●      ExpPrefix(ExpIndex(#@ExpIdentifier))】●
  ►#?STRUCTURE_EXPECTED!【!foo[[123]]● ExpPrefix(ExpIndex(#@ExpIdentifier ExpLiteralArray(?)))】●
¤》
#@EXP_UNARY__LITE《
  ►#?STRUCTURE_EXPECTED!【!foo[[123]]● ExpPrefix(ExpIndex(#@ExpIdentifier ExpLiteralArray(?)))】●
¤》
#@EXP_UNARY__NO_POSSIBLE_REFS《 #@EXP_UNARY #?_EXP_AMBIG{#:DISCARD_CASE}》
#@EXP_UNARY__NO_PENDING《#EXP__NO_PENDING_MISSING(flag) #?STRUCTURE_EXPECTED!【/*UNARY_NO_PENDING*/】#@EXP_UNARY 》


#@EXP_OROR《
  ►#@EXP_UNARY●
   ►#?EXP__NO_PENDING_MISSING【#:DISCARD_CASE●
  ►#@EXP_NON_UNARY_REFS】●
  
  ►#?STRUCTURE_EXPECTED!【4 / 6●ExpInfix(? ?)】●
  ►#?STRUCTURE_EXPECTED!【1 + 2●ExpInfix(? ?)】●
  ►#?STRUCTURE_EXPECTED!【1 << 16●ExpInfix(? ?)】●
  ►#?STRUCTURE_EXPECTED!【0xFF & 123●ExpInfix(? ?)】●
  ►#?STRUCTURE_EXPECTED!【0xFF | 0xAA●ExpInfix(? ?)】●
  ►#?STRUCTURE_EXPECTED!【1 > "2" && 3●ExpInfix(ExpInfix(? String) ?)】●
  ►#?STRUCTURE_EXPECTED!【2 || "3" < 4●ExpInfix(? ExpInfix(String ?))】●
  ►#?STRUCTURE_EXPECTED!【foo *** ptr #EXP__DECL_AMBIG(flag)●ExpInfix(ExpReference(?) ExpPrefix(ExpPrefix(ExpReference(RefIdentifier))) )】●
  
  ►#@EXP_OROR__LITE●
¤》
#@EXP_OROR__LITE《
  ►#?STRUCTURE_EXPECTED!【2 ~ [1, 0xFF, "3"] == null ~ [123 : "entry"] || assert(2 < "four" + length)●
ExpInfix(
  ExpInfix(  ExpInfix(Integer ExpLiteralArray(* * *))   ExpInfix(ExpNull ExpLiteralMapArray(*))  )
  ExpAssert(ExpInfix(Integer ExpInfix(String ExpReference(RefIdentifier))))
)】●
¤》

#@EXP_CONDITIONAL《#@EXP_OROR●#@EXP_CONDITIONAL__LITE》
#@EXP_CONDITIONAL__LITE《
  ►#?STRUCTURE_EXPECTED!【false ? 123 : 456●ExpConditional(Bool Integer Integer)】●
¤》
#@EXP_COND《#@EXP_CONDITIONAL》
#@EXP_COND__LITE《#@EXP_CONDITIONAL__LITE》

Lambda expression is here because it's right side is an "open-ended" parsing of assign expression
#@EXP_ASSIGN《
  ►#@EXP_CONDITIONAL●
  ►#@EXP_ASSIGN__LITE●
  ►#?STRUCTURE_EXPECTED!【foo => foo + 2●ExpSimpleLambda(SimpleLambdaDefUnit(DefSymbol) ExpInfix(* *))】●
¤》
#@EXP_ASSIGN__LITE《
  ►#?STRUCTURE_EXPECTED!【this = super += null●ExpInfix(ExpThis ExpInfix(ExpSuper ExpNull))】●
¤》
#@ASSIGNEXP《#@EXP_ASSIGN》 // The previous name is deprecated
#@ASSIGNEXP__LITE《#@EXP_ASSIGN__LITE》


#@EXP_COMMA《
  ►#@EXP_ASSIGN●
  ►#@EXP_CONDITIONAL__LITE●
  ►#@EXP_COMMA__LITE●
¤》

#@EXP_COMMA__LITE《
  ►#?STRUCTURE_EXPECTED!【12,"asd"●ExpInfix(Integer String)】●
  ►#?STRUCTURE_EXPECTED!【7 .. [123]●ExpInfix(Integer ExpLiteralArray(?))】●
¤》


#@EXP_NO_PENDING《 #EXP__NO_PENDING_MISSING(flag) #@EXP_COMMA 》

  #@EXP_COND__NO_POSSIBLE_REFS《 #@EXP_COND   #?_EXP_AMBIG{#:DISCARD_CASE}》
#@EXP_ASSIGN__NO_POSSIBLE_REFS《 #@EXP_ASSIGN #?_EXP_AMBIG{#:DISCARD_CASE}》
      #@EXPS__NO_POSSIBLE_REFS《 #@EXP_COMMA  #?_EXP_AMBIG{#:DISCARD_CASE}》

#@EXP_ASSIGN__NO_PENDING《 #EXP__NO_PENDING_MISSING(flag) #@EXP_ASSIGN 》   
