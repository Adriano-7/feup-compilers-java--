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
    private HashSet<String> importNames = new HashSet<>();

    @Override
    public void buildVisitor() {
        addVisit(Kind.IMPORT_DECL, this::visitImportDecl);
    }

    private Void visitImportDecl(JmmNode importStmt, SymbolTable table) {
        List<JmmNode> impPackageNode;
        //Go through the children of the import statement to find the package name
        impPackageNode = importStmt.getChildren("ImpPackage");
        if (impPackageNode == null) {
            return null;
        }

        //Compile the package name
        StringBuilder impPackage = new StringBuilder();
        for (JmmNode node : impPackageNode) {
            impPackage.append(node.get("name"));
        }

        String[] splitImport = impPackage.toString().split("\\.");
        String importedName = splitImport[splitImport.length - 1];

        //Check if the package name is already in the import names
        if (importNames.contains(importedName)) {
            var message = String.format("Duplicate import name: '%s'", importedName);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(importStmt),
                    NodeUtils.getColumn(importStmt),
                    message,
                    null)
            );
        } else {
            importNames.add(importedName);
        }


        //Check if the package name is already in the import statements
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