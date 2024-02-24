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
SEMI : ';' ;
NEW : 'new' ;
THIS : 'this' ;
LENGTH : 'length' ;


CLASS : 'class' ;
INT : 'int' ;
PUBLIC : 'public' ;
RETURN : 'return' ;

INTEGER : '0' | [1-9][0-9]* ;
ID : [a-zA-Z_$][a-zA-Z_$0-9]* ;

WS : [ \t\n\r\f]+ -> skip ;

program
    : classDecl EOF
    ;

classDecl
    : CLASS name=ID
        LCURLY
        methodDecl*
        RCURLY
    ;

varDecl
    : type name=ID SEMI
    ;

type
    : name = INT
    | name = FLOAT
    | name = DOUBLE
    | name = VOID
    ;

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN param RPAREN
        LCURLY varDecl* stmt* RCURLY
    ;

param
    : type name=ID
    ;

stmt
    : expr EQUALS expr SEMI #AssignStmt //
    | RETURN expr SEMI #ReturnStmt
    ;

argmtList: expr (COMMA expr)* #ArgList;

expr
    : LPAREN expr RPAREN #ParenExpr 
    | NEW INT LBRACK expr RBRACK #SpTypeNewArrayExpr 
    | NEW name=ID LPAREN argmtList? RPAREN #NewObjectExpr
    | expr LBRACK expr RBRACK #ArrayAccessExpr
    | className=ID expr #classInstanceCreationExpr
    | expr DOT name=ID LPAREN argmtList? RPAREN #MethodCallExpr
    | expr DOT LENGTH #ArrayLengthExpr
    | value=THIS #ThisExpr
    | op=NOT expr #UnaryExpr 
    | expr op= (MUL | DIV) expr #BinaryExpr 
    | expr op= (ADD | SUB) expr #BinaryExpr
    | expr op= (LT | LE | GT | GE) expr #BinaryExpr
    | expr op= (EQ | NE) expr #BinaryExpr 
    | expr op= AND expr #BinaryExpr
    | expr op= OR expr #BinaryExpr
    | LBRACK argmtList? RBRACK #UnspTypeNewArrayExpr
    | value=INTEGER #IntegerLiteral 
    | value='true' #BooleanLiteral 
    | value='false' #BooleanLiteral 
    | name=ID #VarRefExpr 
    ;
