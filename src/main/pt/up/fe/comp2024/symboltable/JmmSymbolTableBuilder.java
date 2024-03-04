package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JmmSymbolTableBuilder extends AJmmVisitor<String, String> {
    private String className;
    private String superClass;
    private final List<String> imports;
    private final List<String> methods;
    private final Map<String, List<Symbol>> params;
    private final Map<String, Type> returnTypes;
    private final Map<String, List<Symbol>> locals;
    private final List<Symbol> fields;

    public JmmSymbolTableBuilder(){
        this.imports = new ArrayList<>();
        this.methods = new ArrayList<>();
        this.params = new HashMap<>();
        this.returnTypes = new HashMap<>();
        this.locals = new HashMap<>();
        this.fields = new ArrayList<>();
    }

    public static JmmSymbolTable build(JmmNode root) {
        var builder = new JmmSymbolTableBuilder();
        builder.visit(root);
        return new JmmSymbolTable(
                builder.imports,
                builder.className,
                builder.superClass,
                builder.fields,
                builder.methods,
                builder.returnTypes,
                builder.params,
                builder.locals
        );
    }

    @Override
    protected void buildVisitor() {
        setDefaultVisit(this::defaultVisit);
        addVisit("ImportStmt", this::dealWithImport);
        addVisit("ClassStmt", this::dealWithClass);
        addVisit("MethodDecl", this::dealWithMethodDecl);
        addVisit("VarDecl", this::dealWithVarDecl);
        addVisit("Param", this::dealWithParam);
        addVisit("Type", this::defaultWithType);
    }

    private String defaultVisit(JmmNode node, String arg) {
        for (JmmNode child : node.getChildren()) {
            visit(child);
        }
        return arg;
    }

    private String dealWithImport(JmmNode node, String arg) {
        String imp = "";
        for (JmmNode child : node.getChildren()) {
            if (!imp.isEmpty()) {
                imp += ".";
            }
            imp += child.get("name");
        }
        this.imports.add(imp);
        return arg;
    }

    private String dealWithClass(JmmNode node, String arg) {
        this.className = node.get("name");
        this.superClass = node.getOptional("extendedClass").orElse(null);
        for (JmmNode child : node.getChildren()) {
            visit(child);
        }
        return arg;
    }

    private String dealWithVarDecl(JmmNode node, String arg) {
        String name = node.get("name");
        JmmNode typeNode = node.getChildren().get(0);
        String typeName = typeNode.get("name");
        boolean isArray = Boolean.parseBoolean(typeNode.get("isArray"));
        this.fields.add(new Symbol(new Type(typeName, isArray), name));
        return arg;
    }

    private String dealWithMethodDecl(JmmNode node, String arg) {
        String name = node.get("name");
        this.methods.add(name);

        if (name.equals("main")) {
            this.returnTypes.put(name, new Type("void", false));
            //String[] args
            this.params.put(name, Arrays.asList(new Symbol(new Type("String[]", true), "args")));
        } else {
            //First child is the return type
            JmmNode returnTypeNode = node.getChildren().get(0);
            String returnType = returnTypeNode.get("name");
            boolean isArray = Boolean.parseBoolean(returnTypeNode.get("isArray"));
            this.returnTypes.put(name, new Type(returnType, isArray));

            //Then traverse the children to get the parameters and the local variables
            List<Symbol> parameters = new ArrayList<>();
            List<Symbol> locals = new ArrayList<>();
            for (int i = 1; i < node.getNumChildren(); i++) {
                JmmNode child = node.getChildren().get(i);
                if (child.getKind().equals("Param")) {
                    String paramName = child.get("name");
                    JmmNode paramTypeNode = child.getChildren().get(0);
                    String paramType = paramTypeNode.get("name");
                    boolean paramIsArray = Boolean.parseBoolean(paramTypeNode.get("isArray"));
                    parameters.add(new Symbol(new Type(paramType, paramIsArray), paramName));

                } else if (child.getKind().equals("VarDecl")) {
                    String localName = child.get("name");
                    JmmNode localTypeNode = child.getChildren().get(0);
                    String localType = localTypeNode.get("name");
                    boolean localIsArray = Boolean.parseBoolean(localTypeNode.get("isArray"));
                    locals.add(new Symbol(new Type(localType, localIsArray), localName));
                }
            }
            this.params.put(name, parameters);
            this.locals.put(name, locals);
        }

        return arg;
    }
    private String defaultWithType(JmmNode node, String arg) {
        return node.get("name");
    }

    private String dealWithParam(JmmNode node, String arg) {
        return node.get("name");
    }
}
