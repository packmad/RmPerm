package it.unige.dibris.rmperm;

import it.unige.dibris.rmperm.loader.CustomMethodsLoader;
import it.unige.dibris.rmperm.loader.PermissionToMethodsParser;
import it.unige.dibris.rmperm.manifest.AndroidManifestUtils;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    private static final String OPTION_REMOVE = "remove";
    private static final String OPTION_LIST = "list";
    private static final String OPTION_SOURCE = "source";
    private static final String OPTION_VERBOSE = "verbose";
    private static final String OPTION_CUSTOM_METHODS = "custom-methods";
    private static final String OPTION_OUTPUT = "output-apk";
    private static final String OPTION_PERMISSIONS = "permissions";
    private static final String OPTION_HELP = "help";

    public static void main(String[] args) {
        CommandLine cmdLine = parseCmdLine(args);
        if (cmdLine == null)
            return;

        String sourceApkFilename = cmdLine.getOptionValue(OPTION_SOURCE);
        boolean verboseOutput = cmdLine.hasOption(OPTION_VERBOSE);
        if (cmdLine.hasOption(OPTION_LIST)) {
            listPermissions(sourceApkFilename, verboseOutput);
            return;
        }
        assert cmdLine.hasOption(OPTION_REMOVE);
        removePermissions(sourceApkFilename, verboseOutput, cmdLine);
    }

    private static void removePermissions(String sourceApkFilename, boolean verboseOutput, CommandLine cmdLine) {
        String outApkFilename = cmdLine.getOptionValue(OPTION_OUTPUT);
        String customMethodsFilename = cmdLine.getOptionValue(OPTION_CUSTOM_METHODS);
        String csvPermissionsToRemove = cmdLine.getOptionValue(OPTION_PERMISSIONS);
        if (outApkFilename==null || customMethodsFilename==null || csvPermissionsToRemove==null) {
            System.err.println("Arguments "+OPTION_OUTPUT+", "+OPTION_CUSTOM_METHODS+", "+OPTION_PERMISSIONS+
                " are required when using "+OPTION_REMOVE);
            return;
        }
        System.out.println("sourceApkFilename="+sourceApkFilename);
        System.out.println("verboseOutput="+verboseOutput);
        System.out.println("outApkFilename="+outApkFilename);
        System.out.println("customMethodsFilename="+customMethodsFilename);
        System.out.println("csvPermissionsToRemove="+csvPermissionsToRemove);
        Map<String, Set<MethodRedirection>> methodRedirections = new HashMap<>();
        try {
            CustomMethodsLoader.load(customMethodsFilename, methodRedirections);
        } catch (IOException e) {
            System.err.println("Cannot load custom methods from "+customMethodsFilename);
            return;
        }
        final Hashtable<String, List<DexMethod>> permissionToMethods;
        try {
            permissionToMethods = PermissionToMethodsParser.loadMapping();
        } catch (IOException e) {
            System.err.println("This is weird: there is something wrong with my permission-to-API resource");
            return;
        }
        System.out.println("Not implemented (yet)!");
    }

    private static void listPermissions(String sourceApkFilename, boolean verboseOutput) {
        try {
            AndroidManifestUtils.printPermissions(sourceApkFilename);
        } catch (IOException e) {
            System.out.println("I/O error: " + e.getMessage());
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
