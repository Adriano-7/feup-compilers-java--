package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static pt.up.fe.comp2024.ast.Kind.*;
import static pt.up.fe.comp2024.optimization.OptUtils.toOllirType;

/**
 * Generates OLLIR code from JmmNodes that are not expressions.
 */
public class OllirGeneratorVisitor extends AJmmVisitor<Void, String> {
    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final String L_BRACKET = " {\n";
    private final String R_BRACKET = "}\n";


    private final SymbolTable table;

    private final OllirExprGeneratorVisitor exprVisitor;

    public OllirGeneratorVisitor(SymbolTable table) {
        this.table = table;
        exprVisitor = new OllirExprGeneratorVisitor(table);
    }


    @Override
    protected void buildVisitor() {
        addVisit(PROGRAM, this::visitProgram);
        addVisit(CLASS_DECL, this::visitClass);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(PARAM, this::visitParam);
        addVisit(RETURN_STMT, this::visitReturn);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(METHOD_CALL_EXPR, this::visitMethodCallExpr);
        addVisit(IMPORT_DECL, this::visitImportDecl);
        addVisit(EXPR_STMT, this::visitExprStmt);
        addVisit(IF_ELSE_STMT, this::visitIfElseStmt);
        addVisit(WHILE_STMT, this::visitWhileStmt);

        setDefaultVisit(this::defaultVisit);
    }

    private String visitAssignStmt(JmmNode node, Void unused) {
        // Get the variable name from the node's attributes
        String varName = node.get("name");


        // Visit the rhs which is the only child of the node
        var rhs = exprVisitor.visit(node.getJmmChild(0));

        StringBuilder code = new StringBuilder();

        // code to compute the rhs
        code.append(rhs.getComputation());

        // code to compute self
        // statement has type of rhs
        String typeString = toOllirType(node.getJmmChild(0), table);

        // Get the type of the lhs variable

        String lhsType = toOllirType(node,table);
        if(rhs.getCode().contains("invoke")){
            //Store in a temporary variable
            String temp = OptUtils.getTemp() + typeString;
            code.append(temp);
            code.append(SPACE);
            code.append(ASSIGN);
            code.append(typeString);
            code.append(SPACE);
            code.append(rhs.getCode());
            code.append(END_STMT);

            code.append(varName);
            code.append(lhsType); // Add the type of the lhs variable
            code.append(SPACE);
            code.append(ASSIGN);
            code.append(typeString);
            code.append(SPACE);
            code.append(temp);
            code.append(END_STMT);
            return code.toString();
        }
        else{
        code.append(varName);
        code.append(lhsType); // Add the type of the lhs variable
        code.append(SPACE);
        }

        code.append(ASSIGN);
        code.append(typeString);
        code.append(SPACE);

        code.append(rhs.getCode());

        code.append(END_STMT);

        return code.toString();
    }
    private String visitReturn(JmmNode node, Void unused) {
        String typeString = toOllirType(node,table);

        StringBuilder code = new StringBuilder();

        if (node.getNumChildren() > 0) {
            var expr = exprVisitor.visit(node.getJmmChild(0));
            code.append(expr.getComputation());
            code.append("ret");
            code.append(typeString);
            code.append(SPACE);
            code.append(expr.getCode());
            code.append(END_STMT);
        } else {
            code.append("ret");
            code.append(typeString);
            code.append(";\n");
        }

        return code.toString();
    }

    private String visitParam(JmmNode node, Void unused) {
        var typeCode = toOllirType(node.getJmmChild(0),table);
        var id = node.get("name");

        String code = id + typeCode;

        return code;
    }

    private String visitMethodDecl(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder(".method ");

        boolean isPublic = NodeUtils.getBooleanAttribute(node, "isPublic", "false");
        boolean isStatic = node.getOptional("name").map(name -> name.equals("main")).orElse(false);


        if (isPublic) {
            code.append("public ");
        }

        if (isStatic) {
            code.append("static ");
        }

        // name
        var name = node.get("name");
        code.append(name);

        // param
        String paramCode = "";
        List<Symbol> parameters = table.getParameters(name);
        for (Symbol parameter : parameters) {
            String paramName = parameter.getName();
            String paramType = toOllirType(parameter.getType());
            if (parameter.getType().isArray()) {
                paramType = ".array" + paramType;
            }
            paramCode += paramName + paramType + ", ";
        }

        // Remove the trailing comma and space
        if (!parameters.isEmpty()) {
            paramCode = paramCode.substring(0, paramCode.length() - 2);
        }
        code.append("(" + paramCode + ")");

        // type
        String retType = "";
        if (name.equals("main")) {
            retType = ".V";
        }else if (node.getNumChildren() > 0) {
            retType = toOllirType(node.getJmmChild(0),table);
        }

        code.append(retType);
        code.append(L_BRACKET);

        // rest of its children stmts
        int numParams = table.getParameters(name).size(); // Use symbol table to get number of parameters

        if (name.equals("main")){
            for (var child : node.getChildren()) {
                code.append(visit(child));
            }
        } else {
            if(node.getNumChildren() > numParams) {
                var children = node.getChildren().subList(numParams+1, node.getNumChildren());
                for (var child : children) {
                    code.append(visit(child));
                }
            }
        }


        if (name.equals("main")) {
            code.append("ret.V;\n");
        }

        code.append(R_BRACKET);
        code.append(NL);

        return code.toString();
    }

    private String visitClass(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        code.append(table.getClassName());

        // Get the superclass name
        String superClass = table.getSuper();
        if (superClass != null && !superClass.isEmpty()) {
            code.append(" extends ");
            code.append(superClass);
        }
//        else {
//            code.append(" extends Object");
//        }

        code.append(L_BRACKET);
        code.append(NL);

        // Generate fields
        for (Symbol field : table.getFields()) {
            String fieldName = field.getName();
            String fieldType = toOllirType(field.getType());
            if (field.getType().isArray()) {
                fieldType = "array" + fieldType;
            }
            code.append(".field private ");
            code.append(fieldName);
            code.append(fieldType);
            code.append(";\n");
        }

        code.append(NL);

        var needNl = true;

        for (var child : node.getChildren()) {
            var result = visit(child);

            if (METHOD_DECL.check(child) && needNl) {
                code.append(NL);
                needNl = false;
            }

            code.append(result);
        }

        code.append(buildConstructor());
        code.append(R_BRACKET);

        return code.toString();
    }
    private String buildConstructor() {
        return ".construct " + table.getClassName() + "().V {\n" +
                "invokespecial(this, \"<init>\").V;\n" +
                "}\n";
    }


    private String visitProgram(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        node.getChildren().stream()
                .map(this::visit)
                .forEach(code::append);

        return code.toString();
    }

    private String visitMethodCallExpr(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        // Get the method name
        String methodName = node.get("name");

        // Get the method's arguments
        List<JmmNode> arguments = new ArrayList<>();
        if (node.getNumChildren() > 0) {
            arguments = node.getChildren().subList(1, node.getNumChildren());
        }

        // Generate the OLLIR code for each argument
        for (JmmNode argument : arguments) {
            var arg = exprVisitor.visit(argument);
            code.append(arg.getComputation());
        }

        // Append the class name, method name and the arguments to the code
        code.append("invokestatic(");
        code.append(node.get("type")); // Assuming the class name is stored in the node
        code.append(", \"");
        code.append(methodName);
        code.append("\"");
        for (int i = 0; i < arguments.size(); i++) {
            JmmNode argument = arguments.get(i);
            code.append(", ");
            switch (argument.getKind()) {
                case "IntegerLiteral":
                    code.append(argument.get("value"));
                    code.append(toOllirType(new Type("int", false)));
                    break;
                case "BooleanLiteral":
                    code.append(argument.get("value"));
                    code.append(toOllirType(new Type("bool", false)));
                    break;
                case "BinaryExpr":
                case "ArrayAccessExpr":
                    code.append(exprVisitor.visit(argument).getCode());
                    break;
                default:
                    code.append(argument.get("name"));
                    code.append(toOllirType(argument, table));
                    break;
            }
            if (i < arguments.size() - 1) {
                code.append(", ");
            }
        }
        code.append(").V;\n");

        return code.toString();
    }

    private String visitImportDecl(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        code.append("import ");
        for (var child : node.getChildren()) {
            code.append(child.get("name"));
        }
        code.append(";\n");

        return code.toString();
    }

    private String visitExprStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        for (var child : node.getChildren()) {
            code.append(visit(child));
        }

        return code.toString();
    }

    private String visitIfElseStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        // Visit the condition expression
        JmmNode condition = node.getJmmChild(0);
        OllirExprResult conditionResult = exprVisitor.visit(condition);
        code.append(conditionResult.getComputation());

        String trueLabel = OptUtils.getLabel("true");
        String falseLabel = OptUtils.getLabel("false");
        String endLabel = OptUtils.getLabel("end");

        code.append("if (");
        code.append(conditionResult.getCode());
        code.append(") goto ").append(trueLabel).append(";\n");
        code.append("goto ").append(falseLabel).append(";\n");

        code.append(trueLabel).append(":\n");
        code.append(visitIfElseBranch(node.getJmmChild(1))); // Visit the true branch

        code.append(falseLabel).append(":\n");
        code.append(visitIfElseBranch(node.getJmmChild(2))); // Visit the false branch

        code.append(endLabel).append(":\n"); // End label

        return code.toString();
    }

    private String visitIfElseBranch(JmmNode node) {
        StringBuilder code = new StringBuilder();

        if (node.getKind().equals("IfElseStmt")) {
            code.append(visit(node));
        }
        else {
            for (var child : node.getChildren()) {
                code.append(visit(child));
            }
        }

        return code.toString();
    }
    private String visitWhileStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        // Generate a label for the loop condition
        String conditionLabel = OptUtils.getLabel("loop_condition");
        code.append(conditionLabel).append(":\n");

        // Visit the condition expression
        JmmNode condition = node.getJmmChild(0);
        OllirExprResult conditionResult = exprVisitor.visit(condition);
        code.append(conditionResult.getComputation());

        // Generate code for the condition
        code.append("if (");
        code.append(conditionResult.getCode());
        code.append(") goto loop_body;\n");
        code.append("goto loop_end;\n");

        // Generate a label for the loop body
        String bodyLabel = OptUtils.getLabel("loop_body");
        code.append(bodyLabel).append(":\n");
        code.append(visit(node.getJmmChild(1))); // Visit the loop body
        code.append("goto ").append(conditionLabel).append(";\n"); // Jump back to the condition

        // Generate a label for the end of the loop
        String endLabel = OptUtils.getLabel("loop_end");
        code.append(endLabel).append(":\n");

        return code.toString();
    }

    /**
     * Default visitor. Visits every child node and return an empty string.
     *
     * @param node
     * @param unused
     * @return
     */
    private String defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return "";
    }
}
