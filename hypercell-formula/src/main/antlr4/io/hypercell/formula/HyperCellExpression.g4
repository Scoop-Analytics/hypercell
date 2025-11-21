/**
 []* Define a grammar called expression to execute derived expressions
 */
 grammar HyperCellExpression;

 @header {
   package io.hypercell.formula;
}

 start
 :
 	expression
 ;

 expression
 :
 	'-' expression # UMINUS |
 	expression powerop expression # POWER |
 	expression mulop expression # MULOP |
 	expression addop expression # ADDOP |
 	expression compareop expression # COMPOPP |
 	expression concatop expression # CONCATOPP |
 	'(' expression ')' # PAREN |
 	number # NUMBER |
 	mathematical # MATH |
 	logical # LOGICAL |
 	lookup # LOOKUP |
 	financial # FINANCIAL |
 	statistical # STATISTICAL |
 	informational # INFORMATIONAL |
 	textual # TEXTUAL |
 	datetime # DATETIME |
 	filter # FILTER |
 	reference # REF |
 	string # STRING | 
 	boolexp # BOOLEAN |
 	constexp # CONSTANT |
 	sheetsexport # SHEETS |
 	genericFunction # GENERIC_FUNCTION
 ;

 mathematical
 :
 	SUMTOKEN '(' (expression | range | tablearray) (',' (expression | range | tablearray))* ')' # SUM |
 	SUMIFTOKEN '(' (range | tablearray)  ',' expression (',' range )? ')' # SUMIF |
 	SUMIFSTOKEN '(' (range | tablearray) (',' (range | tablearray) ',' expression)* ')' # SUMIFS |
 	SUMPRODUCTTOKEN '(' sumproductarguments ')' # SUMPRODUCT |
 	AVERAGETOKEN '(' (expression | range) (',' (expression | range))* ')' # AVG |
 	AVERAGEIFTOKEN '(' range  ',' expression (',' range )? ')' # AVGIF |
 	AVERAGEIFSTOKEN '(' (range | tablearray) (',' (range | tablearray) ',' expression)*  ')' # AVGIFS |
 	MEDIANTOKEN '(' (expression | range) (',' (expression | range))* ')' # MEDIAN |
 	COUNTTOKEN '(' (expression | range | tablearray) (',' (expression | range))* ')' # COUNT |
 	COUNTATOKEN '(' (expression | range | tablearray) (',' (expression | range))* ')' # COUNTA |
 	COUNTIFTOKEN '(' (tablearray | range)  ',' expression ')' # COUNTIF |
 	COUNTIFSTOKEN '(' (tablearray | range) ',' expression (',' (tablearray | range) ',' expression)* ')' # COUNTIFS |
 	MAXIFSTOKEN '(' (range | tablearray) (',' (range | tablearray) ',' expression)* ')' # MAXIFS |
 	MINIFSTOKEN '(' (range | tablearray) (',' (range | tablearray) ',' expression)* ')' # MINIFS |
 	MAXTOKEN '(' (expression | range | tablearray) (',' (expression | range))* ')' # MAX |
 	MINTOKEN '(' (expression | range | tablearray) (',' (expression | range))* ')' # MIN |
 	STDEVTOKEN '(' (expression | range) (',' (expression | range))* ')' # STDEV |
 	LOGTOKEN '(' expression ')' # LOG |
 	LOG10TOKEN '(' expression ')' # LOG10 |
 	EXPTOKEN '(' expression ')' # EXP |
 	LNTOKEN '(' expression ')' # LN |
 	ABSTOKEN '(' expression ')' # ABS |
 	SQRTTOKEN '(' expression ')' # SQRT |
 	CEILINGTOKEN '(' expression (',' expression)? ')' # CEILING |
 	FLOORTOKEN '(' expression (',' expression)? ')' # FLOOR |
 	INTTOKEN '(' expression ')' # INT |
 	MODTOKEN '(' expression ',' expression ')' # MOD |
 	POWERTOKEN '(' expression ',' expression ')' # POWEROP |
 	ROUNDTOKEN '(' expression ',' expression ')' # ROUND |
 	ROUNDUPTOKEN '(' expression ',' expression ')' # ROUNDUP |
 	ROUNDDOWNTOKEN '(' expression ',' expression ')' # ROUNDDOWN |
 	TRUNCTOKEN '(' expression (',' expression)? ')' # TRUNC |
 	SUBTOTALTOKEN '(' expression (',' rangeorreference)+ ')' # SUBTOTAL |
 	RANDBETWEEN '(' expression ',' expression ')' # RANDBETWEEN
 ;
 
 sumproductarguments
 :
 	(rangeorreference (',' rangeorreference )*) |
 	(('(' filteredrange ')' '*')+ rangeorreference) |
 	(filteredrange (',' filteredrange)*)
 ;
 
 filteredrange
 :
 	range |
 	range COMPAREOPERATOR reference
 ;
 
 logical
 :
 	IFTOKEN '(' expression ',' expression (',' expression)? ')' # IF |
 	IFSTOKEN '(' expression ',' expression (',' expression ',' expression)* ')' # IFS |
 	IFERRORTOKEN '(' expression ',' expression ')' # IFERROR |
 	TRUETOKEN '(' ')' # TRUE |
 	FALSETOKEN '(' ')' # FALSE |
 	EQTOKEN '(' expression ',' expression ')' # EQ |
 	ANDTOKEN '(' expression (',' expression)+ ')' # AND |
 	ORTOKEN '(' expression (',' expression)+ ')' # OR |
 	XORTOKEN '(' expression (',' expression)+ ')' # XOR |
 	NOTTOKEN '(' expression ')' # NOT |
 	XLFNTOKEN? IFNATOKEN '(' expression ',' expression ')' # IFNA
 ;
 
 lookup
 :
 	VLOOKUPTOKEN '(' expression ',' (rangeorreference | tablearray) ',' expression (',' expression)? ')' |
 	HLOOKUPTOKEN '(' expression ',' (rangeorreference | tablearray) ',' expression (',' expression)? ')' |
 	CHOOSETOKEN '(' expression (',' expression)+ ')' |
 	SWITCHTOKEN '(' expression (',' expression ',' expression)+ (',' expression)? ')' |
 	MATCHTOKEN '(' expression ',' (expression | range | tablearray | booleanarray | expressionarray) (',' expression)? ')' |
 	XMATCHTOKEN '(' expression ',' (expression | range | tablearray) (',' expression)? (',' expression)? ')' |
 	INDEXTOKEN '(' (expression | range | tablearray) ',' expression (',' expression)? ')' |
 	XLOOKUPTOKEN '(' expression ',' (rangeorreference | tablearray) ',' (rangeorreference | tablearray) (',' expression)? (',' expression)? (',' expression)? ')'
 ;
 
 statistical
 :
 	NORMDISTTOKEN '(' expression (',' expression ',' expression ',' expression)? ')' # NORMDIST |
 	NORMSDISTTOKEN '(' expression (',' expression ',' expression ',' expression)? ')' # NORMSDIST
 ;

 informational
 :
 	TABLETOKEN '(' expression (',' expression)* ')' # TABLE |
 	ISNUMBERTOKEN '(' expression ')' # ISNUMBER |
 	ISTEXTTOKEN '(' expression ')' # ISTEXT |
 	ISNONTEXTTOKEN '(' expression ')' # ISNONTEXT |
 	ISNATOKEN '(' expression ')' # ISNA |
 	ISERRORTOKEN '(' expression ')' # ISERROR |
 	ISERRTOKEN '(' expression ')' # ISERR |
 	ISBLANKTOKEN '(' expression ')' # ISBLANK |
 	ISDATETOKEN '(' expression ')' # ISDATE
 ;

 textual
 :
 	MIDTOKEN '(' expression ',' expression ',' expression ')' |
 	FINDTOKEN '(' expression ',' expression (',' expression)? ')' |
 	LEFTTOKEN '(' expression (',' expression)? ')' |
 	LENTOKEN '(' expression ')' |
 	LOWERTOKEN '(' expression ')' |
 	UPPERTOKEN '(' expression ')' |
 	PROPERTOKEN '(' expression ')' |
 	REPLACETOKEN '(' expression ',' expression ',' expression ',' expression ')' |
 	RIGHTTOKEN '(' expression (',' expression)? ')' |
 	SEARCHTOKEN '(' expression ',' expression (',' expression)? ')' |
 	TRIMTOKEN '(' expression ')' |
 	SUBSTITUTETOKEN '(' expression ',' expression ',' expression (',' expression)? ')' |
 	TEXTTOKEN '(' expression ',' expression ')' |
 	TEXTAFTERTOKEN '(' expression ',' expression (',' expression (',' expression (',' expression (',' expression)?)?)?)? ')' |
 	TEXTBEFORETOKEN '(' expression ',' expression (',' expression (',' expression (',' expression (',' expression)?)?)?)? ')' |
 	TEXTJOINTOKEN '(' (expression | range) ',' expression (',' (expression | range))+ ')' |
 	CONCATENATETOKEN '(' expression (',' expression)* ')' |
 	VALUETOKEN '(' expression ')' |
 	REGEXREPLACETOKEN '(' expression ',' expression ',' expression (',' expression? (',' expression)?)? ')'
 ;

booleanarray
 :
   '(' booleanarray ')'                                 # GROUPARRAY        // Parentheses for grouping
    | (range | tablearray) COMPAREOPERATOR expression   # COMPAREARRAY      // Example: A1:A10 = 1
    | booleanarray (',' | '*' | '+') booleanarray       # BOOLEANARRAYOP    // Example: (A1:A10 = 1) AND (B2:B10 < 5)
    | NOTTOKEN '(' booleanarray ')'                     # NOTARRAY          // Example: NOT(A1:A10 = 1)
 ;

 expressionarray
 :
    '{'
        expression (',' expression)*
    '}' # EXPRESSIONARRAY
 ;

 datetime
 :
 	EOMONTHTOKEN '(' expression ',' expression ')' |
 	DATETOKEN '(' expression ',' expression ',' expression ')' |
 	DATEDIFTOKEN '(' expression ',' expression ',' string ')' |
 	DATEVALUETOKEN '(' expression ')' |
 	DAYTOKEN '(' expression ')' |
 	DAYSTOKEN '(' expression ',' expression ')' |
	EDATETOKEN '(' expression ',' expression ')' |
	HOURTOKEN '(' expression ')' |
	MINUTETOKEN '(' expression ')' |
	SECONDTOKEN '(' expression ')' |
	MONTHTOKEN '(' expression ')' |
	YEARTOKEN '(' expression ')' |
	NOWTOKEN '(' ')' |
	TODAYTOKEN '(' ')' |
 	TIMETOKEN '(' expression ',' expression ',' expression ')' |
 	TIMEVALUETOKEN '(' expression ')' |
 	NETWORKDAYSTOKEN '(' expression ',' expression (',' rangeorreference)? ')' |
 	WEEKDAYTOKEN '(' expression (',' expression)? ')' |
 	WEEKNUMTOKEN '(' expression (',' expression)? ')'
 ;
 
 filter
  :
     FILTERTOKEN '('
         (tablearray | range) ','               // The array argument
         booleanarray                           // The include criteria
         (',' expression)?                      // Optional if_empty fallback
     ')' |
     UNIQUETOKEN '(' (range | tablearray | expression) ')' |
     SORTTOKEN '(' (range | tablearray | expression) (',' expression (',' expression (',' expression)?)?)? ')'
 ;

 financial:
 	IRRTOKEN '(' rangeorreference (',' rangeorreference)? ')' |
 	NPVTOKEN '(' expression ',' rangeorreference ')'
 ;

 scoop:
    SCOOPNEXTCONVERSION '(' expression ',' expression ',' (expression ',' expression)+ ')' |
    SCOOPFINALCONVERSION '(' expression ',' expression ',' (expression ',' expression)+ ')' |
    SCOOPPROMPT '(' expression ',' expression ')' |
    SCOOPJSON '(' expression ',' expression ')' |
    SCOOPLOOKUP '(' expression ',' expression ',' expression ',' expression ')' | // Lookup value, dataset, lookup column, result column
    SCOOPAPPLYMODEL '(' expression ')' | // Apply a model to this row
    SCOOP '(' expression ',' expression ')' |
    NULLTOKEN
 ;

 sheetsexport:
    XLUDFTOKEN '(' expression ')' # XLUDF |
   	COMSUMTOKEN '(' expression (',' expression)* ')' # COMSUMTOKEN
 ;

 powerop
 :
 	'^'
 ;
 
 mulop
 :
 	'*'
 	| '/'
 	| '%'
 ;
 
 addop
 :
 	'+'
 	| '-'
 ;
 
 compareop:
 	COMPAREOPERATOR	
 ;
 
 concatop:
 	CONCATOPERATOR
 ;
 
 rangeorreference
 :
 	reference | range
 ;
 
 reference
 :
	item # CELL 
	| offset # OFFSET
 ;

 offset
 :
 	'OFFSET(' item ',' Integer (',' Integer)? ')'
 ;

 range
 :
 	 ( item | offset) ':' ( item | offset)  # CELLRANGE
 	 //| 	 IDENTIFIER                             # IDENTIFIERRANGE // In the future we can aggregate identifiers as ranges
 ;

 item
 :
    CELLADDRESS |
 	IDENTIFIER
 ;

 tablearray
 :
 	TABLEARRAYADDRESS
 ;
 
 string
 :
 	STRINGTOKEN
 ;
 
 number
 :
 	DecimalFloatingPointLiteral # DECIMALVAL
 	| Integer # INTEGERVAL
 ;
 
 boolexp
 :
 	TRUETOKEN |
 	FALSETOKEN
 ;

 constexp
 :
    NATOKEN '(' ')' |
    ATNATOKEN
 ;
 
 IFTOKEN
 :
 	('I'|'i')('F'|'f')
 ;

 IFSTOKEN
 :
 	('I'|'i')('F'|'f')('S'|'s')
 ;

 IFERRORTOKEN
 :
 	('I'|'i')('F'|'f')('E'|'e')('R'|'r')('R'|'r')('O'|'o')('R'|'r')
 ;

 IFNATOKEN
 :
 	('I'|'i')('F'|'f')('N'|'n')('A'|'a')
 ;

 SUMTOKEN
 :
 	('S'|'s')('U'|'u')('M'|'m')
 ;

 SUMPRODUCTTOKEN
 :
 	('S'|'s')('U'|'u')('M'|'m')('P'|'p')('R'|'r')('O'|'o')('D'|'d')('U'|'u')('C'|'c')('T'|'t')
 ;

 AVERAGETOKEN
 :
 	('A'|'a')('V'|'v')('E'|'e')('R'|'r')('A'|'a')('G'|'g')('E'|'e')
 ;

 MEDIANTOKEN
 :
 	('M'|'m')('E'|'e')('D'|'d')('I'|'i')('A'|'a')('N'|'n')
 ;

 COUNTTOKEN
 :
 	('C'|'c')('O'|'o')('U'|'u')('N'|'n')('T'|'t')
 ;

 COUNTATOKEN
 :
 	('C'|'c')('O'|'o')('U'|'u')('N'|'n')('T'|'t')('A'|'a')
 ;

 MAXTOKEN
 :
 	('M'|'m')('A'|'a')('X'|'x')
 ;

 MINTOKEN
 :
 	('M'|'m')('I'|'i')('N'|'n')
 ;

 STDEVTOKEN
 :
 	('S'|'s')('T'|'t')('D'|'d')('E'|'e')('V'|'v')
 ;
 
 SUBTOTALTOKEN
 :
 	('S'|'s')('U'|'u')('B'|'b')('T'|'t')('O'|'o')('T'|'t')('A'|'a')('L'|'l')
 ;

 VLOOKUPTOKEN
 :
 	('V'|'v')('L'|'l')('O'|'o')('O'|'o')('K'|'k')('U'|'u')('P'|'p')
 ;

 HLOOKUPTOKEN
 :
 	('H'|'h')('L'|'l')('O'|'o')('O'|'o')('K'|'k')('U'|'u')('P'|'p')
 ;
 
 CHOOSETOKEN
 :
 	('C'|'c')('H'|'h')('O'|'o')('O'|'o')('S'|'s')('E'|'e')
 ;

 SWITCHTOKEN
 :
  	('S'|'s')('W'|'w')('I'|'i')('T'|'t')('C'|'c')('H'|'h')
 ;

  MATCHTOKEN
 :
 	('M'|'m')('A'|'a')('T'|'t')('C'|'c')('H'|'h')
 ;

 XMATCHTOKEN
 :
 	('X'|'x')('M'|'m')('A'|'a')('T'|'t')('C'|'c')('H'|'h')
 ;

 INDEXTOKEN
 :
 	('I'|'i')('N'|'n')('D'|'d')('E'|'e')('X'|'x')
 ;

 XLOOKUPTOKEN
 :
 	('X'|'x')('L'|'l')('O'|'o')('O'|'o')('K'|'k')('U'|'u')('P'|'p')
 ;

 COUNTIFTOKEN
 :
 	('C'|'c')('O'|'o')('U'|'u')('N'|'n')('T'|'t')('I'|'i')('F'|'f')
 ;

 COUNTIFSTOKEN
 :
 	('C'|'c')('O'|'o')('U'|'u')('N'|'n')('T'|'t')('I'|'i')('F'|'f')('S'|'s')
 ;

 SUMIFTOKEN
 :
 	('S'|'s')('U'|'u')('M'|'m')('I'|'i')('F'|'f')
 ;

 SUMIFSTOKEN
 :
 	('S'|'s')('U'|'u')('M'|'m')('I'|'i')('F'|'f')('S'|'s')
 ;

 MAXIFSTOKEN
 :
 	('M'|'m')('A'|'a')('X'|'x')('I'|'i')('F'|'f')('S'|'s')
 ;

 MINIFSTOKEN
 :
 	('M'|'m')('I'|'i')('N'|'n')('I'|'i')('F'|'f')('S'|'s')
 ;

 AVERAGEIFTOKEN
 :
 	 	('A'|'a')('V'|'v')('E'|'e')('R'|'r')('A'|'a')('G'|'g')('E'|'e')('I'|'i')('F'|'f')
 ;

 AVERAGEIFSTOKEN
 :
 	 	('A'|'a')('V'|'v')('E'|'e')('R'|'r')('A'|'a')('G'|'g')('E'|'e')('I'|'i')('F'|'f')('S'|'s')
 ;

 IRRTOKEN
 :
 	('I'|'i')('R'|'r')('R'|'r')
 ;

 NPVTOKEN
 :
 	('N'|'n')('P'|'p')('V'|'v')
 ;

 TRUETOKEN
 :
 	('T'|'t')('R'|'r')('U'|'u')('E'|'e')
 ;

 FALSETOKEN
 :
 	('F'|'f')('A'|'a')('L'|'l')('S'|'s')('E'|'e')
 ;
 
 EQTOKEN
 :
 	('E'|'e')('Q'|'q')
 ;

 ANDTOKEN
 :
 	('A'|'a')('N'|'n')('D'|'d')
 ;

 ORTOKEN
 :
 	('O'|'o')('R'|'r')
 ;

 XORTOKEN
 :
 	('X'|'x')('O'|'o')('R'|'r')
 ;

 NOTTOKEN
 :
 	('N'|'n')('O'|'o')('T'|'t')
 ;

 EOMONTHTOKEN
 :
 	('E'|'e')('O'|'o')('M'|'m')('O'|'o')('N'|'n')('T'|'t')('H'|'h')
 ;
 
 DATETOKEN
 :
 	('D'|'d')('A'|'a')('T'|'t')('E'|'e')
 ;
 
 DATEDIFTOKEN
 :
 	('D'|'d')('A'|'a')('T'|'t')('E'|'e')('D'|'d')('I'|'i')('F'|'f')
 ;
 
 DATEVALUETOKEN
 :
 	('D'|'d')('A'|'a')('T'|'t')('E'|'e')('V'|'v')('A'|'a')('L'|'l')('U'|'u')('E'|'e')
 ;
 
 DAYTOKEN
 :
 	('D'|'d')('A'|'a')('Y'|'y')
 ;
 
 DAYSTOKEN
 :
 	('D'|'d')('A'|'a')('Y'|'y')('S'|'s')
 ;
 
 EDATETOKEN
 :
 	('E'|'e')('D'|'d')('A'|'a')('T'|'t')('E'|'e')
 ;
 
 HOURTOKEN
 :
 	('H'|'h')('O'|'o')('U'|'u')('R'|'r')
 ;
 
 MINUTETOKEN
 :
 	('M'|'m')('I'|'i')('N'|'n')('U'|'u')('T'|'t')('E'|'e')
 ;
 
 SECONDTOKEN
 :
 	('S'|'s')('E'|'e')('C'|'c')('O'|'o')('N'|'n')('D'|'d')
 ;
 
 MONTHTOKEN
 :
 	('M'|'m')('O'|'o')('N'|'n')('T'|'t')('H'|'h')
 ;
 
 YEARTOKEN
 :
 	('Y'|'y')('E'|'e')('A'|'a')('R'|'r')
 ;
 
 NOWTOKEN
 :
 	('N'|'n')('O'|'o')('W'|'w')
 ;
 
 TODAYTOKEN
 :
 	('T'|'t')('O'|'o')('D'|'d')('A'|'a')('Y'|'y')
 ;
 
 TIMETOKEN
 :
 	('T'|'t')('I'|'i')('M'|'m')('E'|'e')
 ;
 
 TIMEVALUETOKEN
 :
 	('T'|'t')('I'|'i')('M'|'m')('E'|'e')('V'|'v')('A'|'a')('L'|'l')('U'|'u')('E'|'e')
 ;
 
 NETWORKDAYSTOKEN
 :
 	('N'|'n')('E'|'e')('T'|'t')('W'|'w')('O'|'o')('R'|'r')('K'|'k')('D'|'d')('A'|'a')('Y'|'y')('S'|'s')
 ;
 
 WEEKDAYTOKEN
 :
    ('W'|'w')('E'|'e')('E'|'e')('K'|'k')('D'|'d')('A'|'a')('Y'|'y')
 ;

 WEEKNUMTOKEN
 :
    ('W'|'w')('E'|'e')('E'|'e')('K'|'k')('N'|'n')('U'|'u')('M'|'m')
 ;

 LOG10TOKEN
 :
 	('L'|'l')('O'|'o')('G'|'g')'1''0'
 ;

 LOGTOKEN
 :
 	('L'|'l')('O'|'o')('G'|'g')
 ;

 EXPTOKEN
 :
 	('E'|'e')('X'|'x')('P'|'p')
 ;

 LNTOKEN
 :
 	('L'|'l')('N'|'n')
 ;
 
 ABSTOKEN
 :
 	('A'|'a')('B'|'b')('S'|'s')
 ;

 SQRTTOKEN
 :
 	('S'|'s')('Q'|'q')('R'|'r')('T'|'t')
 ;

 CEILINGTOKEN
 :
 	('C'|'c')('E'|'e')('I'|'i')('L'|'l')('I'|'i')('N'|'n')('G'|'g')
 ;

 FLOORTOKEN
 :
 	('F'|'f')('L'|'l')('O'|'o')('O'|'o')('R'|'r')
 ;

 INTTOKEN
 :
 	('I'|'i')('N'|'n')('T'|'t')
 ;

 MODTOKEN
 :
 	('M'|'m')('O'|'o')('D'|'d')
 ;

 POWERTOKEN
 :
 	('P'|'p')('O'|'o')('W'|'w')('E'|'e')('R'|'r')
 ;

 ROUNDTOKEN
 :
 	('R'|'r')('O'|'o')('U'|'u')('N'|'n')('D'|'d')
 ;

ROUNDUPTOKEN
 :
 	('R'|'r')('O'|'o')('U'|'u')('N'|'n')('D'|'d')('U'|'u')('P'|'p')
 ;

ROUNDDOWNTOKEN
 :
 	('R'|'r')('O'|'o')('U'|'u')('N'|'n')('D'|'d')('D'|'d')('O'|'o')('W'|'w')('N'|'n')
 ;

RANDBETWEEN
:
('R'|'r')('A'|'a')('N'|'n')('D'|'d')('B'|'b')('E'|'e')('T'|'t')('W'|'w')('E'|'e')('E'|'e')('N'|'n')
;

 TRUNCTOKEN
 :
 	('T'|'t')('R'|'r')('U'|'u')('N'|'n')('C'|'c')
 ;

 NORMDISTTOKEN
 :
 	('N'|'n')('O'|'o')('R'|'r')('M'|'m')('D'|'d')('I'|'i')('S'|'s')('T'|'t')
 ;
 
 NORMSDISTTOKEN
 :
 	('N'|'n')('O'|'o')('R'|'r')('M'|'m')('S'|'s')('D'|'d')('I'|'i')('S'|'s')('T'|'t')
 ;
 
 TABLETOKEN
 :
 	('T'|'t')('A'|'a')('B'|'b')('L'|'l')('E'|'e')
 ;
 
 ISNUMBERTOKEN
 :
 	('I'|'i')('S'|'s')('N'|'n')('U'|'u')('M'|'m')('B'|'b')('E'|'e')('R'|'r')
 ;
 
 ISTEXTTOKEN
 :
 	('I'|'i')('S'|'s')('T'|'t')('E'|'e')('X'|'x')('T'|'t')
 ;
 
 ISNATOKEN
 :
 	('I'|'i')('S'|'s')('N'|'n')('A'|'a')
 ;
 
 ISERRTOKEN
 :
 	('I'|'i')('S'|'s')('E'|'e')('R'|'r')('R'|'r')
 ;
 
 ISERRORTOKEN
 :
 	('I'|'i')('S'|'s')('E'|'e')('R'|'r')('R'|'r')('O'|'o')('R'|'r')
 ;
 
 ISBLANKTOKEN
 :
 	('I'|'i')('S'|'s')('B'|'b')('L'|'l')('A'|'a')('N'|'n')('K'|'k')
 ;
 
 ISDATETOKEN
  :
  	('I'|'i')('S'|'s')('D'|'d')('A'|'a')('T'|'t')('E'|'e')
  ;

  ISNONTEXTTOKEN
 :
 	('I'|'i')('S'|'s')('N'|'n')('O'|'o')('N'|'n')('T'|'t')('E'|'e')('X'|'x')('T'|'t')
 ;
 
 MIDTOKEN
 :
 	('M'|'m')('I'|'i')('D'|'d')
 ;
 
 FINDTOKEN
 :
 	('F'|'f')('I'|'i')('N'|'n')('D'|'d')
 ;
 
 LEFTTOKEN
 :
 	('L'|'l')('E'|'e')('F'|'f')('T'|'t')
 ;
 
 LENTOKEN
 :
 	('L'|'l')('E'|'e')('N'|'n')
 ;
 
 LOWERTOKEN
 :
 	('L'|'l')('O'|'o')('W'|'w')('E'|'e')('R'|'r')
 ;
 
 UPPERTOKEN
 :
 	('U'|'u')('P'|'p')('P'|'p')('E'|'e')('R'|'r')
 ;
 
 PROPERTOKEN
 :
 	('P'|'p')('R'|'r')('O'|'o')('P'|'p')('E'|'e')('R'|'r')
 ;
 
 REPLACETOKEN
 :
 	('R'|'r')('E'|'e')('P'|'p')('L'|'l')('A'|'a')('C'|'c')('E'|'e')
 ;
 
 RIGHTTOKEN
 :
 	('R'|'r')('I'|'i')('G'|'g')('H'|'h')('T'|'t')
 ;
 
 SEARCHTOKEN
 :
 	('S'|'s')('E'|'e')('A'|'a')('R'|'r')('C'|'c')('H'|'h')
 ;
 
 TRIMTOKEN
 :
 	('T'|'t')('R'|'r')('I'|'i')('M'|'m')
 ;
 
 SUBSTITUTETOKEN
 :
 	('S'|'s')('U'|'u')('B'|'b')('S'|'s')('T'|'t')('I'|'i')('T'|'t')('U'|'u')('T'|'t')('E'|'e')
 ;
 
 TEXTTOKEN
 :
 	('T'|'t')('E'|'e')('X'|'x')('T'|'t')
 ;
 
 TEXTAFTERTOKEN
 :
 	('T'|'t')('E'|'e')('X'|'x')('T'|'t')('A'|'a')('F'|'f')('T'|'t')('E'|'e')('R'|'r')
 ;
 
 TEXTBEFORETOKEN
 :
 	('T'|'t')('E'|'e')('X'|'x')('T'|'t')('B'|'b')('E'|'e')('F'|'f')('O'|'o')('R'|'r')('E'|'e')
 ;
 
 TEXTJOINTOKEN
 :
 	('T'|'t')('E'|'e')('X'|'x')('T'|'t')('J'|'j')('O'|'o')('I'|'i')('N'|'n')
 ;
 
 TEXTSPLITTOKEN
 :
 	('T'|'t')('E'|'e')('X'|'x')('T'|'t')('S'|'s')('P'|'p')('L'|'l')('I'|'i')('T'|'t')
 ;
 
 VALUETOKEN
 :
 	('V'|'v')('A'|'a')('L'|'l')('U'|'u')('E'|'e')
 ;

 REGEXREPLACETOKEN
 :
    ('R'|'r')('E'|'e')('G'|'g')('E'|'e')('X'|'x')('R'|'r')('E'|'e')('P'|'p')('L'|'l')('A'|'a')('C'|'c')('E'|'e')
 ;

 CONCATENATETOKEN
 :
  	('C'|'c')('O'|'o')('N'|'n')('C'|'c')('A'|'a')('T'|'t')('E'|'e')('N'|'n')('A'|'a')('T'|'t')('E'|'e')
 ;

  FILTERTOKEN
 :
    ('F'|'f')('I'|'i')('L'|'l')('T'|'t')('E'|'e')('R'|'r')
 ;

 UNIQUETOKEN
 :
    ('U'|'u')('N'|'n')('I'|'i')('Q'|'q')('U'|'u')('E'|'e')
 ;

 SORTTOKEN
 :
    ('S'|'s')('O'|'o')('R'|'r')('T'|'t')
 ;

 XLUDFTOKEN:
     '_''_''x''l''u''d''f''.''D''U''M''M''Y''F''U''N''C''T''I''O''N'
 ;

 XLFNTOKEN:
    '_xlfn.'
;

 COMSUMTOKEN:
    'com.sun.star.sheet.addin' ('.' [a-zA-Z]+)*
 ;

 SCOOPNEXTCONVERSION:
    ('S'|'s')('C'|'c')('O'|'o')('O'|'o')('P'|'p')('N'|'n')('E'|'e')('X'|'x')('T'|'t')('C'|'c')('O'|'o')('N'|'n')('V'|'v')('E'|'e')('R'|'r')('S'|'s')('I'|'i')('O'|'o')('N'|'n')
 ;
 
 SCOOPFINALCONVERSION:
    ('S'|'s')('C'|'c')('O'|'o')('O'|'o')('P'|'p')('F'|'f')('I'|'i')('N'|'n')('A'|'a')('L'|'l')('C'|'c')('O'|'o')('N'|'n')('V'|'v')('E'|'e')('R'|'r')('S'|'s')('I'|'i')('O'|'o')('N'|'n')
 ;

 SCOOPPROMPT:
    ('S'|'s')('C'|'c')('O'|'o')('O'|'o')('P'|'p')('P'|'p')('R'|'r')('O'|'o')('M'|'m')('P'|'p')('T'|'t')
 ;

 SCOOPJSON:
    ('S'|'s')('C'|'c')('O'|'o')('O'|'o')('P'|'p')('J'|'j')('S'|'s')('O'|'o')('N'|'n')
 ;

 SCOOPLOOKUP:
    ('S'|'s')('C'|'c')('O'|'o')('O'|'o')('P'|'p')('L'|'l')('O'|'o')('O'|'o')('K'|'k')('U'|'u')('P'|'p')
 ;

 SCOOPAPPLYMODEL:
    ('S'|'s')('C'|'c')('O'|'o')('O'|'o')('P'|'p')('A'|'a')('P'|'p')('P'|'p')('L'|'l')('Y'|'y')('M'|'m')('O'|'o')('D'|'d')('E'|'e')('L'|'l')
 ;

 SCOOP:
    ('S'|'s')('C'|'c')('O'|'o')('O'|'o')('P'|'p')
 ;

 NULLTOKEN:
    ('N'|'n')('U'|'u')('L'|'l')('L'|'l')
 ;

 NATOKEN:
    ('N'|'n')('A'|'a')
 ;

ATNATOKEN:
    '@NA'
;

IDENTIFIER
 :
 	'\'' ([a-zA-Z0-9_ !@#$%^&*()/+] | '-')* '\''
 ;

 STRINGTOKEN
     : '"' ( '""' | ~["] )* '"'
 ;

 OPERATOR
 :
 	'+'
 	| '-'
 	| '*'
 	| '/'
 ;

 COMPAREOPERATOR
 :
 	'='
 	| '>'
 	| '<'
 	| '>='
 	| '<='
 	| '<>'
 ;
 
 CONCATOPERATOR
 :
 	'&'
 ;
 
 DecimalFloatingPointLiteral
 :
 	Digit+ '.' Digit+ ('E' ('+'|'-') Integer+)?
 	| '.' Digit+ ('E' ('+'|'-') Integer+)? | Digit+ ('E' ('+'|'-') Integer+)
 ;

 Integer
 :
 	'0'
 	| NonZeroDigit
 	(
 		Digit*
 	)
 ;
 
 TABLEARRAYADDRESS
 :
	(Hold? '\'' SheetNameWithSpaces '\'' ('.'|'!'))? Hold? Char+ [:] Hold? Char+ |
	(Hold? SheetNameWithoutSpaces ('.'|'!'))? Hold? Char+ [:] Hold? Char+
 ;

 CELLADDRESS
 :
	( '\'' LiveSheetNameWithSpaces? Hold? SheetNameWithSpaces '\'' ('.'|'!'))? Hold? Char+ Hold? Digit+ |
	( LiveSheetNameWithoutSpaces? Hold? SheetNameWithoutSpaces ('.'|'!'))? Hold? Char+ Hold? Digit+
 ;
 
 fragment
 Hold
 :
 	'$'
 ;
 
 fragment
 LiveSheetNameWithSpaces
 :
 Char '[' ([a-zA-Z0-9_ ])* ']'
 ;

 fragment
 SheetNameWithSpaces
 :
 Char (('[' | ']' | [a-zA-Z0-9-_()| ])*)
 ;
 
 fragment
 LiveSheetNameWithoutSpaces
 :
 Char '[' ([a-zA-Z0-9_|])* ']'
 ;

 fragment
 SheetNameWithoutSpaces
 :
 Char (('[' | ']' | [a-zA-Z0-9_])*)
 ;

 fragment
 Char
 : [a-zA-Z] | '[' | ']'
 ;

 fragment
 Digit
 :
 	'0'
 	| NonZeroDigit
 ;

 fragment
 NonZeroDigit
 :
 	[1-9]
 ;

 WS
 :
 	[ \t\r\n]+ -> skip
 ; // skip spaces, tabs, newlines



genericFunction
 :
 	IDENTIFIER '(' (expression (',' expression)*)? ')'
 ;