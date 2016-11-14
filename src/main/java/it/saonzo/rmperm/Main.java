package it.saonzo.rmperm;

import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {
    private static final String OPTION_CUSTOM_METHODS = "custom-methods";
    private static final String OPTION_DEBUG = "debug";
    private static final String OPTION_HELP = "help";
    private static final String OPTION_INPUT = "input";
    private static final String OPTION_LIST = "list";
    private static final String OPTION_NO_AUTO_REMOVE_VOID = "no-auto-remove-void";
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
    private final String folderPermissionStatistics;
    private final boolean noAutoRemoveVoid;
    private final boolean adsRemoval;
    private IOutput out;


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
        folderPermissionStatistics = cmdLine.getOptionValue(OPTION_STATISTICS);
        checkIsFolder(folderPermissionStatistics);
    }

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
        Option remove = new Option(OPTION_REMOVE.substring(0, 1), "Remove");
        remove.setLongOpt(OPTION_REMOVE);
        Option removeOnlyAds = new Option("ra", "Remove only ads");
        removeOnlyAds.setLongOpt(OPTION_REMOVE_ONLY_ADS);
        Option list = new Option(OPTION_LIST.substring(0, 1), "List permissions");
        list.setLongOpt(OPTION_LIST);
        Option statistics = new Option(OPTION_STATISTICS.substring(0, 1), "Statistics of contained APKs");
        statistics.setArgs(1);
        statistics.setArgName("Folder-path");
        statistics.setLongOpt(OPTION_STATISTICS);
        OptionGroup g = new OptionGroup();
        g.addOption(remove)
                .addOption(removeOnlyAds)
                .addOption(list)
                .addOption(statistics)
                .setRequired(true);
        Option input = new Option(OPTION_INPUT.substring(0, 1), "Input APK file");
        input.setArgs(1);
        input.setArgName("APK-filename");
        input.setLongOpt(OPTION_INPUT);
        Option verbose = new Option(OPTION_VERBOSE.substring(0, 1), "Verbose output");
        verbose.setLongOpt(OPTION_VERBOSE);
        Option debug = new Option(OPTION_DEBUG.substring(0, 1), "Debug output (implies -v)");
        debug.setLongOpt(OPTION_DEBUG);
        Option help = new Option(OPTION_HELP.substring(0, 1), "Print this help");
        help.setLongOpt(OPTION_HELP);
        Option autoRemVoid = new Option("nav", "Disable the auto-removal of void methods");
        autoRemVoid.setLongOpt(OPTION_NO_AUTO_REMOVE_VOID);
        Option custom = new Option(OPTION_CUSTOM_METHODS.substring(0, 1), "APK/Dex file with custom classes");
        custom.setArgs(1);
        custom.setArgName("APK/DEX-filename");
        custom.setLongOpt(OPTION_CUSTOM_METHODS);
        Option output = new Option(OPTION_OUTPUT.substring(0, 1), "Output APK filename");
        output.setArgs(1);
        output.setArgName("APK-filename");
        output.setLongOpt(OPTION_OUTPUT);
        Option permissionsCsv = new Option(OPTION_PERMISSIONS.substring(0, 1), "Permissions to remove");
        permissionsCsv.setArgs(1);
        permissionsCsv.setArgName("CSV permission names");
        permissionsCsv.setLongOpt(OPTION_PERMISSIONS);
        Option adsRemoval = new Option(OPTION_ADSREMOVAL.substring(0, 1), "Enable ADs removal");
        adsRemoval.setArgs(0);
        adsRemoval.setLongOpt(OPTION_ADSREMOVAL);

        final Options options = new Options();
        options.addOptionGroup(g)
                .addOption(help)
                .addOption(verbose)
                .addOption(debug)
                .addOption(input)
                .addOption(custom)
                .addOption(output)
                .addOption(permissionsCsv)
                .addOption(adsRemoval)
                .addOption(autoRemVoid);
        return options;
    }

    private void setIOutput(IOutput iOutput) {
        out = iOutput;
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

    private void checkNonsenseOptions(final String currentOption, List<String> forbiddenOptions) throws BadCommandLineException {
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
            checkNonsenseOptions(OPTION_LIST, forbiddenOptions);
            listPermissions();
        } else if (cmdLine.hasOption(OPTION_REMOVE)) {
            forbiddenOptions.add(OPTION_LIST);
            forbiddenOptions.add(OPTION_REMOVE_ONLY_ADS);
            forbiddenOptions.add(OPTION_STATISTICS);
            checkNonsenseOptions(OPTION_REMOVE, forbiddenOptions);
            if (outApkFilename == null || customMethodsFilename == null || csvPermissionsToRemove == null) {
                out.printf(IOutput.Level.ERROR,
                        "Arguments --%s, --%s and --%s are required when using --%s\n",
                        OPTION_OUTPUT,
                        OPTION_CUSTOM_METHODS,
                        OPTION_PERMISSIONS,
                        OPTION_REMOVE);
                throw new BadCommandLineException();
            }
            RmPermissions rmPermissions = new RmPermissions(out, parseCsvPermissions(), inApkFilename, outApkFilename,
                    customMethodsFilename, noAutoRemoveVoid, adsRemoval);
            try {
                rmPermissions.removePermissions();
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
            checkNonsenseOptions(OPTION_REMOVE_ONLY_ADS, forbiddenOptions);
            if (outApkFilename == null) {
                out.printf(IOutput.Level.ERROR, "Argument --%s is required when using --%s\n", OPTION_OUTPUT, OPTION_REMOVE);
                throw new BadCommandLineException();
            }
            try {
                new RmAds(out, inApkFilename, outApkFilename).removeAds();
            } catch (Exception e) {
                out.printf(IOutput.Level.ERROR, "Error: %s\n", e.getMessage());
            }
        } else if (cmdLine.hasOption(OPTION_STATISTICS)) {
            PermissionStatistics ps = new PermissionStatistics(new File(folderPermissionStatistics), out);
            out.printf(IOutput.Level.NORMAL, ps.toString());
        }
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

}
