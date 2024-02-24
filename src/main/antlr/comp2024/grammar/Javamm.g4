grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

EQUALS : '=';
SEMI : ';' ;
LCURLY : '{' ;
RCURLY : '}' ;
LPAREN : '(' ;
RPAREN : ')' ;
MUL : '*' ;
DIV : '/' ;
ADD : '+' ;
SUB: '-' ;
LT : '<' ;
LE : '<=' ;
GT : '>' ;
GE : '>=' ;
EQ : '==' ;
NE : '!=' ;
AND : '&&' ;
OR : '||' ;
NOT : '!' ;

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

expr
    : expr op=NOT expr #UnaryExpr //Unary operations
    | expr op= (MUL | DIV) expr #BinaryExpr // Multiplicative operations
    | expr op= (ADD | SUB) expr #BinaryExpr //Additive operations
    | expr op= (LT | LE | GT | GE) expr #BinaryExpr //Relational operations
    | expr op= (EQ | NE) expr #BinaryExpr //Equality operations
    | expr op= AND expr #BinaryExpr //Logical operations (AND)
    | expr op= OR expr #BinaryExpr //Logical operations (OR)
    | value=INTEGER #IntegerLiteral //
    | value='true' #BooleanLiteral //
    | value='false' #BooleanLiteral //
    | name=ID #VarRefExpr //
    ;



