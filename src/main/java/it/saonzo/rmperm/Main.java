package it.saonzo.rmperm;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.reference.MethodReference;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import kellinwood.security.zipsigner.ZipSigner;

public class Main {
    private static final String OPTION_CUSTOM_METHODS = "custom-methods";
    private static final String OPTION_DEBUG = "debug";
    private static final String OPTION_HELP = "help";
    private static final String OPTION_INPUT = "input";
    private static final String OPTION_LIST = "list";
    private static final String OPTION_NO_AUTO_REMOVE_VOID = "no-auto-remove";
    private static final String OPTION_OUTPUT = "output";
    private static final String OPTION_PERMISSIONS = "permissions";
    private static final String OPTION_REMOVE = "remove";
    private static final String OPTION_REMOVE_ONLY_ADS = "removeads";
    private static final String OPTION_ADSREMOVAL = "ads";
    private static final String OPTION_STATISTICS = "statistics";
    private static final String OPTION_VERBOSE = "verbose";

    private final CommandLine cmdLine;
    private final String inApkFilename;
    private final String outApkFilename;
    private final String customMethodsFilename;
    private final String csvPermissionsToRemove;
    private final String folderToAnalyze;
    private final boolean noAutoRemoveVoid;
    private final boolean adsRemoval;
    private IOutput out;

    public static void main(String[] args) throws NoSuchProviderException, NoSuchAlgorithmException {
        try {
            new Main(args).main();
        } catch (BadCommandLineException e) {
            final String message = e.getMessage();
            if (message != null)
                System.err.println(message);
        }
    }

    public static void androidMain(IOutput iOutput, String[] args) {
        try {
            Main main = new Main(args);
            main.setIOutput(iOutput);
            main.main();
        } catch (BadCommandLineException e) {
            final String message = e.getMessage();
            if (message != null)
                iOutput.printf(IOutput.Level.ERROR, message);
        }
    }

    private void setIOutput(IOutput iOutput) {
        out = iOutput;
    }

    private static class BadCommandLineException extends Exception {
        private BadCommandLineException() {
            super();
        }

        private BadCommandLineException(String message) {
            super(message);
        }
    }

    private Main(String[] args) throws BadCommandLineException {
        cmdLine = parseCmdLine(args);
        if (cmdLine == null)
            throw new BadCommandLineException("You haven't provided any command line argument");
        IOutput.Level outputLevel = IOutput.Level.NORMAL;
        if (cmdLine.hasOption(OPTION_DEBUG))
            outputLevel = IOutput.Level.DEBUG;
        else if (cmdLine.hasOption(OPTION_VERBOSE))
            outputLevel = IOutput.Level.VERBOSE;
        setIOutput(new ConsoleOutput(outputLevel));
        inApkFilename = cmdLine.getOptionValue(OPTION_INPUT);
        checkFileHasApkExtension(inApkFilename);
        outApkFilename = cmdLine.getOptionValue(OPTION_OUTPUT);
        checkFileHasApkExtension(outApkFilename);
        customMethodsFilename = cmdLine.getOptionValue(OPTION_CUSTOM_METHODS);
        csvPermissionsToRemove = cmdLine.getOptionValue(OPTION_PERMISSIONS);
        noAutoRemoveVoid = cmdLine.hasOption(OPTION_NO_AUTO_REMOVE_VOID);
        adsRemoval = cmdLine.hasOption(OPTION_ADSREMOVAL);
        folderToAnalyze = cmdLine.getOptionValue(OPTION_STATISTICS);
        checkIsFolder(folderToAnalyze);
    }

    private void checkFileHasApkExtension(String filePath) throws BadCommandLineException {
        if (filePath != null) {
            File file = new File(filePath);
            if (file.isDirectory()) {
                out.printf(IOutput.Level.ERROR, "This is a path of a directory '%s'. File needed.", filePath);
                throw new BadCommandLineException();
            }
            final int indexOfDot = filePath.lastIndexOf(".");
            if (indexOfDot == -1 || !filePath.substring(indexOfDot).equalsIgnoreCase(".apk")) {
                out.printf(IOutput.Level.ERROR, "The extension of the file '%s' must be '.apk'.", filePath);
                throw new BadCommandLineException();
            }
        }
    }


    private void checkIsFolder(String folderPath) throws BadCommandLineException {
        if (folderPath != null) {
            File file = new File(folderPath);
            if (!file.isDirectory()) {
                out.printf(IOutput.Level.ERROR, "This is a path for a file '%s'. Folder needed.", folderPath);
                throw new BadCommandLineException();
            }
        }
    }


    private void chechNonsenseOptions(final String currentOption, List<String> forbiddenOptions) throws BadCommandLineException {
        for (String option : forbiddenOptions) {
            if (cmdLine.getOptionValue(option) != null) {
                throw new BadCommandLineException(
                        "Error: option " + option + " makes no sense when --" + currentOption + " is specified\n"
                );
            }
        }
    }


    private void main() throws BadCommandLineException {
        List<String> forbiddenOptions = new ArrayList<>();

        if (cmdLine.hasOption(OPTION_LIST)) {
            forbiddenOptions.add(OPTION_ADSREMOVAL);
            forbiddenOptions.add(OPTION_CUSTOM_METHODS);
            forbiddenOptions.add(OPTION_HELP);
            forbiddenOptions.add(OPTION_OUTPUT);
            forbiddenOptions.add(OPTION_PERMISSIONS);
            forbiddenOptions.add(OPTION_REMOVE);
            forbiddenOptions.add(OPTION_STATISTICS);
            chechNonsenseOptions(OPTION_LIST, forbiddenOptions);
            listPermissions();
        } else if (cmdLine.hasOption(OPTION_REMOVE)) {
            forbiddenOptions.add(OPTION_LIST);
            forbiddenOptions.add(OPTION_REMOVE_ONLY_ADS);
            forbiddenOptions.add(OPTION_STATISTICS);
            chechNonsenseOptions(OPTION_REMOVE, forbiddenOptions);
            if (outApkFilename == null || customMethodsFilename == null || csvPermissionsToRemove == null) {
                out.printf(IOutput.Level.ERROR,
                        "Arguments --%s, --%s and --%s are required when using --%s\n",
                        OPTION_OUTPUT,
                        OPTION_CUSTOM_METHODS,
                        OPTION_PERMISSIONS,
                        OPTION_REMOVE);
                throw new BadCommandLineException();
            }
            try {
                removePermissions();
            } catch (IOException e) {
                out.printf(IOutput.Level.ERROR, "I/O error: %s\n", e.getMessage());
            } catch (Exception e) {
                out.printf(IOutput.Level.ERROR, "Error: %s\n", e.getMessage());
            }
        } else if (cmdLine.hasOption(OPTION_REMOVE_ONLY_ADS)) { // removes only ads, it doesn't remove any permissions
            forbiddenOptions.add(OPTION_CUSTOM_METHODS);
            forbiddenOptions.add(OPTION_HELP);
            forbiddenOptions.add(OPTION_LIST);
            forbiddenOptions.add(OPTION_PERMISSIONS);
            forbiddenOptions.add(OPTION_REMOVE);
            forbiddenOptions.add(OPTION_STATISTICS);
            chechNonsenseOptions(OPTION_REMOVE_ONLY_ADS, forbiddenOptions);
            if (outApkFilename == null) {
                out.printf(IOutput.Level.ERROR, "Argument --%s is required when using --%s\n", OPTION_OUTPUT, OPTION_REMOVE);
                throw new BadCommandLineException();
            }
            try {
                removeAds();
            } catch (Exception e) {
                out.printf(IOutput.Level.ERROR, "Error: %s\n", e.getMessage());
            }
        } else if (cmdLine.hasOption(OPTION_STATISTICS)) {
            calculateStatistics();
        }
    }


    private void removeAds() throws Exception {
        final File tmpApkFile = File.createTempFile("OutputApk", null);
        tmpApkFile.deleteOnExit();
        final File tmpClassesDex = File.createTempFile("NewClasseDex", null);
        tmpClassesDex.deleteOnExit();

        BytecodeCustomizer bc = new BytecodeCustomizer(new File(inApkFilename), tmpClassesDex, out);
        bc.customize();

        writeApk(tmpClassesDex, null, tmpApkFile);
        signApk(tmpApkFile);

    }


    private void removePermissions() throws Exception {
        Set<String> permissionsToRemove = parseCsvPermissions();
        out.printf(IOutput.Level.VERBOSE, "Removing permission(s): %s\n", permissionsToRemove);
        Map<MethodReference, MethodReference> redirections = new HashMap<>();
        List<ClassDef> customClasses = new ArrayList<>();
        new CustomMethodsLoader(out).load(customMethodsFilename, customClasses, redirections, permissionsToRemove);

        int nOfRedirections = redirections.size();
        out.printf(IOutput.Level.VERBOSE, "Loaded %d redirections\n", nOfRedirections);
        if (nOfRedirections <= 0) {
            throw new BadCommandLineException("The Apk/Dex that you provided does not contain the definition of any method ");
        }

        final Map<MethodReference, Set<String>> apiToPermissions;
        apiToPermissions = new PermissionMappingLoader(out).loadMapping(permissionsToRemove);
        final File tmpApkFile = File.createTempFile("OutputApk", null);
        tmpApkFile.deleteOnExit();
        try {
            final File tmpClassesDex = customizeBytecode(apiToPermissions, redirections, customClasses);
            try {
                final File tmpManifestFile = stripPermissionsFromManifest(permissionsToRemove);
                try {
                    writeApk(tmpClassesDex, tmpManifestFile, tmpApkFile);
                } finally {
                    tmpManifestFile.delete();
                }
            } finally {
                tmpClassesDex.delete();
            }
            signApk(tmpApkFile);
        } finally {
            tmpApkFile.delete();
        }
    }


    private void signApk(File tmpApkFile) throws Exception {
        ZipSigner zipSigner = new ZipSigner();
        zipSigner.setKeymode("auto-testkey");
        String inputFilename = tmpApkFile.getCanonicalPath();
        out.printf(IOutput.Level.VERBOSE, "Signing %s into %s\n", inputFilename, outApkFilename);
        zipSigner.signZip(inputFilename, outApkFilename);
    }


    private void writeApk(File tmpClassesDex, File tmpManifestFile, File outApkFile) throws IOException {
        out.printf(IOutput.Level.VERBOSE, "Writing APK %s\n", outApkFile);
        JarFile inputJar = new JarFile(inApkFilename);
        try (ZipOutputStream outputJar = new ZipOutputStream(new FileOutputStream(outApkFile))) {
            Enumeration<JarEntry> entries = inputJar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                final String name = entry.getName();
                outputJar.putNextEntry(new ZipEntry(name));
                if (name.equalsIgnoreCase("AndroidManifest.xml") && tmpManifestFile != null)
                    try (final FileInputStream newManifest = new FileInputStream(tmpManifestFile)) {
                        StreamUtils.copy(newManifest, outputJar);
                    }
                else if (name.equalsIgnoreCase("classes.dex"))
                    try (final FileInputStream newClasses = new FileInputStream(tmpClassesDex)) {
                        StreamUtils.copy(newClasses, outputJar);
                    }
                else
                    try (final InputStream entryInputStream = inputJar.getInputStream(entry)) {
                        StreamUtils.copy(entryInputStream, outputJar);
                    }
                outputJar.closeEntry();
            }
        }
    }


    private File stripPermissionsFromManifest(Set<String> permissionsToRemove) throws IOException {
        AndroidManifest manifest = AndroidManifest.extractManifest(inApkFilename);
        for (String permission : permissionsToRemove) {
            boolean result = manifest.tryToRemovePermission(permission);
            if (!result)
                out.printf(IOutput.Level.ERROR, "Couldn't find %s inside the manifest\n", permission);
        }
        final File tmpManifestFile = File.createTempFile("AndroidManifest", null);
        tmpManifestFile.deleteOnExit();
        manifest.write(tmpManifestFile);
        return tmpManifestFile;
    }


    private File customizeBytecode(Map<MethodReference, Set<String>> apiToPermissions,
                                   Map<MethodReference, MethodReference> redirections,
                                   List<ClassDef> customClasses
    ) throws IOException {
        File tmpClassesDex = File.createTempFile("NewClasseDex", null);
        tmpClassesDex.deleteOnExit();
        BytecodeCustomizer c = new BytecodeCustomizer(
                apiToPermissions,
                redirections,
                customClasses,
                new File(inApkFilename),
                tmpClassesDex,
                out,
                noAutoRemoveVoid,
                adsRemoval
        );
        c.customize();
        return tmpClassesDex;
    }


    private Set<String> parseCsvPermissions() {
        Set<String> permissionsToRemove = new HashSet<>();
        for (String p : csvPermissionsToRemove.split(","))
            permissionsToRemove.add(Permissions.fullPermissionName(p));
        return permissionsToRemove;
    }


    private void listPermissions() {
        AndroidManifest manifest;
        try {
            manifest = AndroidManifest.extractManifest(inApkFilename);
        } catch (IOException e) {
            out.printf(IOutput.Level.ERROR, "Cannot read %s: %s\n", inApkFilename, e.getMessage());
            return;
        }
        List<String> permissions = new ArrayList<>();
        for (String p : manifest.getPermissions())
            permissions.add(Permissions.simplifyPermissionName(p));
        if (permissions.isEmpty()) {
            out.printf(IOutput.Level.NORMAL, "No permissions are requested in the manifest!\n");
            return;
        }
        out.printf(IOutput.Level.NORMAL, "Permissions of %s:\n", inApkFilename);
        // cannot use StringJoiner for Android java 1.7 compatibility
        final StringBuilder sb = new StringBuilder();
        for (String p : permissions) {
            out.printf(IOutput.Level.NORMAL, "%s\n", p);
            sb.append(p);
            sb.append(',');
        }
        out.printf(IOutput.Level.NORMAL,
                "\nTo remove all of them you can pass the parameters:\n--%s --%s %s --%s %s\n",
                OPTION_REMOVE,
                OPTION_INPUT,
                inApkFilename,
                OPTION_PERMISSIONS,
                sb.toString()
        );

    }


    private void calculateStatistics() {
        PermissionStatistics ps = new PermissionStatistics(new File(folderToAnalyze), out);
        out.printf(IOutput.Level.NORMAL, ps.toString());
    }


    private static CommandLine parseCmdLine(String[] args) {
        final Options options = SetupOptions();
        CommandLine cmdline = null;
        boolean parsingError = false;
        try {
            cmdline = new GnuParser().parse(options, args);
        } catch (ParseException exp) {
            System.err.println(exp.getMessage());
            System.err.println();
            parsingError = true;
        }
        if (parsingError || cmdline.hasOption(OPTION_HELP)) {
            final HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.printHelp("rmperm", options, true);
            return null;
        }
        return cmdline;
    }


    private static Options SetupOptions() {
        Option rp = new Option(OPTION_REMOVE.substring(0, 1), "Remove");
        rp.setLongOpt(OPTION_REMOVE);
        Option ra = new Option("ra", "Remove only ads");
        ra.setLongOpt(OPTION_REMOVE_ONLY_ADS);
        Option l = new Option(OPTION_LIST.substring(0, 1), "List permissions");
        l.setLongOpt(OPTION_LIST);
        Option s = new Option(OPTION_STATISTICS.substring(0, 1), "Statistics of contained APKs");
        s.setArgs(1);
        s.setArgName("Folder-path");
        s.setLongOpt(OPTION_STATISTICS);
        OptionGroup g = new OptionGroup();
        g.addOption(rp)
                .addOption(ra)
                .addOption(l)
                .addOption(s)
                .setRequired(true);
        Option i = new Option(OPTION_INPUT.substring(0, 1), "Input APK file");
        i.setArgs(1);
        i.setArgName("APK-filename");
        i.setLongOpt(OPTION_INPUT);
        Option v = new Option(OPTION_VERBOSE.substring(0, 1), "Verbose output");
        v.setLongOpt(OPTION_VERBOSE);
        Option d = new Option(OPTION_DEBUG.substring(0, 1), "Debug output (implies -v)");
        d.setLongOpt(OPTION_DEBUG);
        Option h = new Option(OPTION_HELP.substring(0, 1), "Print this help");
        h.setLongOpt(OPTION_HELP);
        Option n = new Option(OPTION_NO_AUTO_REMOVE_VOID.substring(0, 1), "Disable the auto-removal of void methods");
        n.setLongOpt(OPTION_NO_AUTO_REMOVE_VOID);
        Option c = new Option(OPTION_CUSTOM_METHODS.substring(0, 1), "APK/Dex file with custom classes");
        c.setArgs(1);
        c.setArgName("APK/DEX-filename");
        c.setLongOpt(OPTION_CUSTOM_METHODS);
        Option o = new Option(OPTION_OUTPUT.substring(0, 1), "Output APK filename");
        o.setArgs(1);
        o.setArgName("APK-filename");
        o.setLongOpt(OPTION_OUTPUT);
        Option p = new Option(OPTION_PERMISSIONS.substring(0, 1), "Permissions to remove");
        p.setArgs(1);
        p.setArgName("CSV permission names");
        p.setLongOpt(OPTION_PERMISSIONS);
        Option a = new Option(OPTION_ADSREMOVAL.substring(0, 1), "Enable ADs removal");
        a.setArgs(0);
        a.setLongOpt(OPTION_ADSREMOVAL);

        final Options options = new Options();
        options.addOptionGroup(g)
                .addOption(h)
                .addOption(v)
                .addOption(d)
                .addOption(i)
                .addOption(c)
                .addOption(o)
                .addOption(p)
                .addOption(a);
        return options;
    }

}
