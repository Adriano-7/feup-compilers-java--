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
        addVisit(ARRAY_LENGTH_EXPR, this::visitArrayLengthExpr);
        addVisit(ARRAY_ACCESS_EXPR, this::visitArrayAccessExpr);

        setDefaultVisit(this::defaultVisit);
    }


    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        String code = node.get("value") + OptUtils.toOllirType("int");
        return new OllirExprResult(code);
    }

    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {
        var lhs = visit(node.getJmmChild(0));
        var rhs = visit(node.getJmmChild(1));

        StringBuilder computation = new StringBuilder();

        if (node.get("op").equals("&&")) {
            // Label for the short-circuit evaluation
            String trueLabel = OptUtils.getLabel("true");
            String endLabel = OptUtils.getLabel("end");

            computation.append(lhs.getComputation());
            String tmp1 = OptUtils.getTemp() + ".bool";
            computation.append("if(").append(lhs.getCode()).append(") goto ").append(trueLabel).append(END_STMT);
            computation.append(tmp1).append(" :=.bool 0.bool").append(END_STMT);
            computation.append("goto ").append(endLabel).append(END_STMT);

            computation.append(trueLabel).append(":\n");
            String tmp2 = OptUtils.getTemp() + ".bool";
            computation.append(tmp2).append(" :=.bool ").append(rhs.getCode()).append(END_STMT);
            computation.append(tmp1).append(" :=.bool ").append(tmp2).append(END_STMT);

            computation.append(endLabel).append(":\n");
            return new OllirExprResult(tmp1, computation);
        }

        if(rhs.getCode().contains("invoke")){
            String temp = OptUtils.getTemp() + OptUtils.toOllirType(node, table);
            computation.append(lhs.getComputation());

            String lhsTemp = OptUtils.getTemp() + OptUtils.toOllirType(node, table);
            computation.append(lhsTemp)
                    .append(SPACE)
                    .append(ASSIGN)
                    .append(OptUtils.toOllirType(node, table))
                    .append(SPACE)
                    .append(lhs.getCode())
                    .append(END_STMT);

            String rhsTemp = OptUtils.getTemp() + OptUtils.toOllirType(node, table);
            computation.append(rhsTemp)
                    .append(SPACE)
                    .append(ASSIGN)
                    .append(OptUtils.toOllirType(node, table))
                    .append(SPACE)
                    .append(rhs.getCode())
                    .append(END_STMT);

            String resultTemp = OptUtils.getTemp() + OptUtils.toOllirType(node, table);
            computation.append(resultTemp)
                    .append(SPACE)
                    .append(ASSIGN)
                    .append(OptUtils.toOllirType(node, table))
                    .append(SPACE)
                    .append(lhsTemp)
                    .append(SPACE)
                    .append(node.get("op"))
                    .append(OptUtils.toOllirType(node, table))
                    .append(SPACE)
                    .append(rhsTemp)
                    .append(END_STMT);

            return new OllirExprResult(resultTemp, computation);
        }

        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());

        String resOllirType = OptUtils.toOllirType(node, table);
        String code = OptUtils.getTemp() + resOllirType;

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(lhs.getCode()).append(SPACE);

        computation.append(node.get("op")).append(OptUtils.toOllirType(node, table)).append(SPACE)
                .append(rhs.getCode()).append(END_STMT);

        return new OllirExprResult(code, computation);
    }



    private OllirExprResult visitVarRef(JmmNode node, Void unused) {
        var id = node.get("name");
        String ollirType = OptUtils.toOllirType(node,table);

        String parameterNumber = OptUtils.getParameterNumber(node, table);
        String code;
        if (!parameterNumber.isEmpty()) {
            code = parameterNumber + "." + id + ollirType;
        } else {
            code = id + ollirType;
        }

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

        if (caller.getKind().equals("ThisExpr")) {
            callerName = "this";
            callerType = table.getClassName();
        } else {
            callerName = caller.get("name");
            callerType = callerTypeNode.getName();
        }

        String invokeType;
        if (callerType.equals(table.getClassName()) || callerType.equals("this")) {
            invokeType = "invokevirtual";
            methodType = toOllirType(node,table);
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

        code
            .append(invokeType)
            .append("(")
            .append(callerName)
            .append(".")
            .append(callerType)
            .append(", ")
            .append("\"")
            .append(methodName)
            .append("\"")
            .append(argsString)
            .append(")")
            .append(methodType);

        OllirExprResult result = new OllirExprResult(code.toString(), computation.toString());
        return result;
    }
    
    private OllirExprResult visitNewObjectExpr(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        StringBuilder code = new StringBuilder();

        String newObj = "new(" + node.get("type") + ")" + OptUtils.toOllirType(node,table);

        String temp = OptUtils.getTemp() + OptUtils.toOllirType(node,table);

        computation.append(temp).append(SPACE).append(ASSIGN).append(OptUtils.toOllirType(node,table)).append(SPACE).append(newObj).append(END_STMT);
        computation.append("invokespecial(").append(temp).append(",\"<init>\").V;\n");

        return new OllirExprResult(temp, computation);
    }

    private OllirExprResult visitNewArrayObjectExpr(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        StringBuilder code = new StringBuilder();

        String size = visit(node.getJmmChild(0)).getCode();
        String type = OptUtils.toOllirType(node,table);

        String temp = OptUtils.getTemp() + type;

        computation.append(temp).append(SPACE).append(ASSIGN).append(type).append(SPACE).append("new(array,").append(size).append(")").append(type).append(END_STMT);

        return new OllirExprResult(temp, computation);
    }

    private OllirExprResult visitUnaryExpr(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();

        String op = node.get("op");

        var child = visit(node.getJmmChild(0));
        computation.append(child.getComputation());

        String resOllirType = OptUtils.toOllirType(node,table);
        String code = op + resOllirType + SPACE + child.getCode();

        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitBooleanLiteral(JmmNode node, Void unused) {
        String ollirBoolType = OptUtils.toOllirType(node,table);
        String code = node.get("value") + ollirBoolType;
        return new OllirExprResult(code);
    }

    private OllirExprResult visitArrayLengthExpr(JmmNode node, Void unused) {
        JmmNode arrayNode = node.getJmmChild(0);
        OllirExprResult arrayResult = visit(arrayNode);

        StringBuilder computation = new StringBuilder();
        computation.append(arrayResult.getComputation());

        String temp = OptUtils.getTemp() + OptUtils.toOllirType("int");
        computation.append(temp)
                .append(" :=.i32 ")
                .append("arraylength(")
                .append(arrayResult.getCode())
                .append(").i32;\n");

        return new OllirExprResult(temp, computation);
    }

    /*
    Code:
    		result = a[0];
    Tree:
        AssignStmt (name: result, isArray: false, type: int)
            ArrayAccessExpr (isArray: true, type: int)
               VarRefExpr (name: a, isArray: true, type: int)
               IntegerLiteral (isArray: false, type: int, value: 0)
    OLLIR we want to generate:
            result.i32 :=.i32 $1.a[0.i32].i32;
    */
    private OllirExprResult visitArrayAccessExpr(JmmNode node, Void unused){
        StringBuilder code = new StringBuilder();

        JmmNode arrayNode = node.getJmmChild(0);
        JmmNode indexNode = node.getJmmChild(1);

        String parameterIndex = OptUtils.getParameterNumber(arrayNode, table);
        String arrayName = arrayNode.get("name");
        String arrayType = OptUtils.toOllirType(arrayNode, table, false);

        OllirExprResult indexResult = visit(indexNode);
        code.append(indexResult.getComputation());

        if(!parameterIndex.isEmpty()){
            code.append(parameterIndex).append(".");
        }
        code.append(arrayName)
        .append("[")
        .append(indexResult.getCode())
        .append("]")
        .append(arrayType);

        return new OllirExprResult(code.toString());
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
}
