package it.unige.dibris.rmperm;

import brut.common.BrutException;
import it.unige.dibris.rmperm.loader.AllMethodsLoader;
import it.unige.dibris.rmperm.loader.CustomMethodsLoader;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Main {
    private static final Options options = new Options();
    public static Path workingDir;
    public static final String customClassObj = "Lhack/aonzo/simone/emptyappwithhackclass/HackClass;"; //TODO: infer from custom dex file
    public static final String zipAlignPath = "C:\\Program Files (x86)\\Android\\android-sdk\\build-tools\\21.1.2\\zipalign.exe"; //TODO: alt way
    public static final String jarSignerPath = "C:\\Program Files\\Java\\jdk1.8.0_45\\bin\\jarsigner"; //TODO: alt way
    private static final String allMappings = "C:\\Users\\Simone\\workspace\\rmperm\\files\\allMappings.txt"; //TODO: relative path

    private static HashSet<String> permsToRem = new HashSet<>(); // -p
    private static String akpPath; // -s
    private static String outDir; // -d --d
    private static String customDex; // -c --custom
    private static boolean listPerms; // -l --list
    public static boolean verboseOutput; // -v --verbose
    public static boolean autoRemoveVoid = true; // -n --no-auto-remove-void

    public static void main(String[] args) {
        parseCmdLine(args);
        String apkName = FilenameUtils.getBaseName(akpPath);
        workingDir = Paths.get(System.getProperty("java.io.tmpdir"), "rmperm", apkName);

        try {
            FileUtils.deleteDirectory(new File(workingDir.toString()));
            String[] apktoolCmd = {"d", "-s", "-f", "-s", akpPath, "-o", workingDir.toString()};
            brut.apktool.Main.main(apktoolCmd);
        } catch (IOException|InterruptedException|BrutException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        CustomMethodsLoader customMethods = new CustomMethodsLoader(Paths.get(customDex), customClassObj);
        AllMethodsLoader allMethods = new AllMethodsLoader(allMappings);

        Path manifestPath = Paths.get(workingDir.toString(), "AndroidManifest.xml");
        ManifestManager manifestManager = new ManifestManager(manifestPath.toString(), permsToRem, listPerms);

        Customizer customizer = new Customizer(
                allMethods.getPermissionToMethods(),
                customMethods.getPermissionToCustomMethods(),
                customMethods.getCustomClasses(),
                manifestManager.getPermsToRem(),
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


    private static void parseCmdLine(String[] args) {
        options.addOption("s", "src", true, "Apk source path")
                    .addOption("d", "dest", true, "Destination folder")
                    .addOption("c", "cust", true, "apk/dex with custom class/methods path")
                    .addOption("p", "perms", true, "CSV permissions list to remove")
                    .addOption("l", "list", false, "Read and list the permissions in the manifest, then STOP the application")
                    .addOption("v", "verbose", false, "Verbose output")
                    .addOption("n", "no-auto-remove-void", false, "Disable the auto removal of void methods");
            CommandLine cmdline = null;
            CommandLineParser parser = new GnuParser();
            try {
                cmdline = parser.parse(options, args);
            } catch (ParseException exp) {
                System.err.println("Error parsing command line: " + exp.getMessage());
                printHelp();
                System.exit(-1);
        }


        if (cmdline.hasOption("s") || cmdline.hasOption("src")) {
            akpPath = cmdline.getOptionValue("s");
        }
        else {
            paramsError("Missing src");
        }
        if (cmdline.hasOption("d") || cmdline.hasOption("dest")) {
            outDir = cmdline.getOptionValue("d");
        }
        else {
            paramsError("Missing dest");
        }
        if (cmdline.hasOption("c") || cmdline.hasOption("cust")) {
            customDex = cmdline.getOptionValue("c");
        }
        else {
            paramsError("Missing cust");
        }
        if (cmdline.hasOption("p") || cmdline.hasOption("perms")) {
            Pattern permPattern = Pattern.compile("^android.permission.[A-Z_]*$");
            String[] perms = cmdline.getOptionValue("p").split(",");
            for(String p : perms) {
                Matcher permMatcher = permPattern.matcher(p);
                if (!permMatcher.matches())
                    paramsError("Permissions are in wrong format. \n E.g.: android.permission.INTERNET,android.permission.GET_TASKS");
                permsToRem.add(p);
            }
        }
        else {
            paramsError("Missing perms");
        }
        if (listPerms)
            System.out.println("With -l option the app will terminate after displaying the permissions!");
        verboseOutput = cmdline.hasOption("v") || cmdline.hasOption("verbose");
        autoRemoveVoid = !(cmdline.hasOption("n") || cmdline.hasOption("no-auto-remove-void"));
    }

    private static void paramsError(String s) {
        System.err.println(s);
        printHelp();
        System.exit(-1);
    }

    private static void printHelp() {
        final String commandLineSyntax = "ciao";
        final PrintWriter writer = new PrintWriter(System.err);
        final HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("rmperm", options);
        writer.close();
    }

}
