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
import pt.up.fe.specs.util.SpecsCheck;

/**
 *
 * Checks if the operands in an expression are compatible with the operation
 *
 */
public class BinaryInvalidExpr extends AnalysisVisitor {
    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
    }

    private Void visitBinaryExpr(JmmNode binaryExpr, SymbolTable table) {
        // Get the left and right operands of the binary expression
        JmmNode leftOperand = binaryExpr.getChildren().get(0);
        JmmNode rightOperand = binaryExpr.getChildren().get(1);

        // Get the types of the left and right operands
        Type leftType = TypeUtils.getExprType(leftOperand, table);
        Type rightType = TypeUtils.getExprType(rightOperand, table);

        String operator = binaryExpr.get("op");

        if (!checkCompatibility(leftType, rightType, operator)) {
            var message = String.format("Operands of type '%s' and '%s' are not compatible with operator '%s'", leftType, rightType, operator);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(binaryExpr),
                    NodeUtils.getColumn(binaryExpr),
                    message,
                    null)
            );
        }

        return null;
    }
    
    private boolean checkCompatibility(Type leftType, Type rightType, String operator) {
        String leftTypeName = leftType.getName();
        String rightTypeName = rightType.getName();
        boolean leftIsArray = leftType.isArray();
        boolean rightIsArray = rightType.isArray();

        return switch (operator) {
            case "+", "*", "-", "/" ->
                    leftTypeName.equals("int") && rightTypeName.equals("int") && !leftIsArray && !rightIsArray;
            case "<" -> leftType.equals(rightType) && !leftIsArray && !rightIsArray;
            default -> true;
        };
    }
}
