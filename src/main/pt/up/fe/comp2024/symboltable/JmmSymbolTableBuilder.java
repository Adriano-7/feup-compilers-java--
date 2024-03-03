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
        addVisit("varDecl", this::dealWithVarDecl);
        addVisit("methodDecl", this::dealWithMethodDecl);
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
        String type = node.get("type");
        String name = node.get("name");
        this.fields.add(new Symbol(new Type(type, false), name));
        return arg;
    }

    private String dealWithMethodDecl(JmmNode node, String arg) {
        if(node.hasAttribute("name")) {
            methods.add(node.get("name"));
            returnTypes.put(node.get("name"), new Type(node.get("type"), false));

            List<Symbol> methodParams = new ArrayList<>();
            for (JmmNode child : node.getChildren()) {
                if (child.getKind().equals("param")) {
                    String paramName = child.get("name");
                    String paramType = child.get("type");
                    methodParams.add(new Symbol(new Type(paramType, false), paramName));
                }
            }
            params.put(node.get("name"), methodParams);
        }
        else{
            methods.add("main");
            List<Symbol> mainParams = new ArrayList<>();
            mainParams.add(new Symbol(new Type("String", true), "args"));
            params.put("main", mainParams);
            returnTypes.put("main", new Type("void", false));
        }

        return arg;
    }
}