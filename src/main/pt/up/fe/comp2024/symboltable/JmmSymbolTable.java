package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JmmSymbolTable implements SymbolTable {
    private final List<String> imports;
    private final String className;
    private final String superClass;
    private final List<VarargSymbol> fields;
    private final List<String> methods;
    private final Map<String, Type> returnTypes;
    private final Map<String, List<VarargSymbol>> params;
    private final Map<String, List<Symbol>> locals;

    public JmmSymbolTable(
                          List<String> imports,
                          String className,
                          String superClass,
                          List<VarargSymbol> fields,
                          List<String> methods,
                          Map<String, Type> returnTypes,
                          Map<String, List<VarargSymbol>> params,
                          Map<String, List<Symbol>> locals
                          ) {
        this.imports = imports;
        this.className = className;
        this.superClass = superClass;
        this.methods = methods;
        this.returnTypes = returnTypes;
        this.params = params;
        this.locals = locals;
        this.fields = fields;
    }

    @Override
    public List<String> getImports() {
        return imports;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getSuper() {
        return superClass;
    }

    @Override
    public List<Symbol> getFields() {
        return Collections.unmodifiableList(fields);
    }

    @Override
    public List<String> getMethods() {
        return Collections.unmodifiableList(methods);
    }

    @Override
    public Type getReturnType(String methodSignature) {
        return returnTypes.getOrDefault(methodSignature, null);
    }

    @Override
    public List<Symbol> getParameters(String methodSignature) {
        return Collections.unmodifiableList(params.getOrDefault(methodSignature, Collections.emptyList()));
    }

    @Override
    public List<Symbol> getLocalVariables(String methodSignature) {
        return Collections.unmodifiableList(locals.getOrDefault(methodSignature, Collections.emptyList()));
    }
}
