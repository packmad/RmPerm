package it.unige.dibris.rmperm.manifest;

import it.unige.dibris.rmperm.IOutput;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

public class AndroidManifestUtils {
    private final IOutput out;

    public AndroidManifestUtils(IOutput out) {
        this.out = out;
    }

    public static final String STD_PERMISSION_PREFIX = "android.permission.";

    public static String simplifyPermissionName(String name) {
        return name.startsWith(STD_PERMISSION_PREFIX) ? name.substring(STD_PERMISSION_PREFIX.length()) : name;
    }

    public static String fullPermissionName(String name) {
        return name.indexOf('.') == -1 ? STD_PERMISSION_PREFIX + name : name;
    }

    public void printPermissions(final String apkFilename) throws IOException {
        final AndroidManifest manifest = extractManifest(apkFilename);
        List<String> permissions = new ArrayList<>();
        for (String p : manifest.getPermissions())
            permissions.add(simplifyPermissionName(p));
        if (permissions.isEmpty()) {
            out.printf(IOutput.Level.NORMAL, "No permissions are requested in the manifest!\n");
            return;
        }
        out.printf(IOutput.Level.NORMAL, "Permissions of %s:\n", apkFilename);
        for (String p : permissions)
            out.printf(IOutput.Level.NORMAL, "%s\n", p);
        out.printf(IOutput.Level.NORMAL,
                   "\nTo remove all of them you can pass rmperm the parameters:\n-r -s %s -p %s\n",
                   apkFilename,
                   String.join(",", permissions));
    }

    public AndroidManifest extractManifest(final String apkFilename) throws IOException {
        final File tmpManifestFile = extractManifestToTemporaryFile(apkFilename);
        try {
            return new AndroidManifest(tmpManifestFile);
        } finally {
            tmpManifestFile.delete();
        }
    }

    File extractManifestToTemporaryFile(final String apkFilename) throws IOException {
        final File tmpManifestFile = File.createTempFile("AndroidManifest", null);
        tmpManifestFile.deleteOnExit();
        JarFile jf = new JarFile(apkFilename);
        OutputStream out = new FileOutputStream(tmpManifestFile);
        InputStream is = jf.getInputStream(jf.getEntry("AndroidManifest.xml"));
        copyAndClose(is, out);
        return tmpManifestFile;
    }

    void copyAndClose(InputStream is, OutputStream os) throws IOException {
        copy(is, os);
        is.close();
        os.close();
    }

    public void copy(InputStream is, OutputStream os) throws IOException {
        final byte[] buf = new byte[512];
        int nRead;
        while ((nRead = is.read(buf)) != -1) {
            os.write(buf, 0, nRead);
        }
    }

}
