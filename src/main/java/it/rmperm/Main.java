package it.rmperm;

import brut.common.BrutException;
import it.rmperm.loader.AllMethodsLoader;
import it.rmperm.loader.CustomMethodsLoader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;



public class Main {
    public static Path workingDir;
    public static final String customClassObj = "Lhack/aonzo/simone/emptyappwithhackclass/HackClass;"; //TODO: infer from custom dex file
    public static final String zipAlignPath = "C:\\Program Files (x86)\\Android\\android-sdk\\build-tools\\21.1.2\\zipalign.exe"; //TODO: alt way
    public static final String jarSignerPath = "C:\\Program Files\\Java\\jdk1.8.0_45\\bin\\jarsigner"; //TODO: alt way
    public static ManifestManager manifestManager; //TODO: move away from main

    private static final String ALLMAPPINGS = "C:\\Users\\Simone\\workspace\\rmperm\\files\\allMappings.txt"; //TODO: relative path

    public static void main(String[] args) {

        String akpPath = "C:\\Users\\Simone\\Downloads\\apks\\com.boombit.RunningCircles.apk"; //TODO: read cmdline
        String outDir = "C:\\Users\\Simone\\Downloads\\apks\\outputs\\"; //TODO: read cmdline
        String apkName = FilenameUtils.getBaseName(akpPath);

        workingDir = Paths.get(System.getProperty("java.io.tmpdir"), "rmperm", apkName);
        //System.out.println(workingDir);

        try {
            FileUtils.deleteDirectory(new File(workingDir.toString()));
            String[] apktoolCmd = {"d", "-s", akpPath, "-f", "-o", workingDir.toString()};
            brut.apktool.Main.main(apktoolCmd);
        } catch (IOException|InterruptedException|BrutException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        String customDex = "C:\\Users\\Simone\\AndroidStudioProjects\\EmptyAppWithHackClass\\app\\build\\outputs\\apk\\app-debug.apk"; //TODO: read cmdline
        CustomMethodsLoader customMethods = new CustomMethodsLoader(Paths.get(customDex), customClassObj);
        AllMethodsLoader allMethods = new AllMethodsLoader(ALLMAPPINGS);

        Path manifestPath = Paths.get(workingDir.toString(), "AndroidManifest.xml");
        manifestManager = new ManifestManager(manifestPath.toString());


        Customizer customizer = new Customizer(
                allMethods.getPermissionToMethods(),
                customMethods.getPermissionToCustomMethods(),
                customMethods.getCustomClasses(),
                manifestManager.getRemovedPerms(),
                akpPath,
                Paths.get(workingDir.toString(), "classes.dex"));
        customizer.doTheDirtyWork();

        Path newApkUnaligned = Paths.get(workingDir.toString(), "dist", apkName + "-unaligned.apk");
        String[] apktoolCmd = new String[] {"b", workingDir.toString(), "-o", newApkUnaligned.toString()};
        try {
            brut.apktool.Main.main(apktoolCmd);
        } catch (IOException|InterruptedException|BrutException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        System.out.println(newApkUnaligned.toString());

        Path newApkAligned = Paths.get(outDir, apkName + ".apk");

        //TODO: zipalign and sign programmatically
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    zipAlignPath,
                    "-f",
                    "-v", "4",
                    newApkUnaligned.toString(),
                    newApkAligned.toString()
                );
            pb.inheritIO();
            Process p = pb.start();
            p.waitFor();
            pb = new ProcessBuilder(
                    jarSignerPath,
                    "-verbose",
                    "-sigalg", "SHA1withRSA",
                    "-digestalg", "SHA1",
                    "-keystore", "C:\\Users\\Simone\\.android\\debug.keystore",
                    newApkAligned.toString(),
                    "androiddebugkey",
                    "-storepass", "android"
            );
            pb.inheritIO();
            p = pb.start();
            p.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(newApkAligned.toString());
    }


}
