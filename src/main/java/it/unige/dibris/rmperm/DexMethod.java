package it.unige.dibris.rmperm;

import org.jf.dexlib2.base.reference.BaseMethodReference;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class DexMethod extends BaseMethodReference {
    private String definingClass;
    private String name;
    private List<? extends CharSequence> parameterTypes;
    private String returnType;

    public DexMethod(String definingClass, String name, List<? extends CharSequence> parameterTypes, String returnType) {
        this.definingClass = definingClass;
        this.name = name;
        this.parameterTypes = parameterTypes;
        this.returnType = returnType;
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

    public static List<String> parseAndConvertIntoDalvikTypes(String csvParams) {
        List<String> list = new ArrayList<String>();
        String[] params = csvParams.split(",");
        for (String s : params) {
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

    @Override
    public String toString() {
        return  returnType + ' ' +definingClass + '.' + name + '(' + parameterTypes + ")";
    }

}
