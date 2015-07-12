package it.rmperm;


import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AnalysisHelper {

    public static void Analyze(String source) {
        try {
            DexFile dexFile = DexFileFactory.loadDexFile(new File(source), 19, false);
            for (ClassDef classDef : dexFile.getClasses()) {
                for (Method method : classDef.getMethods()) {
                    MethodImplementation implementation = method.getImplementation();
                    for (Instruction i : implementation.getInstructions()) {
                        System.out.println(i.getClass().getName());
                    }
                }
            }
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
        System.exit(-1988);
    }

}
