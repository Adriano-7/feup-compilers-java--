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
  Checks if a vararg type is used correctly.
     • A vararg type when used, must always be the type of the last parameter in a method declaration. Also, only one parameter can be vararg, but the method can have several parameters
     • Variable declarations, field declarations and method returns cannot be vararg
 */
public class VarArgInvalidExpr extends AnalysisVisitor {
    private String currentMethod;

    @Override
    public void buildVisitor() {
    }
}

