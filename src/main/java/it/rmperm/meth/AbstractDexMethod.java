package it.rmperm.meth;

import org.jf.dexlib2.base.reference.BaseMethodReference;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractDexMethod extends BaseMethodReference implements IDexMethod {
    private String definingClass;
    private String name;
    private List<String> parameterTypes;
    private String returnType;

    public AbstractDexMethod(String definingClass, String name, List<String> parameterTypes, String returnType) {
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractDexMethod)) return false;
        if (!super.equals(o)) return false;

        AbstractDexMethod that = (AbstractDexMethod) o;

        if (!getDefiningClass().equals(that.getDefiningClass())) return false;
        if (!getName().equals(that.getName())) return false;
        if (!getParameterTypes().equals(that.getParameterTypes())) return false;
        return getReturnType().equals(that.getReturnType());

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + getDefiningClass().hashCode();
        result = 31 * result + getName().hashCode();
        result = 31 * result + getParameterTypes().hashCode();
        result = 31 * result + getReturnType().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "DexMethod{" +
                "defC='" + definingClass + '\'' +
                ", name='" + name + '\'' +
                ", parTs=" + parameterTypes +
                ", retT='" + returnType + '\'' +
                '}';
    }

}
