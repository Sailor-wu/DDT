▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂  Single line Comment
//+ asdf
///*
asdf
//+ /*    
aaaa
// asdf
// asdf
// asdf#LEXERTEST:
COMMENT_LINE,DOCCOMMENT_LINE,ID,EOL,COMMENT_LINE,ID,EOL,
COMMENT_LINE,COMMENT_LINE,COMMENT_LINE,
▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂  ending with EOF
// eof // eof// //
#LEXERTEST:
COMMENT_LINE,COMMENT_LINE,COMMENT_LINE,COMMENT_LINE,EOL

▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂ 
//#LEXERTEST:
COMMENT_LINE
▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂ 
///#LEXERTEST:
DOCCOMMENT_LINE

▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂ Multi comment
/*+*/a/**/a/***/a/** */a/* /*  */
/* // *//* 
multiline coment /+ 
 */
#LEXERTEST:
COMMENT_MULTI,*,COMMENT_MULTI,*,DOCCOMMENT_MULTI,*,DOCCOMMENT_MULTI,*,
COMMENT_MULTI,EOL,
COMMENT_MULTI,COMMENT_MULTI,EOL

▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂
/+*+/_/++/_/+++/_/++ +/_/+ /+  +/ asdf  +/
/+ // +//+ 
multiline coment /+  /+ 
  +/ //
  /*
+/
*/
 +/
#LEXERTEST:
COMMENT_NESTED,*,COMMENT_NESTED,*,DOCCOMMENT_NESTED,*,DOCCOMMENT_NESTED,*,
COMMENT_NESTED,EOL,
COMMENT_NESTED,COMMENT_NESTED,EOL

▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂ the /*/ situation
/*/ */
/+/aa+/
#LEXERTEST:
COMMENT_MULTI,EOL,COMMENT_NESTED,EOL

▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂ test error cases
/* asdf
/+/aa+/*
#LEXERTEST:
COMMENT_MULTI!Cx
▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂ test error cases
/** asdf
/+/aa+/*
#LEXERTEST:
DOCCOMMENT_MULTI!Cx
▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂ 
/+ xxx
/++ asdf +/ 
asdf#LEXERTEST:
COMMENT_NESTED!CNx
▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂ 
/++ xxxx
/+ asdf +/ 
asdf#LEXERTEST:
DOCCOMMENT_NESTED!CNx