package it.rmperm;


import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.Method;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CustomMethodManager {
    private static final String prefixMethodConvention = "hack"; //TODO: change into "custom"
    private DexFile dexFile;
    private String customClass;

    /**
     *
     * @param definingFile dex or apk that contains the classes with custom methods
     * @param customClass name of class with custom methods
     */
    public CustomMethodManager(Path definingFile, String customClass) {
        File file = new File(definingFile.toUri());
        try {
            dexFile = DexFileFactory.loadDexFile(file, 19, false);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        this.customClass = customClass;
    }

    public List<ClassDef> getCustomClasses() {
        List<ClassDef> classList = new ArrayList<>();
        for (ClassDef classDef : dexFile.getClasses()) {
            if (classDef.toString().contains(customClass)) {
                //System.out.println(classDef);
                classList.add(classDef);
                for (Method method : classDef.getMethods()) {
                    if (method.getName().toString().contains("hack")) {
                        List<String> parms = new ArrayList<>();
                        for (CharSequence cs :  method.getParameterTypes()) {
                            parms.add(cs.toString());
                        }
                        /*
                        DexMethod tmpDM = new DexMethod(method.getDefiningClass(), method.getName(),parms, method.getReturnType());
                        System.out.println(tmpDM);
                        */
                    }

                }
            }
        }
        return classList;
    }
}
