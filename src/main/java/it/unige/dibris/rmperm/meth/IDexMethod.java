package it.unige.dibris.rmperm.meth;

import java.util.List;

public interface IDexMethod {
    String getDefiningClass();
    String getName();
    List<? extends CharSequence> getParameterTypes();
    String getReturnType();
}
