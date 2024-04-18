package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.HashSet;
import java.util.List;

public class InvalidImportExpr extends AnalysisVisitor {
    private HashSet<String> importStatements = new HashSet<>();

    @Override
    public void buildVisitor() {
        addVisit(Kind.IMPORT_DECL, this::visitImportDecl);
    }

    private Void visitImportDecl(JmmNode importStmt, SymbolTable table) {
        List<JmmNode> impPackageNode;
        impPackageNode = importStmt.getChildren("ImpPackage");
        if (impPackageNode == null) {
            return null;
        }

        StringBuilder impPackage = new StringBuilder();
        for (JmmNode node : impPackageNode) {
            impPackage.append(node.get("name"));
        }

        if (importStatements.contains(impPackage.toString())) {
            var message = String.format("Duplicate import statement: '%s'", impPackage.toString());
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(importStmt),
                    NodeUtils.getColumn(importStmt),
                    message,
                    null)
            );
        } else {
            importStatements.add(impPackage.toString());
        }

        return null;
    }
}