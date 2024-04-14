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
import pt.up.fe.comp2024.symboltable.VarargSymbol;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.List;
import java.util.stream.Collectors;

import static pt.up.fe.comp2024.ast.TypeUtils.isImported;

/*
• When calling methods of the class declared in the code, verify if the types of arguments of the
call are compatible with the types in the method declaration
• If the calling method accepts varargs, it can accept both a variable number of arguments of
the same type as an array, or directly an array
• In case the method does not exist, verify if the class extends an imported class and report an
error if it does not.
    – If the class extends another class, assume the method exists in one of the super classes,
    and that is being correctly called
• When calling methods that belong to other classes other than the class declared in the code,
verify if the classes are being imported.
    – If a class is being imported,
    assume the types of the expression where it is used are correct. For instance, for the code
    bool a; a = M.foo();, if M is an imported class, then assume it has a method named
    foo without parameters that returns a boolean.
*/

public class MethodVerification extends AnalysisVisitor {
    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_CALL_EXPR, this::checkMethodCall);
    }

    private Void checkMethodCall(JmmNode expr, SymbolTable table) {
        JmmNode callerExpr = expr.getChildren().get(0);
        String methodName = expr.get("name");

        Type callerType = TypeUtils.getExprType(callerExpr, table);
        List<JmmNode> arguments = expr.getChildren().subList(1, expr.getNumChildren());

        // Annotate arguments
        for(JmmNode argument : arguments) {
            Type argType = TypeUtils.getExprType(argument, table);
            argument.put("type", argType.getName());
            argument.put("isArray", argType.isArray() ? "true" : "false");
        }

        // If the caller type is imported, assume the types of the expression where it is used are correct
        if (callerType != null && table.getImports().contains(callerType.getName())) {
            expr.put("type", callerType.getName());
            expr.put("isArray", callerType.isArray() ? "true" : "false");
            return null;
        }

        if (!table.getMethods().contains(methodName)) {
            // Method does not exist, check if the class extends an imported class
            String superClass = table.getSuper();
            if (superClass == null || !table.getImports().contains(superClass)) {
                var message = String.format("Method '%s' does not exist and class does not extend an imported class", methodName);

                addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(expr), NodeUtils.getColumn(expr), message, null));
                return null;
            }
            // Method is assumed to exist in the superclass
        } else {
            List<VarargSymbol> parameterSymbols = table.getParameters(methodName).stream().map(VarargSymbol.class::cast).collect(Collectors.toList());

            int parameterIndex = 0;

            for(int argumentIndex = 0; argumentIndex < arguments.size(); argumentIndex++) {
                Type argType = TypeUtils.getExprType(arguments.get(argumentIndex), table);
                Type paramType = parameterSymbols.get(parameterIndex).getType();
                boolean isVarArg = parameterSymbols.get(parameterIndex).isVararg();

                if (parameterIndex == parameterSymbols.size() - 1 && isVarArg) {
                    // If the parameter is the last one and is varargs, then consume the remaining arguments
                    while (argumentIndex < arguments.size()) {
                        argType = TypeUtils.getExprType(arguments.get(argumentIndex), table);
                        if (!argType.getName().equals("int")) {
                            var message = String.format("Argument at index %d is of type '%s' but method '%s' expects type int", argumentIndex, argType, methodName);
                            addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(arguments.get(argumentIndex)), NodeUtils.getColumn(arguments.get(argumentIndex)), message, null));
                        }
                        argumentIndex++;
                    }
                }
                else if (TypeUtils.areTypesAssignable(argType, paramType)) {
                      parameterIndex++;
                }
                else {
                    var message = String.format("Argument at index %d is of type '%s' but method '%s' expects type '%s'", argumentIndex, argType, methodName, paramType);
                    addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(arguments.get(argumentIndex)), NodeUtils.getColumn(arguments.get(argumentIndex)), message, null));
                }
            }
        }

        if (callerType == null) {
            addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(callerExpr), NodeUtils.getColumn(callerExpr), "Cannot find the type of the caller", null));
            return null;
        }

        expr.put("type", callerType.getName());
        expr.put("isArray", callerType.isArray() ? "true" : "false");

        return null;
    }
}
