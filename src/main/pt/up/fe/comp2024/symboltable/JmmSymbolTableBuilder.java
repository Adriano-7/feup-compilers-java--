package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;

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
    private final Map<String, List<VarargSymbol>> params;
    private final Map<String, Type> returnTypes;
    private final Map<String, List<Symbol>> locals;
    private final List<VarargSymbol> fields;

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
        this.fields.add(new VarargSymbol(new Type(typeName, isArray), name, false, false));

        return arg;
    }

    private String dealWithMethodDecl(JmmNode node, String arg) {
        String name = node.get("name");
        this.methods.add(name);
    
        if (name.equals("main")) {
            this.returnTypes.put(name, new Type("void", false));
            this.params.put(name, Arrays.asList(new VarargSymbol(new Type("String", true), "args", false, false)));
            List<Symbol> locals = new ArrayList<>();

            List<JmmNode> varDeclNodes = node.getChildren(Kind.VAR_DECL);
            for (JmmNode child : varDeclNodes) {
                String localName = child.get("name");
                JmmNode localTypeNode = child.getChildren().get(0);
                String localType = localTypeNode.get("name");
                boolean localIsArray = Boolean.parseBoolean(localTypeNode.get("isArray"));
                locals.add(new Symbol(new Type(localType, localIsArray), localName));
            }

            this.locals.put(name, locals);
        } else {
            JmmNode returnTypeNode = node.getChildren().get(0);
            String returnType = returnTypeNode.get("name");
            boolean isArray = Boolean.parseBoolean(returnTypeNode.get("isArray"));
            this.returnTypes.put(name, new Type(returnType, isArray));
    
            List<VarargSymbol> parameters = new ArrayList<>();
            List<Symbol> locals = new ArrayList<>();
            
            List<JmmNode> paramNodes = node.getChildren(Kind.PARAM);
            for (JmmNode child : paramNodes) {
                String paramName = child.get("name");
                JmmNode paramTypeNode = child.getChildren().get(0);
                String paramType = paramTypeNode.get("name");
                boolean paramIsArray = Boolean.parseBoolean(paramTypeNode.get("isArray"));
                boolean isVararg = Boolean.parseBoolean(paramTypeNode.get("isVarArg"));
                parameters.add(new VarargSymbol(new Type(paramType, paramIsArray), paramName, isVararg, false));
            }
            
            List<JmmNode> varDeclNodes = node.getChildren(Kind.VAR_DECL);
            for (JmmNode child : varDeclNodes) {
                String localName = child.get("name");
                JmmNode localTypeNode = child.getChildren().get(0);
                String localType = localTypeNode.get("name");
                boolean localIsArray = Boolean.parseBoolean(localTypeNode.get("isArray"));
                locals.add(new Symbol(new Type(localType, localIsArray), localName));
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
