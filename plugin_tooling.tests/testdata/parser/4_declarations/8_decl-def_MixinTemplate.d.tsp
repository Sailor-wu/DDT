▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂ basic case 
mixin foo!(bar);
#STRUCTURE_EXPECTED:
DeclarationMixin(RefTemplateInstance(RefIdentifier RefIdentifier))

▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂ 
#PARSE(DECLARATION)   mixin #@NO_TYPEREF #@SEMICOLON_OR_BREAK
#STRUCTURE_EXPECTED:  DeclarationMixin(#@NO_TYPEREF)
▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂ 
#PARSE(DECLARATION)   mixin #@TYPE_REFS #@SEMICOLON_OR_BREAK
#STRUCTURE_EXPECTED:  DeclarationMixin(#@TYPE_REFS)
▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂ 
#PARSE(DECLARATION)   mixin #@TYPE_REFS__NO_BROKEN id #@SEMICOLON_OR_BREAK
#STRUCTURE_EXPECTED:  DefinitionMixinInstance(#@TYPE_REFS__NO_BROKEN DefSymbol)

▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂ 
#PARSE(DECLARATION)   mixin #@TYPEREF_BROKEN__NoTypeAppend #error(EXP_SEMICOLON) #@BREAK id ;
#STRUCTURE_EXPECTED:  DeclarationMixin(#@TYPEREF_BROKEN__NoTypeAppend)
