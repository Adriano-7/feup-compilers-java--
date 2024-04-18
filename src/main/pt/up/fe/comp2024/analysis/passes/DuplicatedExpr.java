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

public class DuplicatedExpr extends AnalysisVisitor {
    private HashSet<String> params = new HashSet<>();
    private HashMap<JmmNode, List<String>> vars = new HashMap<>();

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
        while (!parent.getKind().equals("ClassStmt") && !parent.getKind().equals("PublicStaticVoidMethodDecl") && !parent.getKind().equals("PublicMethodDecl")) {
            parent = parent.getParent();
        }

        String varName = varDeclNode.get("name");
        if (vars.containsKey(parent)) {
            List<String> declaredVars = vars.get(parent);
            if (declaredVars.contains(varName)) {
                var message = String.format("Duplicate variable declaration: '%s of %s'", varName, parent.get("name"));
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(varDeclNode),
                        NodeUtils.getColumn(varDeclNode),
                        message,
                        null)
                );
            } else {
                declaredVars.add(varName);
            }
        } else {
            List<String> declaredVars = new ArrayList<>();
            declaredVars.add(varName);
            vars.put(parent, declaredVars);
        }

        JmmNode varType = varDeclNode.getChildren().get(0);
        if(varType.get("isVarArg").equals("true")){
            var message = String.format("Variable '%s' has a vararg type", varName);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(varDeclNode),
                    NodeUtils.getColumn(varDeclNode),
                    message,
                    null)
            );
        }

        return null;
    }
}