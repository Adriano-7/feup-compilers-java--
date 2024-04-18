package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.HashMap;
import java.util.HashSet;

/**
 Checks if the type of the expression in a return statement is compatible with the method return type.
 • Verify if identifiers used in the code have a corresponding declaration, either as a local variable,
 a method parameter, a field of the class or an imported class
 */
public class UndeclaredVariable extends AnalysisVisitor {
    private String currentMethod;
    HashSet<String> declaredVariables = new HashSet<>();

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.VAR_REF_EXPR, this::visitVarRefExpr);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        if (declaredVariables.contains(currentMethod)) {
            var message = String.format("Duplicate method declaration: '%s'", currentMethod);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    message,
                    null)
            );
        } else {
            declaredVariables.add(currentMethod);
        }
        return null;
    }

    private Void visitVarRefExpr(JmmNode varRefExpr, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        JmmNode parent = varRefExpr.getParent();
        while (!parent.getKind().equals("ClassStmt") && !parent.getKind().equals("PublicStaticVoidMethodDecl") && !parent.getKind().equals("PublicMethodDecl")) {
            parent = parent.getParent();
        }

        // Check if exists a parameter or variable declaration with the same name as the variable reference
        var varRefName = varRefExpr.get("name");

        // Var is a field, return
        if (table.getFields().stream().anyMatch(param -> param.getName().equals(varRefName))) {

            if(parent.getKind().equals("PublicStaticVoidMethodDecl")){
                var message = String.format("Trying to access field '%s' from a static context", varRefName);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(varRefExpr),
                        NodeUtils.getColumn(varRefExpr),
                        message,
                        null)
                );
            }

            return null;
        }

        // Var is a parameter, return
        if (table.getParameters(currentMethod).stream()
                .anyMatch(param -> param.getName().equals(varRefName))) {
            return null;
        }

        // Var is a declared variable, return
        if (table.getLocalVariables(currentMethod).stream()
                .anyMatch(varDecl -> varDecl.getName().equals(varRefName))) {
            return null;
        }

        //Var is an imported class, return
        if (table.getImports().stream()
                .anyMatch(importedClass -> importedClass.equals(varRefName))) {
            return null;
        }

        // Create error report
        var message = String.format("Variable '%s' does not exist.", varRefName);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(varRefExpr),
                NodeUtils.getColumn(varRefExpr),
                message,
                null)
        );

        return null;
    }
}
