package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

public class VarargSymbol extends Symbol {
    private final boolean isVararg;
    private final boolean isPublic;

    public VarargSymbol(Type type, String name, boolean isVararg, boolean isPublic) {
        super(type, name);
        this.isVararg = isVararg;
        this.isPublic = isPublic;
    }

    public boolean isVararg() {
        return isVararg;
    }
}
