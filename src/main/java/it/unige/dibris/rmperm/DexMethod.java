package it.unige.dibris.rmperm;

import org.jf.dexlib2.base.reference.BaseMethodReference;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

class DexMethod extends BaseMethodReference {
    private final String definingClass;
    private final String name;
    private final List<? extends CharSequence> parameterTypes;
    private final String returnType;

    public DexMethod(String definingClass,
                     String name,
                     List<? extends CharSequence> parameterTypes,
                     String returnType
    ) {
        if (definingClass==null || name==null || parameterTypes==null || returnType==null)
            throw new NullPointerException();
        this.definingClass = definingClass;
        this.name = name;
        this.parameterTypes = parameterTypes;
        this.returnType = returnType;
    }

    public static List<String> parseAndConvertIntoDalvikTypes(String csvParams) {
        List<String> list = new ArrayList<>();
        for (String s : csvParams.split(",")) {
            if (!s.equals(""))
                list.add(fromJavaTypeToDalvikType(s));
        }
        return list;
    }

    public static String fromJavaTypeToDalvikType(String jType) {
        switch (jType) {
            case "void":
                return "V";
            case "boolean":
                return "Z";
            case "byte":
                return "B";
            case "short":
                return "S";
            case "char":
                return "C";
            case "int":
                return "I";
            case "long":
                return "J";
            case "float":
                return "F";
            case "double":
                return "D";
            default:
                return "L" + jType.replace(".", "/") + ";";
        }
    }

    @Nonnull
    @Override
    public String getDefiningClass() {
        return definingClass;
    }

    @Nonnull
    @Override
    public String getName() {
        return name;
    }

    @Nonnull
    @Override
    public List<? extends CharSequence> getParameterTypes() {
        return parameterTypes;
    }

    @Nonnull
    @Override
    public String getReturnType() {
        return returnType;
    }

    @Override
    public String toString() {
        return returnType + ' ' + definingClass + '.' + name + '(' + parameterTypes + ")";
    }

}
