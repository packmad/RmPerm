package it.unige.dibris.rmperm;


import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.BuilderInstruction;
import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.builder.instruction.BuilderInstruction35c;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.immutable.ImmutableClassDef;
import org.jf.dexlib2.immutable.ImmutableMethod;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.*;

class Customizer {
    private final Map<MethodReference, Set<String>> apiToPermissions;
    private final Map<MethodReference, MethodReference> redirections;
    private final List<ClassDef> customClasses;
    private final boolean noAutoRemoveVoid;
    private final File inputFile;
    private final File outputDex;
    private final IOutput out;
    private int nRedirected;
    private int nNotRedirected;
    private int nRemoved;

    public Customizer(Map<MethodReference, Set<String>> apiToPermissions,
                      Map<MethodReference, MethodReference> redirections,
                      List<ClassDef> customClasses,
                      File inputFile,
                      File outputDex,
                      IOutput out,
                      boolean noAutoRemoveVoid
    ) {
        this.apiToPermissions = apiToPermissions;
        this.redirections = redirections;
        this.customClasses = customClasses;
        this.inputFile = inputFile;
        this.outputDex = outputDex;
        this.out = out;
        this.noAutoRemoveVoid = noAutoRemoveVoid;
    }

    public void customize() throws IOException {
        nRedirected = nRemoved = 0;
        final List<ClassDef> classes = new ArrayList<>(customClasses);
        DexFile dexFile = DexFileFactory.loadDexFile(inputFile, 19, false);
        for (ClassDef classDef : dexFile.getClasses())
            classes.add(customizeClass(classDef));
        if (nNotRedirected == 0)
            out.printf(IOutput.Level.VERBOSE,
                       "Removed %d invocation(s) and redirected %d (no NOT redirected)",
                       nRemoved,
                       nRedirected);
        else
            out.printf(IOutput.Level.ERROR,
                       "Removed %d invocation(s), redirected %d, NOT redirected %d\n",
                       nRemoved,
                       nRedirected,
                       nNotRedirected);
        String outputFilename = outputDex.getCanonicalPath();
        out.printf(IOutput.Level.DEBUG, "Writing DEX-file: %s\n", outputFilename);
        DexFileFactory.writeDexFile(outputFilename, new DexFile() {
            @Override
            @Nonnull
            public Set<? extends ClassDef> getClasses() {
                return new AbstractSet<ClassDef>() {
                    @Nonnull
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

    private ClassDef customizeClass(ClassDef classDef) {
        List<Method> methods = new ArrayList<>();
        boolean modifiedMethod = false;
        for (Method method : classDef.getMethods()) {
            MethodImplementation implementation = method.getImplementation();
            if (implementation == null) {
                methods.add(method);
                continue;
            }
            MethodImplementation customImpl = searchAndReplaceInvocations(implementation);
            modifiedMethod = true;
            final ImmutableMethod newMethod = new ImmutableMethod(method.getDefiningClass(),
                                                                  method.getName(),
                                                                  method.getParameters(),
                                                                  method.getReturnType(),
                                                                  method.getAccessFlags(),
                                                                  method.getAnnotations(),
                                                                  customImpl);
            methods.add(newMethod);
        }
        if (!modifiedMethod)
            return classDef;
        return new ImmutableClassDef(classDef.getType(),
                                     classDef.getAccessFlags(),
                                     classDef.getSuperclass(),
                                     classDef.getInterfaces(),
                                     classDef.getSourceFile(),
                                     classDef.getAnnotations(),
                                     classDef.getFields(),
                                     methods);
    }

    private MethodImplementation searchAndReplaceInvocations(MethodImplementation origImplementation) {
        MutableMethodImplementation newImplementation = new MutableMethodImplementation(origImplementation);
        List<BuilderInstruction> instructions = newImplementation.getInstructions();
        for (int i = 0; i < instructions.size(); i++) {
            Instruction instruction = instructions.get(i);
            if (instruction.getOpcode() == Opcode.INVOKE_VIRTUAL) {
                BuilderInstruction35c invokeVirtualInstruction = (BuilderInstruction35c) instruction;
                BuilderInstruction35c newInstruction = checkInstruction(invokeVirtualInstruction);
                if (newInstruction == invokeVirtualInstruction)
                    continue;
                if (newInstruction == null) {
                    ++nRemoved;
                    newImplementation.removeInstruction(i--);
                    continue;
                }
                ++nRedirected;
                newImplementation.replaceInstruction(i, newInstruction);
            }
        }
        return newImplementation;
    }

    private BuilderInstruction35c checkInstruction(BuilderInstruction35c invokeInstr) {
        MethodReference r = (MethodReference) invokeInstr.getReference();
        Set<String> permissions = apiToPermissions.get(r);
        if (permissions == null)
            return invokeInstr;
        assert !permissions.isEmpty();
        //out.printf(IOutput.Level.DEBUG, "Method %s.%s uses %s\n", r.getDefiningClass(), r.getName(), permissions);
        MethodReference redirection = redirections.get(r);
        if (redirection != null) {
            out.printf(IOutput.Level.DEBUG, "Applying redirection to %s\n", redirection);
            return new BuilderInstruction35c(Opcode.INVOKE_STATIC,
                                             invokeInstr.getRegisterCount(),
                                             invokeInstr.getRegisterC(),
                                             invokeInstr.getRegisterD(),
                                             invokeInstr.getRegisterE(),
                                             invokeInstr.getRegisterF(),
                                             invokeInstr.getRegisterG(),
                                             redirection);
        }
        if (r.getReturnType()
             .equals("V") && !noAutoRemoveVoid) {
            out.printf(IOutput.Level.DEBUG,
                       "Can't find a specific redirection, but it's a void method, so I throw " + "the invocation away\n");
            return null;
        }
        ++nNotRedirected;
        out.printf(IOutput.Level.ERROR,
                   "Method %s.%s uses %s, but I don't have a redirection for it\n",
                   r.getDefiningClass(),
                   r.getName(),
                   permissions);
        return invokeInstr;
    }

}
