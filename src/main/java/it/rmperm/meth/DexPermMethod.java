package it.rmperm.meth;

import javax.annotation.Nonnull;
import java.util.List;


public class DexPermMethod extends DexMethod {
    private String permission;

    /*
    public DexPermMethod(String definingClass, String name, List<String> parameterTypes, String returnType) {
        super(definingClass, name, parameterTypes, returnType);
        this.permission = "";
    }
    */

    public DexPermMethod(String definingClass, String name, List<String> parameterTypes, String returnType, String permission) {
        super(definingClass, name, parameterTypes, returnType);
        this.permission = permission;
    }

    @Nonnull
    public void setPermission(String permission) {
        this.permission = permission;
    }

    @Nonnull
    public String getPermission() {
        return permission;
    }

    @Override
    public boolean equals(Object o) {
        boolean b = super.equals(o);

        if (b && o instanceof DexPermMethod) {
            DexPermMethod that = (DexPermMethod) o;
            return getPermission().equals(that.getPermission());
        }

        return b && o instanceof DexMethod;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + getPermission().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "DexPermMethod{" + '\'' +
                "perm='" + getPermission() + '\'' +
                ", defC='" + getDefiningClass() + '\'' +
                ", name='" + getName() + '\'' +
                ", parmsT=" + getParameterTypes() +
                ", retT='" + getReturnType() + '\'' +
                '}';
    }

}
