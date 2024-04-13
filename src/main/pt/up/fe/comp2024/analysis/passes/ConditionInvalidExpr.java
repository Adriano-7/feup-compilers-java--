package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

/**
 Checks if the assignment is valid.
 • Expressions in conditions must return a boolean (if(2+3) is an error)
 */
public class ConditionInvalidExpr extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.IF_ELSE_STMT, this::visitConditionStmt);
        addVisit(Kind.WHILE_STMT, this::visitConditionStmt);
    }

    private Void visitConditionStmt(JmmNode conditionStmt, SymbolTable table) {
        JmmNode condition = conditionStmt.getChildren().get(0);

        Type conditionType = TypeUtils.getExprType(condition, table);
        if (!conditionType.getName().equals("boolean")) {
            var message = String.format("Condition expression must return a boolean, but it returns '%s'", conditionType);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(condition),
                    NodeUtils.getColumn(condition),
                    message,
                    null)
            );
        }

        return null;
    }
}
