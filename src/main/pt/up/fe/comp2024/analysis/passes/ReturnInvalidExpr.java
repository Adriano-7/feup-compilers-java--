package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static pt.up.fe.comp2024.ast.TypeUtils.isImported;

/*
• Verify if the return expression is compatible with the return type of the method
*/

public class ReturnInvalidExpr extends AnalysisVisitor {
    @Override
    public void buildVisitor() {
        addVisit(Kind.RETURN_STMT, this::visitReturnStmt);
    }

    private Void visitReturnStmt(JmmNode returnStmt, SymbolTable table) {
        JmmNode expression = returnStmt.getChildren().get(0);

        JmmNode method = returnStmt.getParent();
        while (!method.getKind().equals("MethodDecl")  && !method.getKind().equals("PublicMethodDecl")) {
            method = method.getParent();
        }

        Optional<String> methodName = method.getOptional("name");
        if (methodName.isEmpty()) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(returnStmt),
                    NodeUtils.getColumn(returnStmt),
                    "Cannot find the name of the method",
                    null)
            );
            return null;
        }

        Type returnType = table.getReturnType(methodName.get());
        Type expressionType = TypeUtils.getExprType(expression, table);

        if (isImported(returnType, table) && isImported(expressionType, table)) {
            return null;
        }

        if (table.getSuper() != null && table.getSuper().equals(returnType.getName())) {
            return null;
        }

        if (!returnType.getName().equals(expressionType.getName())) {
            var message = String.format("Return expression is of type '%s', but the method returns '%s'", expressionType, returnType);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(returnStmt),
                    NodeUtils.getColumn(returnStmt),
                    message,
                    null)
            );
        }

        return null;
    }

}
