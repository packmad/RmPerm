package it.unige.dibris.rmperm;

import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.dexbacked.value.DexBackedStringEncodedValue;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.reference.MethodReference;

import java.io.IOException;
import java.util.*;

class CustomMethodsLoader {
    private static final String CUSTOM_METHOD_CLASS_ANNOTATION = "Lit/unige/dibris/rmperm/annotations/CustomMethodClass;";
    private static final String METHOD_PERMISSION_ANNOTATION = "Lit/unige/dibris/rmperm/annotations/MethodPermission;";
    private static final String METHOD_ANNOTATION_PERMISSION_ELEMENT = "permission";
    private static final String METHOD_ANNOTATION_DEFINING_CLASS_ELEMENT = "defClass";

    private final IOutput out;

    public CustomMethodsLoader(IOutput out) {
        this.out = out;
    }

    private static class MethodAnnotationPair {
        final Method method;
        final Annotation annotation;

        public MethodAnnotationPair(Method method, Annotation annotation) {
            this.annotation = annotation;
            this.method = method;
        }
    }

    private static class AnnotationElements {
        final String definingClass;
        final String permission;

        public AnnotationElements(String definingClass, String permission) {
            this.definingClass = definingClass;
            this.permission = permission;
        }
    }

    private static AnnotationElements extractElements(Annotation annotation) {
        String definingClass = null;
        String permission = null;
        for (AnnotationElement element : annotation.getElements()) {
            final String elementName = element.getName();
            switch (elementName) {
                case METHOD_ANNOTATION_PERMISSION_ELEMENT:
                    permission = ((DexBackedStringEncodedValue) element.getValue()).getValue();
                    break;
                case METHOD_ANNOTATION_DEFINING_CLASS_ELEMENT:
                    String javaType = ((DexBackedStringEncodedValue) element.getValue()).getValue();
                    definingClass = DexMethod.fromJavaTypeToDalvikType(javaType);
                    break;
                default:
                    assert false;
                    break;
            }
        }
        assert permission != null && definingClass != null;
        return new AnnotationElements(definingClass, permission);
    }

    public void load(String filename,
                     List<ClassDef> customClasses,
                     Map<MethodReference, MethodReference> redirections,
                     Set<String> permissionToRemove
    ) throws IOException {
        out.printf(IOutput.Level.VERBOSE, "Loading custom methods from %s\n", filename);
        DexFile dexFile = DexFileFactory.loadDexFile(filename, 19, false);
        Set<ClassDef> customMethodClasses = getAnnotatedClasses(dexFile);
        customClasses.addAll(customMethodClasses);
        for (MethodAnnotationPair methodAnnotationPair : getAnnotatedMethods(customMethodClasses)) {
            final Method method = methodAnnotationPair.method;
            final AnnotationElements elements = extractElements(methodAnnotationPair.annotation);
            final String permission = elements.permission;
            final String definingClass = elements.definingClass;
            final String methodName = method.getName();
            final String fullMethodName = method.getDefiningClass() + "." + methodName;
            final List<? extends CharSequence> parameterTypes = method.getParameterTypes();
            if (parameterTypes.isEmpty()) {
                PrintWarning("ignoring " + fullMethodName + " because its signature is missing the 'this' parameter");
                continue;
            }
            String firstParameterType = parameterTypes.get(0).toString();
            if (!firstParameterType.equals(definingClass)) {
                PrintWarning("ignoring " + fullMethodName + " because its 'this' parameter has a wrong type");
                continue;
            }
            final String returnType = method.getReturnType();
            final DexMethod originalMethod = new DexMethod(definingClass,
                                                           methodName,
                                                           removeThisParam(parameterTypes),
                                                           returnType);
            final DexMethod newMethod = new DexMethod(method.getDefiningClass(),
                                                      methodName,
                                                      parameterTypes,
                                                      returnType);
            if (!permissionToRemove.contains(permission)) {
                //out.printf(IOutput.Level.DEBUG, "[%s] Skipping redirection %s--->%s\n", permission, originalMethod, newMethod);
                continue;
            }
            out.printf(IOutput.Level.DEBUG,
                       "[%s] Adding redirection: %s--->%s\n",
                       permission,
                       originalMethod,
                       newMethod);
            redirections.put(originalMethod, newMethod);
        }
        out.printf(IOutput.Level.DEBUG, "Loaded custom methods from %s\n", filename);
    }

    private ArrayList<String> removeThisParam(List<? extends CharSequence> parameterTypes) {
        ArrayList<String> result = new ArrayList<>();
        for (CharSequence cs : parameterTypes)
            result.add(cs.toString());
        final int THIS_PARAM_INDEX = 0;
        result.remove(THIS_PARAM_INDEX);
        return result;
    }

    private Set<MethodAnnotationPair> getAnnotatedMethods(Set<ClassDef> classes) {
        Set<MethodAnnotationPair> result = new HashSet<>();
        for (ClassDef classDef : classes)
            for (Method method : classDef.getMethods())
                for (Annotation a : method.getAnnotations()) {
                    if (a.getType().equals(METHOD_PERMISSION_ANNOTATION)) {
                        final int flags = method.getAccessFlags();
                        boolean isPublic = AccessFlags.PUBLIC.isSet(flags);
                        boolean isStatic = AccessFlags.STATIC.isSet(flags);
                        if (isPublic && isStatic)
                            result.add(new MethodAnnotationPair(method, a));
                        else
                            PrintWarning("ignoring method " + method.getDefiningClass() + "." + method.getName() + " " +
                                                 "because is not static and public");
                        break;
                    }
                }
        return result;
    }

    private Set<ClassDef> getAnnotatedClasses(DexFile dexFile) {
        Set<ClassDef> result = new HashSet<>();
        for (ClassDef classDef : dexFile.getClasses())
            for (Annotation a : classDef.getAnnotations()) {
                if (a.getType().equals(CUSTOM_METHOD_CLASS_ANNOTATION)) {
                    if (AccessFlags.PUBLIC.isSet(classDef.getAccessFlags()))
                        result.add(classDef);
                    else
                        PrintWarning("ignoring class " + classDef + " because it is not public");
                    break;
                }
            }
        return result;
    }

    private void PrintWarning(String msg) {
        out.printf(IOutput.Level.NORMAL, "Warning: %s", msg);
    }

}
