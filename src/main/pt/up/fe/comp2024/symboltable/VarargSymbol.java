package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

public class VarargSymbol extends Symbol {
    private final boolean isVararg;

    public VarargSymbol(Type type, String name, boolean isVararg) {
        super(type, name);
        this.isVararg = isVararg;
    }

    public boolean isVararg() {
        return isVararg;
    }
}
