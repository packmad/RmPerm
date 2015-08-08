package it.unige.dibris.rmperm;


import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.BuilderInstruction;
import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.builder.instruction.BuilderInstruction11n;
import org.jf.dexlib2.builder.instruction.BuilderInstruction11x;
import org.jf.dexlib2.builder.instruction.BuilderInstruction35c;
import org.jf.dexlib2.dexbacked.reference.DexBackedMethodReference;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.reference.Reference;
import org.jf.dexlib2.immutable.ImmutableClassDef;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class Customizer {
    private final Hashtable<String, List<DexMethod>> reducedPermToMethods = new Hashtable<>();
    private final Hashtable<String, List<DexMethod>> reducedPermToCustomMethods = new Hashtable<>();
    private final List<ClassDef> customClasses;
    private final File inputFile;
    private final String dstApkFilename;
    private final IOutput out;

    public Customizer(
            Hashtable<String, List<DexMethod>> permissionToMethods,
            Map<MethodReference, MethodReference> redirections,
            List<ClassDef> customClasses,
            String src,
            String dstApkFilename,
            IOutput out) {
        this.customClasses = customClasses;
        this.inputFile = new File(src);
        this.dstApkFilename = dstApkFilename;
        this.out = out;
    }

    public void doTheDirtyWork() {
        try {
            final List<ClassDef> classes = new ArrayList(customClasses); //TODO: test if it can be added later
            DexFile dexFile = DexFileFactory.loadDexFile(inputFile, 19, false);
            for (ClassDef classDef : dexFile.getClasses()) {
                List<Method> methods = new ArrayList();
                boolean modifiedMethod = false;

                for (Method method : classDef.getMethods()) {
                    MethodImplementation implementation = method.getImplementation();
                    MethodImplementation customImpl;
                    if (implementation != null) {
                        customImpl = searchAndReplaceInvocations(implementation);
                        if (customImpl != null) {
                            modifiedMethod = true;
                            methods.add( new ImmutableMethod (
                                    method.getDefiningClass(),
                                    method.getName(),
                                    method.getParameters(),
                                    method.getReturnType(),
                                    method.getAccessFlags(),
                                    method.getAnnotations(),
                                    customImpl
                                    ));
                        }
                    }
                    else {
                        methods.add(method);
                    }
                }
                if (modifiedMethod) {
                    classes.add(new ImmutableClassDef(
                            classDef.getType(),
                            classDef.getAccessFlags(),
                            classDef.getSuperclass(),
                            classDef.getInterfaces(),
                            classDef.getSourceFile(),
                            classDef.getAnnotations(),
                            classDef.getFields(),
                            methods));
                }
                else {
                    classes.add(classDef);
                }
            }

            DexFileFactory.writeDexFile(dstApkFilename, new DexFile() {
                    @Override
                    public Set<? extends ClassDef> getClasses() {
                        return new AbstractSet<ClassDef>() {
                            @Nonnull
                            @Override
                            public Iterator<ClassDef> iterator() { return classes.iterator(); }
                            @Override
                            public int size() { return classes.size(); }
                        };
                    }
                }
            );
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
        catch (ExceptionWithContext ewc) {
            ewc.printStackTrace();
            //System.err.println("You are working on a inputFile that has already been hacked");
        }

    }


    private boolean contains(Hashtable<String, List<DexMethod>> ht, DexMethod dm) {
        for (List<DexMethod> dpmList : ht.values())
        {
            if (dpmList.contains(dm))
                return true;
        }
        return false;
    }


    private MethodImplementation searchAndReplaceInvocations(MethodImplementation implementation) {
        MutableMethodImplementation mutableImplementation = new MutableMethodImplementation(implementation);
        List<BuilderInstruction> instructions = mutableImplementation.getInstructions();
        for (int i=0; i<instructions.size(); i++) {
            Instruction instruction = instructions.get(i);
            if (instruction instanceof BuilderInstruction35c) {
                BuilderInstruction35c bi35c = (BuilderInstruction35c) instruction;
                Reference r = bi35c.getReference();
                if (r instanceof DexBackedMethodReference) {
                    DexBackedMethodReference dmbr = (DexBackedMethodReference) r;
                    /*
                    DexMethod tmpDM = new DexMethod(dmbr.getDefiningClass(), dmbr.getName(), dmbr.getParameterTypes(), dmbr.getReturnType());

                    if (contains(reducedPermToCustomMethods, tmpDM)) { // this call must be replaced
                        LinkedList<String> hackParms = new LinkedList<>(dmbr.getParameterTypes());
                        hackParms.add(0, dmbr.getDefiningClass()); // defClass as first argument

                        Reference hackRef = new DexMethod(
                                Main.customClassObj,
                                CustomMethodsLoader.prefixMethodConvention + dmbr.getName(),
                                hackParms,
                                dmbr.getReturnType()
                        );
                        mutableImplementation.replaceInstruction(
                                i,
                                new BuilderInstruction35c(
                                        Opcode.INVOKE_STATIC,
                                        bi35c.getRegisterCount(),
                                        bi35c.getRegisterC(),
                                        bi35c.getRegisterD(),
                                        bi35c.getRegisterE(),
                                        bi35c.getRegisterF(),
                                        bi35c.getRegisterG(),
                                        hackRef
                                )
                        );
                        if (Main.verboseOutput) {
                            System.out.println("REPLACED: " + tmpDM + " WITH: " + hackRef);
                        }
                    }
                    else if (contains(reducedPermToMethods, tmpDM)) {
                        String err = "MISSING: " + tmpDM;
                        if (tmpDM.getReturnType().equals("V") && Main.autoRemoveVoid) {
                            mutableImplementation.removeInstruction(i--);
                            err += "...but it's return type is void, so I removed it!";
                            System.out.println(err);
                        }
                        else
                            System.err.println(err);
                    }*/

                }
            }
        }
        return mutableImplementation;
    }

    // maybe useful in future
    private void replaceCallWithNull(MutableMethodImplementation mutableImplementation, List<BuilderInstruction> instructions, int i, BuilderInstruction35c bi35c) {
        Instruction instruction;
        mutableImplementation.removeInstruction(i); // remove the method invocation
        instruction = instructions.get(i);
        assert (instruction instanceof BuilderInstruction11x);
        BuilderInstruction11x i11x = (BuilderInstruction11x) instruction; // move-result-obj
        mutableImplementation.replaceInstruction(i, new BuilderInstruction11n(Opcode.CONST_4, i11x.getRegisterA(), 0));
    }

}