package it.rmperm;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;



public class Main {
    public static Path workingDir;
    public static final String customClassObj = "Lhack/aonzo/simone/emptyappwithhackclass/HackClass;"; ////TODO: infer from custom dex file
    public static ManifestManager manifestManager; //TODO: move away from main

    private static final String ALLMAPPINGS = "C:\\Users\\Simone\\Downloads\\apks\\mappings\\allMappings.txt"; //TODO: read cmdline
    private static final String FAKEDMAPPINGS = "C:\\Users\\Simone\\Downloads\\apks\\mappings\\fakedMappings.txt";//TODO: read from custom dexes

    public static PermissionLoader allPerms; //TODO: move away from main
    public static PermissionLoader fakedPerms; //TODO: move away from main

    public static void main(String[] args) throws Exception {


        String akpPath = "C:\\Users\\Simone\\Downloads\\apks\\com.boombit.RunningCircles.apk"; //TODO: read cmdline
        String outDir = "C:\\Users\\Simone\\Downloads\\apks\\outputs\\"; //TODO: read cmdline
        String apkName = FilenameUtils.getBaseName(akpPath);

        workingDir = Paths.get(System.getProperty("java.io.tmpdir"), "rmperm", apkName);
        //System.out.println(workingDir);

        FileUtils.deleteDirectory(new File(workingDir.toString()));
        String[] apktoolCmd = {"d", "-s", akpPath, "-f", "-o", workingDir.toString()};
        brut.apktool.Main.main(apktoolCmd);

        String customDex = "C:\\Users\\Simone\\AndroidStudioProjects\\EmptyAppWithHackClass\\app\\build\\outputs\\apk\\app-debug.apk"; //TODO: read cmdline
        CustomMethodManager cmm = new CustomMethodManager(Paths.get(customDex), customClassObj);

        allPerms = new PermissionLoader(ALLMAPPINGS);
        fakedPerms = new PermissionLoader(FAKEDMAPPINGS);

        Path manifestPath = Paths.get(workingDir.toString(), "AndroidManifest.xml");
        manifestManager = new ManifestManager(manifestPath.toString());


        Customizer.doTheDirtyWork(akpPath, Paths.get(workingDir.toString(), "classes.dex"), cmm.getCustomClasses());

        apktoolCmd = new String[] {"b", workingDir.toString()};
        brut.apktool.Main.main(apktoolCmd);
        Path newApk = Paths.get(workingDir.toString(), "dist", apkName + ".apk");
        //TODO: zipalign
        //TODO: sign
    }


}
