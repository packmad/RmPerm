package it.saonzo.rmperm;


import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.reference.MethodReference;

import java.io.File;
import java.io.IOException;
import java.util.*;


public class RmPermissions extends AbsRmPerm {

    private Set<String> permissionsToRemove;

    private String customMethodsFilename;
    private boolean noAutoRemoveVoid;
    private boolean removeAds;


    public RmPermissions(IOutput out, Set<String> permissionsToRemove, String inApkFilename, String outApkFilename,
                         String customMethodsFilename) throws IOException {
        this(out, permissionsToRemove, inApkFilename, outApkFilename, customMethodsFilename, false, false);
        //TODO load custom.dex from resources
        /*
        ClassLoader classLoader = getClass().getClassLoader();
        URL customMethodRes = classLoader.getResource("custom.dex");
        if (customMethodRes == null)
            throw new IllegalStateException("Cannot get the URL to local dex with custom methods!");
        File customMethodsFile = File.createTempFile("custom", ".dex");
        FileUtils.copyURLToFile(customMethodRes, customMethodsFile);
        if (!customMethodsFile.exists())
            throw new IllegalStateException("Cannot read the local dex file with custom methods!");
        customMethodsFilename = customMethodsFile.toString();
        */
    }

    public RmPermissions(IOutput out, Set<String> permissionsToRemove, String inApkFilename, String outApkFilename,
                         String customMethodsFilename, boolean noAutoRemoveVoid, boolean removeAds) {
        super(out, inApkFilename, outApkFilename);
        this.permissionsToRemove = permissionsToRemove;
        this.customMethodsFilename = customMethodsFilename;
        this.noAutoRemoveVoid = noAutoRemoveVoid;
        this.removeAds = removeAds;
    }

    public void removePermissions() throws Exception {
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
                removeAds
        );
        c.customize();
        return tmpClassesDex;
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


}
