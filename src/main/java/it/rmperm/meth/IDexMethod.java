package it.rmperm.meth;

import java.util.List;

public interface IDexMethod {
    public String getDefiningClass();
    public String getName();
    public List<? extends CharSequence> getParameterTypes();
    public String getReturnType();
}
