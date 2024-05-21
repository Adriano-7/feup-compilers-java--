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
 Checks if arrays are used correctly.
 • Array access is done over an array
 • Array access index is an expression of type integer
 */
public class ArrayInvalidExpr extends AnalysisVisitor {
    @Override
    public void buildVisitor() {
        addVisit(Kind.ARRAY_ACCESS_EXPR, this::visitArrayAccessExpr);
        addVisit(Kind.ARRAY_LENGTH_EXPR, this::visitArrayLengthExpr);
    }

    private Void visitArrayAccessExpr(JmmNode binaryExpr, SymbolTable table) {
        JmmNode array = binaryExpr.getChildren().get(0);
        Type arrayType = TypeUtils.getExprType(array, table);

        if (!arrayType.isArray()) {
            var message = String.format("Array access is done over an expression of type '%s'", arrayType);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(binaryExpr),
                    NodeUtils.getColumn(binaryExpr),
                    message,
                    null)
            );
        }

        JmmNode index = binaryExpr.getChildren().get(1);
        if (index == null) {
            // Array length expression
            binaryExpr.put("type", "int");
            binaryExpr.put("isArray", "false");
        } else {
            Type indexType = TypeUtils.getExprType(index, table);
            if (!indexType.getName().equals("int")) {
                var message = String.format("Array access index is an expression of type '%s'", indexType);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(binaryExpr),
                        NodeUtils.getColumn(binaryExpr),
                        message,
                        null)
                );
            }

            binaryExpr.put("type", arrayType.getName());
            binaryExpr.put("isArray", "true");
        }

        return null;
    }

    private Void visitArrayLengthExpr(JmmNode arrayLengthExpr, SymbolTable table) {
        if(!arrayLengthExpr.get("name").equals("length")) {
            var message = "Calling a method that is not 'length'.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(arrayLengthExpr),
                    NodeUtils.getColumn(arrayLengthExpr),
                    message,
                    null)
            );
        }

        JmmNode array = arrayLengthExpr.getChildren().get(0);
        Type arrayType = TypeUtils.getExprType(array, table);

        if (!arrayType.isArray()) {
            var message = String.format("Array length is done over an expression of type '%s'", arrayType);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(arrayLengthExpr),
                    NodeUtils.getColumn(arrayLengthExpr),
                    message,
                    null)
            );
        }

        arrayLengthExpr.put("type", "int");
        arrayLengthExpr.put("isArray", "false");

        return null;
    }
}
