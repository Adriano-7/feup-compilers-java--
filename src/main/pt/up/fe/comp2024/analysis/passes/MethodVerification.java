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
import java.util.stream.Collectors;

public class MethodVerification extends AnalysisVisitor {
    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_CALL_EXPR, this::checkMethodCall);
    }

    private Void checkMethodCall(JmmNode expr, SymbolTable table) {
        String methodName = expr.get("name");
        List<JmmNode> arguments = expr.getChildren();

        List<Symbol> methods = table.getFields().stream()
                .filter(symbol -> symbol.getName().equals(methodName))
                .collect(Collectors.toList());

        if (methods.isEmpty()) {
            // Method does not exist, check if the class extends an imported class
            String superClass = table.getSuper();
            if (superClass == null || !table.getImports().contains(superClass)) {
                // Class does not extend an imported class, report an error
                var message = String.format("Method '%s' does not exist and class does not extend an imported class", methodName);

                addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(expr), NodeUtils.getColumn(expr), message, null));
                return null;
            }
            // Method is assumed to exist in the superclass
        } else {
            Symbol method = methods.get(0);
            List<Type> parameterTypes = table.getParameters(method.getName()).stream()
                    .map(Symbol::getType)
                    .collect(Collectors.toList());

            if (arguments.size() != parameterTypes.size()) {
                var message = String.format("Method '%s' expects %d arguments but %d were provided", methodName, parameterTypes.size(), arguments.size());
                addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(expr), NodeUtils.getColumn(expr), message, null));
                return null;
            }
            for (int i = 0; i < arguments.size(); i++) {
                Type argType = TypeUtils.getExprType(arguments.get(i), table);
                Type paramType = parameterTypes.get(i);

                if (!TypeUtils.areTypesAssignable(argType, paramType)) {
                    var message = String.format("Argument at index %d is of type '%s' but method '%s' expects type '%s'", i, argType, methodName, paramType);
                    addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(arguments.get(i)), NodeUtils.getColumn(arguments.get(i)), message, null));
                }
            }
        }

        return null;
    }
}
