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
import java.util.Map;

import static pt.up.fe.comp2024.ast.TypeUtils.getVarExprType;
import static pt.up.fe.comp2024.ast.TypeUtils.isImported;

/**
 Checks if the assignment is valid.
 • Type of the assignee must be compatible with the assigned (an_int = a_bool is an error)
 • Array initializer (e.g., [1, 2, 3]) can be used in all places (i.e., expressions) that can accept
 an array of integers
**/

public class AssignInvalidExpr extends AnalysisVisitor {
    @Override
    public void buildVisitor() {
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
    }
    private Void visitAssignStmt(JmmNode assignStmt, SymbolTable table) {
        //Get the type of the variable in the name
        Type assigneeType = getVarExprType(assignStmt, table);
        if(assigneeType == null) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assignStmt),
                    NodeUtils.getColumn(assignStmt),
                    "Cannot find the type of the assignee",
                    null)
            );
            return null;
        }

        assignStmt.put("type", assigneeType.getName());
        assignStmt.put("isArray", assigneeType.isArray() ? "true" : "false");

        //Get the type of the expression
        JmmNode expression = assignStmt.getChildren().get(0);

        Type expressionType = TypeUtils.getExprType(expression, table);

        if (isImported(assigneeType, table) && isImported(expressionType, table)) {
            return null;
        }

        if (table.getSuper() != null && table.getSuper().equals(assigneeType.getName())) {
            return null;
        }

        //Check if the types are compatible
        if (!TypeUtils.areTypesAssignable(expressionType, assigneeType)) {
            var message = String.format("Type of the assignee (%s) must be compatible with the assigned (%s)", assigneeType, expressionType);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assignStmt),
                    NodeUtils.getColumn(assignStmt),
                    message,
                    null)
            );
        }
        else if (expression.getKind().equals("UnspecifiedTypeNewArrayExpr")) {
            List<JmmNode> children = expression.getChildren();
            for (JmmNode child : children) {
                if (!child.getKind().equals("IntegerLiteral")) {
                    var message = String.format("Array initializer can only have integers");
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(assignStmt),
                            NodeUtils.getColumn(assignStmt),
                            message,
                            null)
                    );
                }
            }

            if (!expressionType.isArray() || !expressionType.getName().equals("int")) {
                var message = String.format("Type of the assignee (%s) must be an array of integers", assigneeType);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(assignStmt),
                        NodeUtils.getColumn(assignStmt),
                        message,
                        null)
                );
            }
        }

        return null;
    }

}
