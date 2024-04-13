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
 * Checks that array accesses is done over an array and that array access index is an integer
 *
 */
public class ArrayInvalidExpr extends AnalysisVisitor {
    @Override
    public void buildVisitor() {
        addVisit(Kind.ARRAY_ACCESS_EXPR, this::visitArrayAccessExpr);
    }

    private Void visitArrayAccessExpr(JmmNode binaryExpr, SymbolTable table) {
        JmmNode array = binaryExpr.getChildren().get(0);
        JmmNode index = binaryExpr.getChildren().get(1);

        Type arrayType = TypeUtils.getExprType(array, table);
        Type indexType = TypeUtils.getExprType(index, table);

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
        return null;
    }
}
