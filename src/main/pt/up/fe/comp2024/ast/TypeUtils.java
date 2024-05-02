package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.symboltable.JmmSymbolTable;

import java.util.Optional;

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

        Type type = getOptionalType(expr);
        if(type != null) {
            return type;
        }

         type = switch (kind) {
            case BINARY_EXPR -> getBinExprType(expr);
            case VAR_REF_EXPR -> getVarExprType(expr, table);
            case INTEGER_LITERAL -> new Type(INT_TYPE_NAME, false);
            case BOOLEAN_LITERAL -> new Type(BOOLEAN_TYPE_NAME, false);
            case NEW_OBJECT_EXPR -> new Type(expr.get("name"), false);
            case UNSPECIFIED_TYPE_NEW_ARRAY_EXPR -> getUnspecifiedTypeNewArrayExprType(expr, table);
            case METHOD_CALL_EXPR -> getMethodCallExprType(expr, table);
            case SPECIFIC_TYPE_NEW_ARRAY_EXPR -> new Type(INT_TYPE_NAME, true);
            case THIS_EXPR -> getThisExprType(expr, table);
            case ARRAY_ACCESS_EXPR -> new Type(INT_TYPE_NAME, false);
            case STRING_TYPE -> new Type("String", false);
            case INT_TYPE -> new Type(INT_TYPE_NAME, false);
            default -> throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
        };

        if (type != null && !expr.getOptional("type").isPresent()) {
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
                //throw new RuntimeException("Variable '" + varName + "' not found in method '" + methodName + "'");
                return null;
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

    public static Type getOptionalType(JmmNode node) {
        Optional<String> type = node.getOptional("type");
        Optional<String> isArray = node.getOptional("isArray");

        if(type.isEmpty()) {
            return null;
        }
        else if (isArray.isEmpty()) {
            return new Type(type.get(), false);
        } else {
            return new Type(type.get(), Boolean.parseBoolean(isArray.get()));

        }
    }

    private static Type getMethodCallExprType(JmmNode methodCallExpr, SymbolTable table) {
        String methodName = methodCallExpr.get("name");
        Optional<Type> type = table.getReturnTypeTry(methodName);

        if(type.isPresent()) {
            return type.get();
        }

        JmmNode methodCaller = methodCallExpr.getChild(0);
        JmmNode parent = methodCallExpr.getParent();

        if(methodCaller.getKind().equals("ThisExpr")) {
            //Scope is the class
            while (!parent.getKind().equals("ClassStmt")) {
                parent = parent.getParent();
            }

            Type methodType = table.getReturnType(methodName);
            if (methodType == null) {
                String superClass = table.getSuper();
                if (superClass == null || !table.getImports().contains(superClass)) {
                    return null;
                }
                return new Type(superClass, false);
            }

            return methodType;
        }
        else if(isImported(new Type(methodCaller.get("name"), false), table)) {
            return new Type(methodCaller.get("name"), false);
        }

        while (!parent.getKind().equals("ImportDecl") && !parent.getKind().equals("PublicMethodDecl") && !parent.getKind().equals("PublicStaticVoidMethodDecl")) {
            parent = parent.getParent();
        }

        if(!parent.getKind().equals("ImportDecl")) {
            String methodNameParent = parent.get("name");
            Type methodType = table.getReturnType(methodName);

            if (methodType == null) {



                String superClass = table.getSuper();
                if (superClass == null || !table.getImports().contains(superClass)) {
                    return null;
                }
                return new Type(superClass, false);
            }

            return methodType;
        }

        return null;
    }

    private static Type getThisExprType(JmmNode thisExpr, SymbolTable table) {
        var parent = getMethodCallExprType(thisExpr.getParent(), table);

        if(thisExpr.getParent().getKind().equals("MethodCallExpr")) {
            return getMethodCallExprType(thisExpr.getParent(), table);
        }

        if(thisExpr.getParent().getKind().equals("AssignStmt")) {
            return getVarExprType(thisExpr.getParent(), table);
        }

        return null;
    }

    public static boolean isImported(Type type, SymbolTable table) {
       return table.getImports().contains(type.getName());
    }

    /**
     * @param sourceType
     * @param destinationType
     * @return true if sourceType can be assigned to destinationType
     */
    public static boolean areTypesAssignable(Type sourceType, Type destinationType, SymbolTable table) {
        if(sourceType == null || destinationType == null) {
            return false;
        }

        // Check if the types are the same
        if (sourceType.getName().equals(destinationType.getName())) {
            if(!sourceType.isArray() && destinationType.isArray()) {
                return false;
            }

            return true;
        }

        String superClass = table.getSuper();
        if (superClass != null && superClass.equals(destinationType.getName())) {
            return true;
        }

        //If destinationType is imported and sourceType is an object of the class that does not extend the imported class return false
        if ((isImported(destinationType, table) && (sourceType.getName().equals(table.getClassName())
                && (superClass==null || !superClass.equals(destinationType.getName()))
        ))) {
            return false;
        }

        if (isImported(sourceType, table) || isImported(destinationType, table)) {
            return true;
        }

        return false;
    }
}