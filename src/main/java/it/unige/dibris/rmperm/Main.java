package it.unige.dibris.rmperm;

import it.unige.dibris.rmperm.loader.CustomMethodsLoader;
import it.unige.dibris.rmperm.loader.PermissionToMethodsParser;
import it.unige.dibris.rmperm.manifest.AndroidManifest;
import it.unige.dibris.rmperm.manifest.AndroidManifestUtils;
import org.apache.commons.cli.*;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.reference.MethodReference;

import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Main {
    private static final String OPTION_REMOVE = "remove";
    private static final String OPTION_LIST = "list";
    private static final String OPTION_SOURCE = "source";
    private static final String OPTION_VERBOSE = "verbose";
    private static final String OPTION_DEBUG = "debug";
    private static final String OPTION_CUSTOM_METHODS = "custom-methods";
    private static final String OPTION_OUTPUT = "output-apk";
    private static final String OPTION_PERMISSIONS = "permissions";
    private static final String OPTION_HELP = "help";
    private static final String OPTION_NOAUTOREMOVE_VOID = "no-autoremove";
    private final IOutput out;
    private final CommandLine cmdLine;
    private final String sourceApkFilename;
    private final String outApkFilename;
    private final String customMethodsFilename;
    private final String csvPermissionsToRemove;
    private final boolean noAutoRemoveVoid;

    private static class BadCommandLineException extends Exception {
    }

    private Main(String[] args) throws BadCommandLineException {
        cmdLine = parseCmdLine(args);
        if (cmdLine == null)
            throw new BadCommandLineException();
        out = createOutput();
        sourceApkFilename = cmdLine.getOptionValue(OPTION_SOURCE);
        outApkFilename = cmdLine.getOptionValue(OPTION_OUTPUT);
        customMethodsFilename = cmdLine.getOptionValue(OPTION_CUSTOM_METHODS);
        csvPermissionsToRemove = cmdLine.getOptionValue(OPTION_PERMISSIONS);
        noAutoRemoveVoid = cmdLine.hasOption(OPTION_NOAUTOREMOVE_VOID);
    }

    public static void main(String[] args) {
        try {
            new Main(args).main();
        } catch (BadCommandLineException e) {
            // NOP
        }
    }

    private IOutput createOutput() {
        IOutput.Level outputLevel = IOutput.Level.NORMAL;
        if (cmdLine.hasOption(OPTION_DEBUG))
            outputLevel = IOutput.Level.DEBUG;
        else if (cmdLine.hasOption(OPTION_VERBOSE))
            outputLevel = IOutput.Level.VERBOSE;
        return new ConsoleOutput(outputLevel);
    }

    private void main() {
        if (cmdLine.hasOption(OPTION_LIST))
            listPermissions();
        else {
            assert cmdLine.hasOption(OPTION_REMOVE);
            removePermissions();
        }
    }

    private void removePermissions() {
        if (outApkFilename == null || customMethodsFilename == null || csvPermissionsToRemove == null) {
            out.printf(IOutput.Level.ERROR,
                       "Arguments --%s, --%s and --%s are required when using --%s\n",
                       OPTION_OUTPUT,
                       OPTION_CUSTOM_METHODS,
                       OPTION_PERMISSIONS,
                       OPTION_REMOVE);
            return;
        }
        Set<String> permissionsToRemove = new HashSet<>();
        for (String p : csvPermissionsToRemove.split(","))
            permissionsToRemove.add(AndroidManifestUtils.fullPermissionName(p));
        out.printf(IOutput.Level.VERBOSE, "Removing permission(s): %s\n", permissionsToRemove);
        Map<MethodReference, MethodReference> redirections = new HashMap<>();
        List<ClassDef> customClasses = new ArrayList<>();
        try {
            new CustomMethodsLoader(out).load(customMethodsFilename, customClasses, redirections, permissionsToRemove);
        } catch (IOException e) {
            out.printf(IOutput.Level.ERROR, "Cannot load custom methods from %s\n", customMethodsFilename);
            return;
        }
        out.printf(IOutput.Level.VERBOSE, "Loaded %d redirections\n", redirections.size());
        final Map<MethodReference, Set<String>> apiToPermissions;
        try {
            apiToPermissions = new PermissionToMethodsParser(out).loadMapping(permissionsToRemove);
        } catch (IOException e) {
            out.printf(IOutput.Level.ERROR,
                       "This is weird: there is something wrong with my permission-to-API resource\n");
            return;
        }
        File tmpClassesDex = null;
        try {
            tmpClassesDex = File.createTempFile("NewClasseDex", null);
            tmpClassesDex.deleteOnExit();
            Customizer c = new Customizer(apiToPermissions,
                                          redirections,
                                          customClasses,
                                          new File(sourceApkFilename),
                                          tmpClassesDex,
                                          out,
                                          noAutoRemoveVoid);
            c.customize();
        } catch (IOException e) {
            out.printf(IOutput.Level.ERROR, "I/O error: %s\n", e.getMessage());
            return;
        }
        try {
            AndroidManifestUtils androidManifestUtils = new AndroidManifestUtils(out);
            AndroidManifest manifest = androidManifestUtils.extractManifest(sourceApkFilename);
            for (String permission : permissionsToRemove) {
                boolean result = manifest.tryToRemovePermission(permission);
                if (!result)
                    out.printf(IOutput.Level.ERROR, "Couldn't remove %s\n", permission);
            }
            final File tmpManifestFile = File.createTempFile("AndroidManifest", null);
            tmpManifestFile.deleteOnExit();
            try {
                manifest.write(tmpManifestFile);
                out.printf(IOutput.Level.NORMAL, "Rewriting from %s to %s\n\n", sourceApkFilename, outApkFilename);
                JarFile inputJar = new JarFile(sourceApkFilename);
                ZipOutputStream outputJar = new ZipOutputStream(new FileOutputStream(outApkFilename));
                Enumeration<JarEntry> entries = inputJar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    final String name = entry.getName();
                    if (name.equalsIgnoreCase("META-INF/MANIFEST.MF"))
                        continue;
                    outputJar.putNextEntry(new ZipEntry(name));
                    if (name.equalsIgnoreCase("AndroidManifest.xml")) {
                        FileInputStream newManifest = new FileInputStream(tmpManifestFile);
                        androidManifestUtils.copy(newManifest, outputJar);
                        newManifest.close();
                    } else if (name.equalsIgnoreCase("classes.dex")) {
                        FileInputStream newClasses = new FileInputStream(tmpClassesDex);
                        androidManifestUtils.copy(newClasses, outputJar);
                        newClasses.close();
                    } else {
                        final InputStream entryInputStream = inputJar.getInputStream(entry);
                        androidManifestUtils.copy(entryInputStream, outputJar);
                        entryInputStream.close();
                    }
                    outputJar.closeEntry();
                }
                outputJar.close();
            } finally {
                tmpManifestFile.delete();
                tmpClassesDex.delete();
            }
        } catch (IOException e) {
            out.printf(IOutput.Level.ERROR, "I/O error: %s\n", e.getMessage());
        }
        // TODO sign-and-align the APK
    }

    private void listPermissions() {
        final String errorMsg = "Error: option --%s makes no sense when --%s is specified\n";
        final String[] nonsensicalOptions = {OPTION_OUTPUT, OPTION_CUSTOM_METHODS, OPTION_PERMISSIONS};
        boolean error = false;
        for (String nonsensicalOption : nonsensicalOptions)
            if (cmdLine.getOptionValue(nonsensicalOption) != null) {
                out.printf(IOutput.Level.ERROR, errorMsg, nonsensicalOption, OPTION_LIST);
                error = true;
            }
        if (!error)
            try {
                new AndroidManifestUtils(out).printPermissions(sourceApkFilename);
            } catch (IOException e) {
                out.printf(IOutput.Level.ERROR, "I/O error: %s\n", e.getMessage());
            }
    }

    private static CommandLine parseCmdLine(String[] args) {
        Option r = new Option(OPTION_REMOVE.substring(0, 1), "Remove permissions");
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
        Option n = new Option(OPTION_NOAUTOREMOVE_VOID.substring(0, 1), "Disable the auto-removal of void methods");
        n.setLongOpt(OPTION_NOAUTOREMOVE_VOID);
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

}
