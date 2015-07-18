package it.rmperm.loader;

import it.rmperm.meth.AbstractDexMethod;
import it.rmperm.meth.DexPermMethod;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.dexbacked.DexBackedAnnotationElement;
import org.jf.dexlib2.dexbacked.DexBackedMethod;
import org.jf.dexlib2.dexbacked.value.DexBackedStringEncodedValue;
import org.jf.dexlib2.iface.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

public class CustomMethodsLoader {
    public static final String prefixMethodConvention = "custom_";
    private DexFile dexFile;
    private String customClass;
    private Hashtable<String, List<DexPermMethod>> permissionToCustomMethods = new Hashtable<>();

    /**
     * @param definingFile dex or apk that contains the classes with custom methods
     * @param customClass name of class with custom methods
     */
    public CustomMethodsLoader(Path definingFile, String customClass) {
        File file = new File(definingFile.toUri());
        try {
            dexFile = DexFileFactory.loadDexFile(file, 19, false);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        this.customClass = customClass;
    }


    public Hashtable<String, List<DexPermMethod>> getPermissionToCustomMethods() {
        return permissionToCustomMethods;
    }


    public List<ClassDef> getCustomClasses() {
        List<ClassDef> classList = new ArrayList<>();
        for (ClassDef classDef : dexFile.getClasses()) {
            if (classDef.toString().contains("HackClass")) { //TODO set convention
                classList.add(classDef);
                for (Method method : classDef.getMethods()) {
                    if (method.getName().toString().contains(prefixMethodConvention)) {
                        String perm, defClass;
                        List<String> parms;
                        perm = defClass = "";
                        if (((DexBackedMethod) method).getAccessFlags() == 9) { // public static
                            for (Annotation a : method.getAnnotations()) {
                                Set<? extends AnnotationElement> annotationElements = a.getElements();
                                if (annotationElements.size() == 2) {
                                    for (AnnotationElement ae : annotationElements) {
                                        String field = ae.getName();
                                        String value = ((DexBackedStringEncodedValue) ((DexBackedAnnotationElement) ae).value).getValue();
                                        // System.out.println(field + "->" + value);
                                        if (field.equals("perm")) {
                                            perm = value;
                                        } else if (field.equals("defClass")) {
                                            defClass = AbstractDexMethod.fromJavaTypeToDalvikType(value);
                                        }
                                    }
                                    if (!permissionToCustomMethods.containsKey(perm)) {
                                        permissionToCustomMethods.put(perm, new ArrayList<>());
                                    }
                                    parms = new ArrayList<>();
                                    for (CharSequence cs : method.getParameterTypes()) {
                                        parms.add(cs.toString());
                                    }
                                    if (!parms.isEmpty())
                                        parms.remove(0); // original class containing the method
                                    DexPermMethod tmpDpm = new DexPermMethod(
                                            defClass,
                                            method.getName().replace(prefixMethodConvention, ""), // remove the custom prefix
                                            parms,
                                            method.getReturnType(),
                                            perm);
                                    permissionToCustomMethods.get(perm).add(tmpDpm);
                                    //System.out.println(tmpDpm);
                                }
                            }
                        }
                        else {
                            System.err.println("Wrong access flags in your method '" + method.getName() + "' it MUST BE public and static!");
                        }
                    }

                }
            }
        }
        return classList;
    }
}
