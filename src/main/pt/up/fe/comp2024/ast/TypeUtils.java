package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class TypeUtils {

    private static final String INT_TYPE_NAME = "int";
    private static final String BOOLEAN_TYPE_NAME = "boolean";

    public static String getIntTypeName() {
        return INT_TYPE_NAME;
    }

    /**
     * Gets the {@link Type} of an arbitrary expression.
     *
     * @param expr
     * @param table
     * @return
     */
    public static Type getExprType(JmmNode expr, SymbolTable table) {
        // TODO: Simple implementation that needs to be expanded
        var kind = Kind.fromString(expr.getKind());

        Type type = switch (kind) {
            case BINARY_EXPR -> getBinExprType(expr);
            case VAR_REF_EXPR -> getVarExprType(expr, table);
            case INTEGER_LITERAL -> new Type(INT_TYPE_NAME, false);
            case BOOLEAN_LITERAL -> new Type(BOOLEAN_TYPE_NAME, false);
            case NEW_OBJECT_EXPR -> new Type(expr.get("name"), false);
            case UNSPECIFIED_TYPE_NEW_ARRAY_EXPR -> getUnspecifiedTypeNewArrayExprType(expr, table);
            default -> throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
        };

        if (type != null) {
            expr.put("type", type.getName());
            expr.put("isArray", type.isArray() ? "true" : "false");
        }

        return type;
    }

    private static Type getBinExprType(JmmNode binaryExpr) {
        // TODO: Simple implementation that needs to be expanded

        String operator = binaryExpr.get("op");

        return switch (operator) {
            case "+", "-", "*", "/" -> new Type(INT_TYPE_NAME, false);
            case "<", "&&" -> new Type(BOOLEAN_TYPE_NAME, false);
            default ->
                    throw new RuntimeException("Unknown operator '" + operator + "' of expression '" + binaryExpr + "'");
        };
    }

    public static Type getVarExprType(JmmNode varRefExpr, SymbolTable table) {
        String varName = varRefExpr.get("name");
        JmmNode parent = varRefExpr.getParent();
        while (!parent.getKind().equals("ImportDecl") && !parent.getKind().equals("PublicMethodDecl") && !parent.getKind().equals("PublicStaticVoidMethodDecl")) {
            parent = parent.getParent();
        }

        if(!parent.getKind().equals("ImportDecl")) {
            String methodName = parent.get("name");
            var localVariable = table.getLocalVariables(methodName).stream()
                    .filter(varDecl -> varDecl.getName().equals(varName))
                    .findFirst();

            if (localVariable.isPresent()) {
                return localVariable.get().getType();
            }

            var parameter = table.getParameters(methodName).stream()
                    .filter(param -> param.getName().equals(varName))
                    .findFirst();
            if (parameter.isPresent()) {
                return parameter.get().getType();
            }

            var field = table.getFields().stream()
                    .filter(param -> param.getName().equals(varName))
                    .findFirst();
            if (field.isPresent()) {
                return field.get().getType();
            }

            if((table.getImports() == null || !table.getImports().contains(varName)) && (table.getSuper() == null || !table.getSuper().equals(varName)) && !table.getClassName().equals(varName)) {
                throw new RuntimeException("Variable '" + varName + "' not found in method '" + methodName + "'");
            }

            if(table.getImports() != null && table.getImports().contains(varName)) {
                return new Type(varName, false);
            }
        }

        return null;
    }

    private static Type getUnspecifiedTypeNewArrayExprType(JmmNode unspecifiedTypeNewArrayExpr, SymbolTable table) {
        JmmNode assignStmt = unspecifiedTypeNewArrayExpr.getParent();
        return getVarExprType(assignStmt, table);
    }

    public static boolean isImported(Type type) {
        return !(type.getName().equals("int") || type.getName().equals("boolean"));
    }

    /**
     * @param sourceType
     * @param destinationType
     * @return true if sourceType can be assigned to destinationType
     */
    public static boolean areTypesAssignable(Type sourceType, Type destinationType) {
        return sourceType.getName().equals(destinationType.getName());
    }
}

