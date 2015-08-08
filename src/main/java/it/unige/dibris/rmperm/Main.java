package it.unige.dibris.rmperm;

import it.unige.dibris.rmperm.loader.CustomMethodsLoader;
import it.unige.dibris.rmperm.loader.PermissionToMethodsParser;
import it.unige.dibris.rmperm.manifest.AndroidManifestUtils;
import org.apache.commons.cli.*;
import org.jf.dexlib2.iface.ClassDef;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Exchanger;

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
    private final IOutput out;
    private final CommandLine cmdLine;
    private final String sourceApkFilename;
    private final String outApkFilename;
    private final String customMethodsFilename;
    private final String csvPermissionsToRemove;

    private static class BadCommandLineException extends Exception {}

    private Main(String[] args) throws BadCommandLineException {
        cmdLine = parseCmdLine(args);
        if (cmdLine==null)
            throw new BadCommandLineException();
        out = createOutput();
        sourceApkFilename = cmdLine.getOptionValue(OPTION_SOURCE);
        outApkFilename = cmdLine.getOptionValue(OPTION_OUTPUT);
        customMethodsFilename = cmdLine.getOptionValue(OPTION_CUSTOM_METHODS);
        csvPermissionsToRemove = cmdLine.getOptionValue(OPTION_PERMISSIONS);
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
            out.printf(IOutput.Level.ERROR, "Arguments --%s, --%s and --%s are required when using --%s\n", OPTION_OUTPUT, OPTION_CUSTOM_METHODS, OPTION_PERMISSIONS, OPTION_REMOVE);
            return;
        }
        Set<String> permissionToRemove = new HashSet<>();
        for (String p : csvPermissionsToRemove.split(","))
            permissionToRemove.add(AndroidManifestUtils.fullPermissionName(p));
        out.printf(IOutput.Level.VERBOSE, "Removing permission(s): %s\n", permissionToRemove);
        Map<String, Set<MethodRedirection>> methodRedirections = new HashMap<>();
        List<ClassDef> customClasses = new ArrayList<>();
        try {
            new CustomMethodsLoader(out).load(customMethodsFilename, customClasses, methodRedirections, permissionToRemove);
        } catch (IOException e) {
            out.printf(IOutput.Level.ERROR, "Cannot load custom methods from %s\n", customMethodsFilename);
            return;
        }
        int totRedirections = 0;
        for(Set<MethodRedirection> redirections : methodRedirections.values())
            totRedirections+=redirections.size();
        out.printf(IOutput.Level.VERBOSE, "Loaded %d redirections, for %d permissions\n", totRedirections, methodRedirections.size());
        final Hashtable<String, List<DexMethod>> permissionToMethods;
        try {
            permissionToMethods = PermissionToMethodsParser.loadMapping(out);
        } catch (IOException e) {
            out.printf(IOutput.Level.ERROR, "This is weird: there is something wrong with my permission-to-API resource\n");
            return;
        }
        //Customizer c = new Customizer(permissionToMethods, methodRedirections, customClasses, permissionToRemove, sourceApkFilename, outApkFilename, out);
        out.printf(IOutput.Level.ERROR, "Not implemented (yet)!\n");
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
        Option n = new Option("n", "Disable the auto removal of void methods");
        n.setLongOpt("no-auto-remove-void");
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
