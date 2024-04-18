package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.HashSet;

public class DuplicatedExpr extends AnalysisVisitor {
    private HashSet<String> params = new HashSet<>();
 
    @Override
    public void buildVisitor() {
        addVisit(Kind.PARAM, this::visitParam);
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
}