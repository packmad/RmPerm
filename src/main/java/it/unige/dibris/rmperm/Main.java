package it.unige.dibris.rmperm;

import kellinwood.security.zipsigner.ZipSigner;
import org.apache.commons.cli.*;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.reference.MethodReference;

import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class Main {
    private static final String OPTION_REMOVE = "remove";
    private static final String OPTION_LIST = "list";
    private static final String OPTION_SOURCE = "source";
    private static final String OPTION_VERBOSE = "verbose";
    private static final String OPTION_DEBUG = "debug";
    private static final String OPTION_CUSTOM_METHODS = "custom-methods";
    private static final String OPTION_OUTPUT = "output-apk";
    private static final String OPTION_PERMISSIONS = "permissions";
    private static final String OPTION_HELP = "help";
    private static final String OPTION_NO_AUTO_REMOVE_VOID = "no-auto-remove";
    private final IOutput out;
    private final CommandLine cmdLine;
    private final String sourceApkFilename;
    private final String outApkFilename;
    private final String customMethodsFilename;
    private final String csvPermissionsToRemove;
    private final boolean noAutoRemoveVoid;

    public static void main(String[] args) {
        try {
            new Main(args).main();
        } catch (BadCommandLineException e) {
            // NOP
        }
    }

    private static class BadCommandLineException extends Exception {}

    private Main(String[] args) throws BadCommandLineException {
        cmdLine = parseCmdLine(args);
        if (cmdLine == null)
            throw new BadCommandLineException();
        IOutput.Level outputLevel = IOutput.Level.NORMAL;
        if (cmdLine.hasOption(OPTION_DEBUG))
            outputLevel = IOutput.Level.DEBUG;
        else if (cmdLine.hasOption(OPTION_VERBOSE))
            outputLevel = IOutput.Level.VERBOSE;
        out = new ConsoleOutput(outputLevel);
        sourceApkFilename = cmdLine.getOptionValue(OPTION_SOURCE);
        outApkFilename = cmdLine.getOptionValue(OPTION_OUTPUT);
        customMethodsFilename = cmdLine.getOptionValue(OPTION_CUSTOM_METHODS);
        csvPermissionsToRemove = cmdLine.getOptionValue(OPTION_PERMISSIONS);
        noAutoRemoveVoid = cmdLine.hasOption(OPTION_NO_AUTO_REMOVE_VOID);
    }

    private void main() {
        if (cmdLine.hasOption(OPTION_LIST)) {
            final String[] nonsensicalOptions = {OPTION_OUTPUT, OPTION_CUSTOM_METHODS, OPTION_PERMISSIONS};
            final String errorMsg = "Error: option --%s makes no sense when --%s is specified\n";
            boolean thereAreNonsensicalOptions = false;
            for (String nonsensicalOption : nonsensicalOptions)
                if (cmdLine.getOptionValue(nonsensicalOption) != null) {
                    out.printf(IOutput.Level.ERROR, errorMsg, nonsensicalOption, OPTION_LIST);
                    thereAreNonsensicalOptions = true;
                }
            if (thereAreNonsensicalOptions)
                return;
            listPermissions();
        } else {
            assert cmdLine.hasOption(OPTION_REMOVE);
            if (outApkFilename == null || customMethodsFilename == null || csvPermissionsToRemove == null) {
                out.printf(IOutput.Level.ERROR,
                           "Arguments --%s, --%s and --%s are required when using --%s\n",
                           OPTION_OUTPUT,
                           OPTION_CUSTOM_METHODS,
                           OPTION_PERMISSIONS,
                           OPTION_REMOVE);
                return;
            }
            try {
                removePermissions();
            } catch (IOException e) {
                out.printf(IOutput.Level.ERROR, "I/O error: %s\n", e.getMessage());
            } catch (Exception e) {
                out.printf(IOutput.Level.ERROR, "Error: %s\n", e.getMessage());
            }
        }
    }

    private void removePermissions() throws Exception {
        Set<String> permissionsToRemove = parseCsvPermissions();
        out.printf(IOutput.Level.VERBOSE, "Removing permission(s): %s\n", permissionsToRemove);
        Map<MethodReference, MethodReference> redirections = new HashMap<>();
        List<ClassDef> customClasses = new ArrayList<>();
        new CustomMethodsLoader(out).load(customMethodsFilename, customClasses, redirections, permissionsToRemove);
        out.printf(IOutput.Level.VERBOSE, "Loaded %d redirections\n", redirections.size());
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
        out.printf(IOutput.Level.VERBOSE, "Customizing %s into %s\n", sourceApkFilename, outApkFile);
        JarFile inputJar = new JarFile(sourceApkFilename);
        ZipOutputStream outputJar = new ZipOutputStream(new FileOutputStream(outApkFile));
        Enumeration<JarEntry> entries = inputJar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            final String name = entry.getName();
            outputJar.putNextEntry(new ZipEntry(name));
            if (name.equalsIgnoreCase("AndroidManifest.xml")) {
                FileInputStream newManifest = new FileInputStream(tmpManifestFile);
                StreamUtils.copy(newManifest, outputJar);
                newManifest.close();
            } else if (name.equalsIgnoreCase("classes.dex")) {
                FileInputStream newClasses = new FileInputStream(tmpClassesDex);
                StreamUtils.copy(newClasses, outputJar);
                newClasses.close();
            } else {
                final InputStream entryInputStream = inputJar.getInputStream(entry);
                StreamUtils.copy(entryInputStream, outputJar);
                entryInputStream.close();
            }
            outputJar.closeEntry();
        }
        outputJar.close();
    }

    private File stripPermissionsFromManifest(Set<String> permissionsToRemove) throws IOException {
        AndroidManifest manifest = AndroidManifest.extractManifest(sourceApkFilename);
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
        File tmpClassesDex;
        tmpClassesDex = File.createTempFile("NewClasseDex", null);
        tmpClassesDex.deleteOnExit();
        BytecodeCustomizer c = new BytecodeCustomizer(apiToPermissions,
                                                      redirections,
                                                      customClasses,
                                                      new File(sourceApkFilename),
                                                      tmpClassesDex,
                                                      out,
                                                      noAutoRemoveVoid);
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
            manifest = AndroidManifest.extractManifest(sourceApkFilename);
        } catch (IOException e) {
            out.printf(IOutput.Level.ERROR, "Cannot read %s: %s\n", sourceApkFilename, e.getMessage());
            return;
        }
        List<String> permissions = new ArrayList<>();
        for (String p : manifest.getPermissions())
            permissions.add(Permissions.simplifyPermissionName(p));
        if (permissions.isEmpty()) {
            out.printf(IOutput.Level.NORMAL, "No permissions are requested in the manifest!\n");
            return;
        }
        out.printf(IOutput.Level.NORMAL, "Permissions of %s:\n", sourceApkFilename);
        for (String p : permissions)
            out.printf(IOutput.Level.NORMAL, "%s\n", p);
        out.printf(IOutput.Level.NORMAL,
                   "\nTo remove all of them you can pass rmperm the parameters:\n-r -s %s -p %s\n",
                   sourceApkFilename,
                   String.join(",", permissions));

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

    private static Options SetupOptions() {Option r = new Option(OPTION_REMOVE.substring(0, 1), "Remove permissions");
        r.setLongOpt(OPTION_REMOVE);
        Option l = new Option(OPTION_LIST.substring(0, 1), "List permissions");
        l.setLongOpt(OPTION_LIST);
        OptionGroup g = new OptionGroup();
        g.addOption(r)
         .addOption(l)
         .setRequired(true);
        Option s = new Option(OPTION_SOURCE.substring(0, 1), "Specifies the source APK file");
        s.setArgs(1);
        s.setArgName("APK-filename");
        s.setLongOpt(OPTION_SOURCE);
        s.setRequired(true);
        Option v = new Option(OPTION_VERBOSE.substring(0, 1), "Verbose output");
        v.setLongOpt(OPTION_VERBOSE);
        Option d = new Option(OPTION_DEBUG.substring(0, 1), "Debug output (implies -v)");
        d.setLongOpt(OPTION_DEBUG);
        Option h = new Option(OPTION_HELP.substring(0, 1), "Print this help");
        h.setLongOpt(OPTION_HELP);
        Option n = new Option(OPTION_NO_AUTO_REMOVE_VOID.substring(0, 1), "Disable the auto-removal of void methods");
        n.setLongOpt(OPTION_NO_AUTO_REMOVE_VOID);
        Option c = new Option(OPTION_CUSTOM_METHODS.substring(0, 1), "Source APK filename");
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
        final Options options = new Options();
        options.addOptionGroup(g)
               .addOption(h)
               .addOption(v)
               .addOption(d)
               .addOption(s)
               .addOption(c)
               .addOption(o)
               .addOption(p);
        return options;
    }

}
