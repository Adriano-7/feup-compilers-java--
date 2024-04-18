package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.HashSet;

public class InvalidImportExpr extends AnalysisVisitor {
    private HashSet<String> importStatements = new HashSet<>();

    @Override
    public void buildVisitor() {
        addVisit(Kind.IMPORT_DECL, this::visitImportDecl);
    }

    private Void visitImportDecl(JmmNode importStmt, SymbolTable table) {
        JmmNode impPackageNode = importStmt.getChildren().stream()
            .filter(child -> child.getKind().equals("ImpPackage"))
            .findFirst()
            .orElse(null);

        if (impPackageNode == null) {
            return null;
        }

        String importName = impPackageNode.get("name");
        if (importStatements.contains(importName)) {
            var message = String.format("Duplicate import statement: '%s'", importName);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(importStmt),
                    NodeUtils.getColumn(importStmt),
                    message,
                    null)
            );
        } else {
            importStatements.add(importName);
        }
        return null;
    }
}