package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.ArrayList;

import static pt.up.fe.comp2024.ast.Kind.*;
import static pt.up.fe.comp2024.optimization.OptUtils.toOllirType;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends AJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";

    private final SymbolTable table;

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
    }

    @Override
    protected void buildVisitor() {
        addVisit(VAR_REF_EXPR, this::visitVarRef);
        addVisit(BINARY_EXPR, this::visitBinExpr);
        addVisit(INTEGER_LITERAL, this::visitInteger);
        addVisit(METHOD_CALL_EXPR, this::visitMethodCall);
        addVisit(NEW_OBJECT_EXPR, this::visitNewObjectExpr);
        addVisit(SPECIFIC_TYPE_NEW_ARRAY_EXPR, this::visitNewArrayObjectExpr);
        addVisit(UNARY_EXPR, this::visitUnaryExpr);
        addVisit(BOOLEAN_LITERAL, this::visitBooleanLiteral);

        setDefaultVisit(this::defaultVisit);
    }


    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        var intType = new Type(TypeUtils.getIntTypeName(), false);
        String ollirIntType = OptUtils.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }


    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {

        var lhs = visit(node.getJmmChild(0));
        var rhs = visit(node.getJmmChild(1));

        if(rhs.getCode().contains("invoke")){
            //Use OptUtils.getTemp() to get a new temporary variable and store the code in the computation
            String temp = OptUtils.getTemp() + OptUtils.toOllirType(TypeUtils.getExprType(node, table));
            StringBuilder computation = new StringBuilder();
            computation.append(temp).append(SPACE).append(ASSIGN).append(".i32").append(SPACE).append(rhs.getCode()).append(END_STMT);
            computation.append(lhs.getComputation());
            //new temporary variable
            String temp1 = OptUtils.getTemp() + OptUtils.toOllirType(TypeUtils.getExprType(node, table));
            computation.append(temp1).append(SPACE).append(ASSIGN).append(".i32").append(SPACE).append(lhs.getCode()).append(SPACE).append(node.get("op")).append(OptUtils.toOllirType(TypeUtils.getExprType(node, table))).append(SPACE).append(temp).append(END_STMT);

            return new OllirExprResult(temp1, computation);
        }

        StringBuilder computation = new StringBuilder();

        // code to compute the children
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());

        // code to compute self
        Type resType = TypeUtils.getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);
        String code = OptUtils.getTemp() + resOllirType;

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(lhs.getCode()).append(SPACE);

        Type type = TypeUtils.getExprType(node, table);
        computation.append(node.get("op")).append(OptUtils.toOllirType(type)).append(SPACE)
                .append(rhs.getCode()).append(END_STMT);

        return new OllirExprResult(code, computation);
    }


    private OllirExprResult visitVarRef(JmmNode node, Void unused) {

        var id = node.get("name");
        Type type = TypeUtils.getExprType(node, table);
        String ollirType = OptUtils.toOllirType(type);

        String code = id + ollirType;

        return new OllirExprResult(code);
    }

    private OllirExprResult visitMethodCall(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        StringBuilder code = new StringBuilder();

        String methodName = node.get("name");
        String methodType;

        JmmNode caller = node.getJmmChild(0);
        String callerName, callerType;

        Type callerTypeNode = TypeUtils.getExprType(caller, table);

        if(caller.getKind().equals("ThisExpr")) {
            callerName = "this";
            callerType = "V";
        } else {
            callerName = caller.get("name");
            callerType = OptUtils.toOllirType(new Type(caller.get("type"), caller.getOptional("isArray").map(Boolean::parseBoolean).orElse(false)));
        }

        if (!caller.getKind().equals("ThisExpr")) {
            if (this.table.getImports().contains(callerName)) {
                callerName = caller.get("name");
                callerType = callerTypeNode.getName();
            } else {
                String[] callerSplit = callerName.split("\\.");
                callerType = callerTypeNode.getName();
            }
        } else {
            callerName = "this";
            callerType = table.getClassName();
        }


        String invokeType;
        if (callerType.equals(table.getClassName()) || callerType.equals("this")) {
            invokeType = "invokevirtual";
            methodType = toOllirType(table.getReturnType(methodName));
        } else {
            if (table.getImports().contains(callerType)) {
                invokeType = "invokevirtual";
            } else {
                invokeType = "invokestatic";
            }
            methodType = ".V";
        }

        ArrayList<String> args = new ArrayList<String>();
        for (int i = 1; i < node.getNumChildren(); i++) {
            String arg = visit(node.getJmmChild(i)).getCode();
            args.add(arg);
        }

        String argsString;
        if (args.size() > 0) {
            argsString = ", " + String.join(", ", args);
        } else {
            argsString = "";
        }

        //invokeMethod + "(" + callerName.callerType + ", " + "\"" + methodName + "\"" + argsString + ")"+ returnType
        code.append(invokeType).append("(").append(callerName).append(".").append(callerType).append(", ").append("\"").append(methodName).append("\"").append(argsString).append(")").append(methodType);

        OllirExprResult result = new OllirExprResult(code.toString(), computation.toString());
        return result;
    }
    
    private OllirExprResult visitNewObjectExpr(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        StringBuilder code = new StringBuilder();

        String newObj = "new(" + node.get("type") + ")" + OptUtils.toOllirType(TypeUtils.getExprType(node, table));

        Type callType = TypeUtils.getExprType(node, table);
        String temp = OptUtils.getTemp() + OptUtils.toOllirType(callType);

        computation.append(temp).append(SPACE).append(ASSIGN).append(OptUtils.toOllirType(callType)).append(SPACE).append(newObj).append(END_STMT);
        computation.append("invokespecial(").append(temp).append(",\"<init>\").V;\n");

        return new OllirExprResult(temp, computation);
    }

    /**
     * Default visitor. Visits every child node and return an empty result.
     *
     * @param node
     * @param unused
     * @return
     */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return OllirExprResult.EMPTY;
    }


    private OllirExprResult visitNewArrayObjectExpr(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        StringBuilder code = new StringBuilder();

        String size = visit(node.getJmmChild(0)).getCode();
        String type = OptUtils.toOllirType(new Type(node.get("type"), node.getOptional("isArray").map(Boolean::parseBoolean).orElse(false)));

        String temp = OptUtils.getTemp() + type;

        computation.append(temp).append(SPACE).append(ASSIGN).append(type).append(SPACE).append("new(array,").append(size).append(")").append(type).append(END_STMT);

        return new OllirExprResult(temp, computation);
    }

    private OllirExprResult visitUnaryExpr(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();

        // Get the operation
        String op = node.get("op");

        // Visit the child expression
        var child = visit(node.getJmmChild(0));

        // Code to compute the child
        computation.append(child.getComputation());

        // Code to compute self
        Type resType = TypeUtils.getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);
        String code = op + resOllirType + SPACE + child.getCode();

        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitBooleanLiteral(JmmNode node, Void unused) {
        var boolType = new Type("boolean", false);
        String ollirBoolType = OptUtils.toOllirType(boolType);
        String code = node.get("value") + ollirBoolType;
        return new OllirExprResult(code);
    }
}
