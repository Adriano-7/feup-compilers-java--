package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.Instruction;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.List;
import java.util.Optional;

import static pt.up.fe.comp2024.ast.Kind.TYPE;

public class OptUtils {
    private static int tempNumber = -1;
    private static int labelCounter = 0;


    public static String getTemp() {

        return getTemp("tmp");
    }

    public static String getTemp(String prefix) {

        return prefix + getNextTempNum();
    }

    public static int getNextTempNum() {

        tempNumber += 1;
        return tempNumber;
    }

    public static String toOllirType(JmmNode typeNode, SymbolTable table){
        return toOllirType(typeNode, table, true);
    }
    public static String toOllirType(JmmNode typeNode, SymbolTable table, boolean showArray) {
        //TYPE.checkOrThrow(typeNode);
        Type type = TypeUtils.getExprType(typeNode, table);

        String result = ".";

        if (type.isArray() && showArray) {
            result += "array.";
        }

        result += switch (type.getName()) {
            case "int" -> "i32";
            case "boolean" -> "bool";
            case "String" -> "String";
            default -> type.getName();
        };

        return result;
    }

    public static String toOllirType(Type type) {
        String result = "";
        if(type.isArray()){
            result += "array.";
        }
        result += "." + switch (type.getName()) {
            case "int" -> "i32";
            case "boolean" -> "bool";
            case "String" -> "String";
            default -> type.getName();
        };

        return toOllirType(type.getName());
    }

    public static String toOllirType(String typeName) {
        String type = "." + switch (typeName) {
            case "int" -> "i32";
            case "boolean" -> "bool";
            case "String" -> "String";
            default -> typeName;
        };
        return type;
    }

    public static String getLabel(String prefix) {
        String label = prefix + "_" + labelCounter;
        labelCounter++;
        return label;
    }

    public static String getParameterNumber(JmmNode node, SymbolTable table){
        JmmNode method = node.getParent();
        while ( !method.getKind().equals("PublicStaticVoidMethodDecl") && !method.getKind().equals("PublicMethodDecl")) {
            method = method.getParent();
        }

        List<Symbol> parameters = table.getParameters(method.get("name"));
        for (int i = 0; i < parameters.size(); i++) {
            if(parameters.get(i).getName().equals(node.get("name"))){
                return "$" + (i+1);
            }
        }

        return "";
    }
}
