package it.rmperm;

import org.jf.dexlib2.base.reference.BaseMethodReference;
import javax.annotation.Nonnull;
import java.util.List;


public class DexMethod extends BaseMethodReference {
    private String permission;
    private String definingClass;
    private String name;
    private List<String> parameterTypes;
    private String returnType;

    public DexMethod(String permission, String definingClass, String name, List<String> parameterTypes, String returnType) {
        this.permission = permission;
        this.definingClass = definingClass;
        this.name = name;
        this.parameterTypes = parameterTypes;
        this.returnType = returnType;
    }

    public DexMethod(String definingClass, String name, List<String> parameterTypes, String returnType) {
        this.permission = "";
        this.definingClass = definingClass;
        this.name = name;
        this.parameterTypes = parameterTypes;
        this.returnType = returnType;
    }

    public String getPermission() {
        return permission;
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
    /**
     * permission field not used for equals-hashcode
     */
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        DexMethod dexMethod = (DexMethod) o;

        //if (!definingClass.equals(dexMethod.definingClass)) return false;
        if (!name.equals(dexMethod.name)) return false;
        if (!parameterTypes.equals(dexMethod.parameterTypes)) return false;
        return returnType.equals(dexMethod.returnType);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        //result = 31 * result + definingClass.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + parameterTypes.hashCode();
        result = 31 * result + returnType.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "DexMethod{" +
                "perm='" + permission + '\'' +
                ", defC='" + definingClass + '\'' +
                ", name='" + name + '\'' +
                ", parTs=" + parameterTypes +
                ", retT='" + returnType + '\'' +
                '}';
    }
}
