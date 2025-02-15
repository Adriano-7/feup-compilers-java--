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
        JmmNode parent = assignStmt.getParent();
        while (!parent.getKind().equals("ClassStmt") && !parent.getKind().equals("PublicStaticVoidMethodDecl") && !parent.getKind().equals("PublicMethodDecl")) {
            parent = parent.getParent();
        }

        boolean isLocal = table.getLocalVariables(parent.get("name")).stream().anyMatch(param -> param.getName().equals(assignStmt.get("name")));
        boolean isField = table.getFields().stream().anyMatch(param -> param.getName().equals(assignStmt.get("name")));
        if (!isLocal && isField) {
            if (parent.getKind().equals("PublicStaticVoidMethodDecl")) {
                var message = String.format("Cannot assign value to a field in a static method");
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(assignStmt),
                        NodeUtils.getColumn(assignStmt),
                        message,
                        null)
                );
            }
        }
        else if (!isLocal && !isField) {
            var message = String.format("Variable %s is not declared", assignStmt.get("name"));
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assignStmt),
                    NodeUtils.getColumn(assignStmt),
                    message,
                    null)
            );
        }

        // Get the type of the variable in the name
        Type assigneeType = getVarExprType(assignStmt, table);
        if (assigneeType == null) {
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

        // Get the type of the expression
        JmmNode expression = assignStmt.getChildren().get(0);
        Type expressionType = TypeUtils.getExprType(expression, table);

        if (expression.getKind().equals("UnspecifiedTypeNewArrayExpr")) {
            // Array initializer
            if (!assigneeType.isArray() || !assigneeType.getName().equals("int")) {
                var message = String.format("Type of the assignee (%s) must be an array of integers", assigneeType);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(assignStmt),
                        NodeUtils.getColumn(assignStmt),
                        message,
                        null)
                );
            } else {
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
            }
        } else {
            // Check if the types are compatible
            if (!TypeUtils.areTypesAssignable(expressionType, assigneeType, table)) {
                var message = String.format("Type of the assignee (%s) must be compatible with the assigned (%s)", assigneeType, expressionType);
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
