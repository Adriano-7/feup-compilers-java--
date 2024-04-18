package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/*
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
AND : '&&';
NOT : '!';
EQUALS : '=';
SEMICOL : ';';
NEW : 'new';
THIS : 'this';
TRUE : 'true';
FALSE : 'false';

STATIC: 'static';
IMPORT : 'import';
EXTENDS : 'extends';
CLASS : 'class';
INT : 'int';
VOID : 'void';
PUBLIC : 'public';
RETURN : 'return';
IF : 'if';
ELSE : 'else';
WHILE : 'while';

INTEGER : '0' | [1-9][0-9]* ;
ID : [a-zA-Z_$][a-zA-Z_$0-9]* ;

WS : [ \t\n\r\f]+ -> skip ;

program
    : importDecl* classDecl EOF
        ;

importDecl
    : IMPORT impPackage('.'impPackage)* SEMICOL #ImportStmt
;
impPackage
    : name=ID
;

classDecl
    : CLASS name=ID (EXTENDS extendedClass=ID)?
        LCURLY
varDecl*
methodDecl*
RCURLY #ClassStmt
;

varDecl
    : type name=ID SEMICOL
;

type returns [boolean isArray, boolean isVarArg=false]
        : name = 'int' #IntType
    | name = 'int' '...' {$isArray=true; $isVarArg=true;} #VarArgsType
    | name = 'int' '[' ']' {$isArray=true;} #IntArrayType
    | name = 'boolean' #BooleanType
    | name = 'String' #StringType
    | name = ID #IdType
;

methodDecl locals[boolean isPublic=false]
        : (PUBLIC {$isPublic=true;})?
type name=ID
LPAREN (param (COMMA param)* )? RPAREN
LCURLY varDecl* stmt*
returnStmt RCURLY #PublicMethodDecl
    | (PUBLIC {$isPublic=true;})?
STATIC VOID name=ID
LPAREN 'String' LBRACK RBRACK ID RPAREN
LCURLY varDecl* stmt* RCURLY #PublicStaticVoidMethodDecl
;

returnStmt
    : RETURN expr SEMICOL
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

expr
    : LPAREN expr RPAREN #ParenExpr
    | expr LBRACK expr RBRACK #ArrayAccessExpr
    | expr DOT name=ID LPAREN (expr (COMMA expr)*)? RPAREN #MethodCallExpr
    | expr DOT 'length' #ArrayLengthExpr
    | op=NOT expr #UnaryExpr
    | expr op= (MUL | DIV) expr #BinaryExpr
    | expr op= (ADD | SUB) expr #BinaryExpr
    | expr op=LT expr #BinaryExpr
    | expr op= AND expr #BinaryExpr
    | NEW name=ID LPAREN RPAREN #NewObjectExpr
    | NEW INT LBRACK expr RBRACK #SpecificTypeNewArrayExpr
    | LBRACK (expr (COMMA expr)*)? RBRACK #UnspecifiedTypeNewArrayExpr
    | value=INTEGER #IntegerLiteral
    | value=(TRUE|FALSE) #BooleanLiteral
    | name=ID #VarRefExpr
    | value=THIS #ThisExpr
;
*/

public class DuplicatedExpr extends AnalysisVisitor {
    private HashSet<String> params = new HashSet<>();
    private HashMap<String, List<String>> vars = new HashMap<>();

    @Override
    public void buildVisitor() {
        addVisit(Kind.PARAM, this::visitParam);
        addVisit(Kind.VAR_DECL, this::visitVarDecl);
    }

    private Void visitParam(JmmNode paramNode, SymbolTable table) {
        String paramName = paramNode.get("name");
        if (params.contains(paramName)) {
            var message = String.format("Duplicate parameter: '%s'", paramName);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(paramNode),
                    NodeUtils.getColumn(paramNode),
                    message,
                    null)
            );
        } else {
            params.add(paramName);
        }
        return null;
    }

    private Void visitVarDecl(JmmNode varDeclNode, SymbolTable table) {
        JmmNode parent = varDeclNode.getParent();
        String parentName = "";
        while (!parent.getKind().equals("ClassStmt") && !parent.getKind().equals("methodDecl") ) {
            parent = parent.getParent();
        }
        if (parent.getKind().equals("ClassStmt")) {
            parentName = "C" + parent.get("name");
        } else {
            parentName = "M" + varDeclNode.get("name");
        }

        String varName = varDeclNode.get("name");
        if (vars.containsKey(parent)) {
            List<String> varList = vars.get(parent);
            if (varList.contains(varName)) {
                var message = String.format("Duplicate variable: '%s'", varName);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(varDeclNode),
                        NodeUtils.getColumn(varDeclNode),
                        message,
                        null)
                );
            } else {
                varList.add(varName);
            }
        } else {
            List<String> varList = new ArrayList<>();
            varList.add(varName);
            vars.put(parentName, varList);
        }
        return null;
    }
}