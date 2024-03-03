package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.*;

public class JmmSymbolTableBuilder extends AJmmVisitor<String, String> {
    private String className;
    private String superClass;
    private List<String> imports;
    private List<String> methods;
    private Map<String, List<Symbol>> params;
    private Map<String, Type> returnTypes;
    private Map<String, List<Symbol>> locals;
    private List<Symbol> fields;

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
                builder.methods,
                builder.returnTypes,
                builder.params,
                builder.locals,
                builder.fields
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
        if(node.hasAttribute("extendedClass")){
            this.superClass = node.get("extendedClass");
        }
        for (JmmNode child : node.getChildren()) {
            visit(child);
        }
        return arg;
    }

    private String dealWithVarDecl(JmmNode node, String arg) {
        String name = node.get("name");
        JmmNode typeNode = node.getChildren().get(0);
        String type = typeNode.get("typeName");
        boolean isArray = typeNode.getKind().equals("Array");
        this.fields.add(new Symbol(new Type(type, isArray), name));
        return arg;
    }
    private String dealWithMethodDecl(JmmNode node, String arg) {
        String name = node.hasAttribute("name") ? node.get("name") : "main";
        this.methods.add(name);

        if(name.equals("main")){
            this.returnTypes.put(name, new Type("static void", false));
            //String[] args
            this.params.put(name, Arrays.asList(new Symbol(new Type("String[]", true), "args")));
        }
        else{
            //First child is the return type
            JmmNode returnTypeNode = node.getChildren().get(0);
            String returnType = returnTypeNode.get("typeName");
            boolean isArray = returnTypeNode.getKind().equals("Array");
            this.returnTypes.put(name, new Type(returnType, isArray));

            //Then traverse the children to get the parameters and the local variables
            List<Symbol> parameters = new ArrayList<>();
            List<Symbol> locals = new ArrayList<>();
            for (int i = 1; i < node.getNumChildren(); i++) {
                JmmNode child = node.getChildren().get(i);
                if (child.getKind().equals("Param")) {
                    String paramName = child.get("name");
                    JmmNode paramTypeNode = child.getChildren().get(0);
                    String paramType = paramTypeNode.get("typeName");
                    boolean paramIsArray = paramTypeNode.getKind().equals("Array");
                    parameters.add(new Symbol(new Type(paramType, paramIsArray), paramName));
                }
                else if (child.getKind().equals("VarDecl")) {
                    String localName = child.get("name");
                    JmmNode localTypeNode = child.getChildren().get(0);
                    String localType = localTypeNode.get("typeName");
                    boolean localIsArray = localTypeNode.getKind().equals("Array");
                    locals.add(new Symbol(new Type(localType, localIsArray), localName));
                }
            }
            this.params.put(name, parameters);
            this.locals.put(name, locals);
        }

        return arg;
    }

    private String defaultWithType(JmmNode node, String arg) {
        return node.get("typeName");
    }

    private String dealWithParam(JmmNode node, String arg) {
        return node.get("name");
    }
}