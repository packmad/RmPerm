package it.saonzo.rmperm;


import java.io.File;

public class RmAds extends AbsRmPerm {


    public RmAds(IOutput out, String inApkFilename, String outApkFilename) {
        super(out, inApkFilename, outApkFilename);
    }

    public void removeAds() throws Exception {
        final File tmpApkFile = File.createTempFile("OutputApk", null);
        tmpApkFile.deleteOnExit();
        final File tmpClassesDex = File.createTempFile("NewClasseDex", null);
        tmpClassesDex.deleteOnExit();

        BytecodeCustomizer bc = new BytecodeCustomizer(new File(inApkFilename), tmpClassesDex, out);
        bc.customize();

        writeApk(tmpClassesDex, null, tmpApkFile);
        signApk(tmpApkFile);

    }
}
