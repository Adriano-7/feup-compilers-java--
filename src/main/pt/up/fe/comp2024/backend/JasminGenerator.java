package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Generates Jasmin code from an OllirResult.
 * <p>
 * One JasminGenerator instance per OllirResult.
 */
public class JasminGenerator {

    private static final String NL = "\n";
    private static final String TAB = "   ";

    private final OllirResult ollirResult;

    List<Report> reports;

    String code;

    Method currentMethod;

    private final FunctionClassMap<TreeNode, String> generators;

    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;

        reports = new ArrayList<>();
        code = null;
        currentMethod = null;

        this.generators = new FunctionClassMap<>();
        generators.put(ClassUnit.class, this::generateClassUnit);
        generators.put(Method.class, this::generateMethod);
        generators.put(AssignInstruction.class, this::generateAssign);
        generators.put(SingleOpInstruction.class, this::generateSingleOp);
        generators.put(LiteralElement.class, this::generateLiteral);
        generators.put(Operand.class, this::generateOperand);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(PutFieldInstruction.class, this::generatePutField);
        generators.put(GetFieldInstruction.class, this::generateGetField);
        generators.put(CallInstruction.class, this::generateCall);
    }

    private String generateCall(CallInstruction callInstruction) {
        var code = new StringBuilder();

        if (callInstruction.getInvocationType().equals(CallType.invokespecial)) {
            Operand className = (Operand) callInstruction.getOperands().get(0);
            code.append(generators.apply(className));

            var superClass = ollirResult.getOllirClass().getSuperClass()==null ? ollirResult.getOllirClass().getSuperClass() : "java/lang/Object";

            if (className.getName().equals("this"))
                code.append("invokespecial ").append(superClass).append("/<init>()V").append(NL);
            else {
                ClassType classType = (ClassType) className.getType();
                String importedClass = getClassName(classType.getName());
                code.append("invokespecial ").append(importedClass).append("/<init>()V").append(NL);
                code.append("pop").append(NL);
            }
        } else if (callInstruction.getInvocationType().equals(CallType.invokestatic)) {
            Operand className = (Operand) callInstruction.getOperands().get(0);
            LiteralElement method = (LiteralElement) callInstruction.getOperands().get(1);
            String importedClass = getClassName(className.getName());

            var instruction = new StringBuilder();
            instruction.append("invokestatic ");
            instruction.append(importedClass).append("/").append(method.getLiteral().replace("\"", ""));
            instruction.append("(");

            for (Element element : callInstruction.getArguments()) {
                code.append(generators.apply(element));
                instruction.append(getDescriptor(element.getType(), ollirResult.getOllirClass().getClassName()));
            }
            instruction.append(")");
            instruction.append(getDescriptor(callInstruction.getReturnType(), ollirResult.getOllirClass().getClassName()));
            instruction.append(NL);
            code.append(instruction);

        } else if (callInstruction.getInvocationType().equals(CallType.invokevirtual)) {
            Operand className = (Operand) callInstruction.getOperands().get(0);
            LiteralElement method = (LiteralElement) callInstruction.getOperands().get(1);
            var instruction = new StringBuilder();
            ClassType classType = (ClassType) className.getType();
            String importedClass = getClassName(classType.getName());

            code.append(generators.apply(className));

            instruction.append("invokevirtual ");
            instruction.append(importedClass).append("/");
            instruction.append(method.getLiteral().replace("\"", ""));
            instruction.append("(");

            for (Element element : callInstruction.getArguments()) {
                code.append(generators.apply(element));
                instruction.append(getDescriptor(element.getType(), ollirResult.getOllirClass().getClassName()));
            }

            instruction.append(")");
            instruction.append(getDescriptor(callInstruction.getReturnType(), ollirResult.getOllirClass().getClassName())).append(NL);
            code.append(instruction);

            if (callInstruction.getReturnType().getTypeOfElement().equals(ElementType.VOID))
                code.append("pop").append(NL);

        } else if (callInstruction.getInvocationType().equals(CallType.NEW)) {
            Operand className = (Operand) callInstruction.getOperands().get(0);
            String importedClass = getClassName(className.getName());
            code.append("new ").append(importedClass).append(NL);
            code.append("dup").append(NL);
        }

        return code.toString();
    }

    private String generatePutField(PutFieldInstruction putFieldInstruction) {
        var code = new StringBuilder();
        Operand operand1 = (Operand) putFieldInstruction.getOperands().get(0);
        Operand operand2 = (Operand) putFieldInstruction.getOperands().get(1);
        Element element3 = putFieldInstruction.getOperands().get(2);

        code.append(generators.apply(operand1));
        code.append(generators.apply(element3));
        ClassType classType = (ClassType) operand1.getType();
        String importedClass = getClassName(classType.getName());

        var ollirClassName = ollirResult.getOllirClass().getClassName();
        code.append("putfield ").append(importedClass).append("/").append(operand2.getName()).append(" ");
        code.append(getDescriptor(element3.getType(), ollirClassName)).append(NL);

        return code.toString();
    }

    private String generateGetField(GetFieldInstruction getFieldInstruction) {
        var code = new StringBuilder();
        Operand operand1 = (Operand) getFieldInstruction.getOperands().get(0);
        Operand operand2 = (Operand) getFieldInstruction.getOperands().get(1);

        ClassType classType = (ClassType) operand1.getType();
        var ollirClassName = ollirResult.getOllirClass().getClassName();
        String newClassName = getClassName(classType.getName());

        code.append(generators.apply(operand1));
        code.append("getfield ").append(newClassName).append("/").append(operand2.getName()).append(" ");
        code.append(getDescriptor(operand2.getType(), ollirClassName)).append(NL);

        return code.toString();
    }


    public List<Report> getReports() {
        return reports;
    }

    public String build() {

        // This way, build is idempotent
        if (code == null) {
            code = generators.apply(ollirResult.getOllirClass());
        }

        return code;
    }

    private StringBuilder getDescriptor(Type type, String className) {
        var ret = new StringBuilder();
        if (type.getTypeOfElement().equals(ElementType.INT32))
            ret.append("I");
        else if (type.getTypeOfElement().equals(ElementType.BOOLEAN))
            ret.append("Z");
        else if (type.getTypeOfElement().equals(ElementType.VOID))
            ret.append("V");
        else if (type.getTypeOfElement().equals(ElementType.STRING))
            ret.append("Ljava/lang/String;");
        else if (type.getTypeOfElement().equals(ElementType.CLASS))
            ret.append("Ljava/lang/Class;");
        else if (type.getTypeOfElement().equals(ElementType.ARRAYREF)) {
            ArrayType array = (ArrayType) type;
            ret.append("[").append(getDescriptor(array.getElementType(), className));
        }
        else if (type.getTypeOfElement().equals(ElementType.THIS))
            ret.append("L").append(className).append(";");
        else if (type.getTypeOfElement().equals(ElementType.OBJECTREF)) {
            ClassType classType = (ClassType) type;
            String importedClass = getClassName(classType.getName());
            ret.append("L").append(importedClass).append(";");
        }
        return ret;
    }

    private String getPrefix(Type type) {
        if (type.getTypeOfElement().equals(ElementType.INT32) || type.getTypeOfElement().equals(ElementType.BOOLEAN)) {
            return "i";
        }
        else {
            return "a";
        }

    }

    private String getClassName(String className) {
        AtomicReference<String> newClassName = new AtomicReference<>("");
        ollirResult.getOllirClass().getImports().forEach(i -> {
            if (i.contains(className))
                newClassName.set(i.replace(".", "/"));
        });
        if (newClassName.get().isEmpty()){
            newClassName.set(className);
        }
        return newClassName.get();
    }


    private String generateClassUnit(ClassUnit classUnit) {

        var code = new StringBuilder();

        // generate class name
        ClassUnit ollirClass = ollirResult.getOllirClass();
        var className = ollirResult.getOllirClass().getClassName();
        code.append(".class ").append(className).append(NL).append(NL);

        if (ollirClass.getSuperClass() == null){
            code.append(".super java/lang/Object").append(NL);
        } else {
            code.append(".super ").append(getClassName(ollirClass.getSuperClass())).append(NL);
        }

        // generate fields
        code.append(NL);
        for (Field field : ollirClass.getFields()) {
            String modifier = field.getFieldAccessModifier().name().equals("DEFAULT")?"":field.getFieldAccessModifier().name().toLowerCase() + " ";
            code.append(".field ").append(modifier).append(field.getFieldName());
            code.append(" ").append(getDescriptor(field.getFieldType(), ollirClass.getClassName())).append(NL);
        }
        code.append(NL);

        // generate a single constructor method
        var defaultConstructor = """
                ;default constructor
                .method public <init>()V
                    aload_0
                    invokespecial java/lang/Object/<init>()V
                    return
                .end method
                """;

        if (ollirClass.getSuperClass() == null)
            code.append(defaultConstructor);

        // generate code for all other methods
        for (var method : ollirResult.getOllirClass().getMethods()) {

            // Ignore constructor, since there is always one constructor
            // that receives no arguments, and has been already added
            // previously
            if (method.isConstructMethod()) {
                continue;
            }

            code.append(generators.apply(method));
        }

        return code.toString();
    }


    private String generateMethod(Method method) {

        // set method
        currentMethod = method;

        var code = new StringBuilder();

        // calculate modifier
        var modifier = method.getMethodAccessModifier() != AccessModifier.DEFAULT ?
                method.getMethodAccessModifier().name().toLowerCase() + " " :
                "";

        var methodName = method.getMethodName();

        var tmp = new StringBuilder();
        if (method.isFinalMethod())
            tmp.append("final ");

        tmp.append(modifier);

        if (method.isStaticMethod())
            tmp.append("static ");

        code.append("\n.method ").append(tmp).append(methodName).append("(");

        for (Element param : method.getParams()) {
            tmp = getDescriptor(param.getType(), ollirResult.getOllirClass().getClassName());
            code.append(tmp);
        }

        code.append(")").append(getDescriptor(method.getReturnType(), ollirResult.getOllirClass().getClassName())).append(NL);

        // Add limits
        code.append(TAB).append(".limit stack 99").append(NL);
        code.append(TAB).append(".limit locals 99").append(NL);

        for (var inst : method.getInstructions()) {
            var instCode = StringLines.getLines(generators.apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            code.append(instCode);
        }

        code.append(".end method\n");

        // unset method
        currentMethod = null;

        return code.toString();
    }

    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();

        // generate code for loading what's on the right
        code.append(generators.apply(assign.getRhs()));

        // store value in the stack in destination
        var lhs = assign.getDest();

        if (!(lhs instanceof Operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        var operand = (Operand) lhs;

        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        if (reg < 4){
            code.append(getPrefix(operand.getType())).append("store_").append(reg).append(NL);
        }else{
            code.append(getPrefix(operand.getType())).append("store ").append(reg).append(NL);
        }

        return code.toString();
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        if (literal.getType().getTypeOfElement().equals(ElementType.INT32) || literal.getType().getTypeOfElement().equals(ElementType.BOOLEAN)) {
            int val = Integer.parseInt(literal.getLiteral());

            if (val > -2 && val < 6){
                return "iconst_" + literal.getLiteral() + NL;

            }
            else if (val > -129 && val < 128)
                return "bipush " + literal.getLiteral() + NL;
            else if (val > -32769 && val < 32768)
                return "sipush " + literal.getLiteral() + NL;
            else
                return "ldc " + literal.getLiteral() + NL;
        }
        else
            return "ldc \"" + literal.getLiteral() + "\"\n";
    }

    private String generateOperand(Operand operand) {
        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        if (reg < 4)
            return getPrefix(operand.getType()) + "load_" + reg + "\n";
        else
            return getPrefix(operand.getType()) + "load " + reg + "\n";
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();

        // load values on the left and on the right
        code.append(generators.apply(binaryOp.getLeftOperand()));
        code.append(generators.apply(binaryOp.getRightOperand()));

        // apply operation
        var opTypeInfo = binaryOp.getOperation().getTypeInfo();
        var opPrefix = getPrefix(opTypeInfo);
        var op = switch (binaryOp.getOperation().getOpType()) {
            case ADD -> opPrefix + "add";
            case MUL -> opPrefix + "mul";
            case SUB -> opPrefix + "sub";
            case DIV -> opPrefix + "div";
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        };

        code.append(op).append(NL);

        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        if (returnInst.hasReturnValue()){
            code.append(generators.apply(returnInst.getOperand()));
        } else {
            code.append("return").append(NL);
            return code.toString();
        }

        if (returnInst.getReturnType().getTypeOfElement().equals(ElementType.INT32) || returnInst.getReturnType().getTypeOfElement().equals(ElementType.BOOLEAN))
            code.append("ireturn").append(NL);
        else
            code.append("areturn").append(NL);

        return code.toString();
    }

}
