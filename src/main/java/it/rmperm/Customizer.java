package it.rmperm;


import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.BuilderInstruction;
import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.builder.instruction.BuilderInstruction11n;
import org.jf.dexlib2.builder.instruction.BuilderInstruction11x;
import org.jf.dexlib2.builder.instruction.BuilderInstruction35c;
import org.jf.dexlib2.dexbacked.instruction.DexBackedInstruction35c;
import org.jf.dexlib2.dexbacked.reference.DexBackedMethodReference;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.reference.Reference;
import org.jf.dexlib2.immutable.ImmutableClassDef;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.util.ExceptionWithContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class Customizer {

    private static DexMethod tmpDM = null;

    public static void doTheDirtyWork(String src, Path dst, List<ClassDef> customClasses) throws Exception {
        File file = new File(src);

        try {
            final List<ClassDef> classes = new ArrayList(customClasses);
            DexFile dexFile = DexFileFactory.loadDexFile(file, 19, false);
            for (ClassDef classDef : dexFile.getClasses()) {
                List<Method> methods = new ArrayList();
                boolean modifiedMethod = false;

                for (Method method : classDef.getMethods()) {
                    MethodImplementation implementation = method.getImplementation();
                    DexMethod replMeth;
                    if (implementation != null && (replMeth = containsPermMethInvoke(implementation)) != null) {
                        modifiedMethod = true;
                        methods.add(new ImmutableMethod(
                                method.getDefiningClass(),
                                method.getName(),
                                method.getParameters(),
                                method.getReturnType(),
                                method.getAccessFlags(),
                                method.getAnnotations(),
                                replaceMethodWithNull(implementation, replMeth)));

                    } else {
                        methods.add(method);
                    }
                }
                if (!modifiedMethod) {
                    //System.out.println(classDef.toString());
                    classes.add(classDef);
                } else {
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
            }

            DexFileFactory.writeDexFile(dst.toString(), new DexFile() {
                @Override
                public Set<? extends ClassDef> getClasses() {
                    return new AbstractSet<ClassDef>() {
                        //@Nonnull
                        @Override
                        public Iterator<ClassDef> iterator() {
                            return classes.iterator();
                        }

                        @Override
                        public int size() {
                            return classes.size();
                        }
                    };
                }
            });
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
        catch (ExceptionWithContext ewc) {
            ewc.printStackTrace();
            System.err.println("You are working on a file that has already been hacked");
        }

    }

    private static DexMethod containsPermMethInvoke(MethodImplementation implementation) {


        if (implementation != null) {
            for (Instruction i : implementation.getInstructions()) {
                if (i instanceof DexBackedInstruction35c) {
                    DexBackedInstruction35c i35c = (DexBackedInstruction35c) i;
                    Reference ref = i35c.getReference();
                    if (ref instanceof DexBackedMethodReference) {
                        DexBackedMethodReference r = (DexBackedMethodReference) ref;
                        tmpDM = new DexMethod(r.getDefiningClass(), r.getName(), r.getParameterTypes(), r.getReturnType());
                        DexMethod getDM;
                        getDM = Main.fakedPerms.getContain(tmpDM);
                        if (getDM != null) {
                            return getDM;
                        }
                        getDM = Main.allPerms.getContain(tmpDM);
                        if (getDM != null && Main.manifestManager.mustBeRemoved(getDM.getPermission())) { // found a method that must be faked
                            System.err.println("MISSING: " + getDM);
                        }
                    }
                }
            }
        }
        return null;
    }

    private static MethodImplementation replaceMethodWithNull(MethodImplementation implementation, DexMethod replMeth) throws Exception {
        if (implementation != null) {
            MutableMethodImplementation mutableImplementation = new MutableMethodImplementation(implementation);
            List<BuilderInstruction> instructions = mutableImplementation.getInstructions();
            for (int i=0; i<instructions.size(); i++) {
                Instruction instruction = instructions.get(i);
                if (instruction instanceof BuilderInstruction35c) {
                    BuilderInstruction35c bi35c = (BuilderInstruction35c) instruction;
                    DexBackedMethodReference r = (DexBackedMethodReference) bi35c.getReference();
                    tmpDM = new DexMethod(r.getDefiningClass(), r.getName(), r.getParameterTypes(), r.getReturnType());
                    if (replMeth.equals(tmpDM)) {
                        // replaceCallWithNull(mutableImplementation, instructions, i, bi35c);
                        LinkedList<String> hackParms = new LinkedList<>(r.getParameterTypes());
                        hackParms.add(0, r.getDefiningClass());

                        Reference hackRef = new DexMethod(
                                replMeth.getPermission(),
                                Main.customClassObj,
                                "hack_" + r.getName(),
                                hackParms,
                                r.getReturnType()
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
                        /*
                        if (replMeth.getPermission().equals("android.permission.ACCESS_FINE_LOCATION"))
                            System.out.println(tmpDM);
                            */
                        System.out.println("REPLACED: " + tmpDM + " WITH: " + hackRef);
                    }
                }
            }
            return mutableImplementation;
        }
        return null;
    }

    private static void replaceCallWithNull(MutableMethodImplementation mutableImplementation, List<BuilderInstruction> instructions, int i, BuilderInstruction35c bi35c) {
        Instruction instruction;
        mutableImplementation.removeInstruction(i); // remove the method invocation
        instruction = instructions.get(i);
        assert (instruction instanceof BuilderInstruction11x);
        BuilderInstruction11x i11x = (BuilderInstruction11x) instruction; // move-result-obj

        /*
        int[] bi35cRegs = {
                bi35c.getRegisterC(),
                bi35c.getRegisterD(),
                bi35c.getRegisterE(),
                bi35c.getRegisterF(),
                bi35c.getRegisterG(),
        };
        for(int reg : bi35cRegs) {

            //System.out.println(reg);
        }
        */
        int retReg = i11x.getRegisterA();
        //System.out.println(retReg);
        mutableImplementation.replaceInstruction(i, new BuilderInstruction11n(Opcode.CONST_4, retReg, 0));
    }

}
