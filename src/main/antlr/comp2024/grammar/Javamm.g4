grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

LPAREN : '(';
RPAREN : ')';
LBRACK : '[';
RBRACK : ']';
LCURLY : '{';
RCURLY : '}';
COMMA : ',';
DOT : '.';
MUL : '*';
DIV : '/';
ADD : '+';
SUB: '-';
LT : '<';
LE : '<=';
GT : '>';
GE : '>=';
EQ : '==';
NE : '!=';
AND : '&&';
OR : '||';
NOT : '!';
EQUALS : '=';
SEMICOL : ';' ;
NEW : 'new' ;
THIS : 'this' ;
LENGTH : 'length' ;
TRUE : 'true' ;
FALSE : 'false' ;

STATIC: 'static';
IMPORT : 'import';
EXTENDS : 'extends';
CLASS : 'class' ;
INT : 'int' ;
STRING : 'String';
FLOAT : 'float' ;
DOUBLE : 'double' ;
VOID : 'void' ;
PUBLIC : 'public' ;
RETURN : 'return' ;
IF : 'if' ;
ELSE : 'else' ;
WHILE : 'while' ;

INTEGER : '0' | [1-9][0-9]* ;
ID : [a-zA-Z_$][a-zA-Z_$0-9]* ;

LINE_COMMENT : '//' ~[\r\n]* -> skip ;
BLOCK_COMMENT : '/*' .*? '*/' -> skip ;

WS : [ \t\n\r\f]+ -> skip ;

program
    : importDecl* classDecl EOF
    ;

importDecl
    : IMPORT ID('.'ID)* SEMI
    ;

classDecl
    : CLASS name=ID (EXTENDS extendedClass=ID)?
        LCURLY
        varDecl*
        methodDecl*
        RCURLY
    ;

varDecl
    : type name=ID SEMICOL
    ;

type
    : typeName = 'int'
    | typeName = 'int' '[' ']'
    | typeName = ID
    | typeName = ID '[' ']'
    | typeName = 'boolean'
    | typeName = 'String'
    ;

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN param RPAREN
        LCURLY varDecl* stmt* RCURLY
    ;

mainMethod
    : PUBLIC? STATIC VOID 'main'
        LPAREN STRING LBRACK RBRACK name=ID RPAREN
        LCURLY varDecl* stmt* RCURLY
    ;

param
    : type name=ID
    ;

stmt
    : LCURLY stmt* RCURLY #BlockStmt
    | IF LPAREN expr RPAREN stmt ELSE stmt #IfElseStmt
    | WHILE LPAREN expr RPAREN stmt #WhileStmt
    | expr SEMICOL #ExprStmt
    | name=ID EQUALS expr SEMICOL #AssignStmt
    | name=ID LBRACK expr RBRACK EQUALS expr SEMICOL #ArrayAssignStmt
    ;

argmtList: expr (COMMA expr)* #ArgList;

expr
    : LPAREN expr RPAREN #ParenExpr 
    | NEW name=ID LPAREN argmtList? RPAREN #NewObjectExpr
    | NEW INT LBRACK expr RBRACK #SpecificTypeNewArrayExpr 
    | expr DOT name=ID LPAREN argmtList? RPAREN #MethodCallExpr
    | expr LBRACK expr RBRACK #ArrayAccessExpr
    | className=ID expr #ClassInstanceCreationExpr
    | expr DOT LENGTH #ArrayLengthExpr
    | op=NOT expr #UnaryExpr
    | expr op= (MUL | DIV) expr #BinaryExpr 
    | expr op= (ADD | SUB) expr #BinaryExpr
    | expr op= (LT | LE | GT | GE) expr #BinaryExpr
    | expr op= (EQ | NE) expr #BinaryExpr 
    | expr op= AND expr #BinaryExpr
    | expr op= OR expr #BinaryExpr
    | LBRACK argmtList? RBRACK #UnspecifiedTypeNewArrayExpr 
    | value=INTEGER #IntegerLiteral
    | value=(TRUE|FALSE) #BooleanLiteral
    | name=ID #VarRefExpr
    | value=THIS #ThisExpr
    ;
