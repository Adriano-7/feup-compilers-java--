package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.SpecsCheck;

/**
 Checks if the keyword “this” is used correctly.
 • “this” expression cannot be used in a static method
 • “this” can be used as an “object” (e.g. A a; a = this; is correct if the declared class is A or the declared class extends A)
 */
public class ThisInvalidUse extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
    }
}
