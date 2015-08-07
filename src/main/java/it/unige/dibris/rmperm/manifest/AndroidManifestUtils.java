package it.unige.dibris.rmperm.manifest;

import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class AndroidManifestUtils {
    public static final String STD_PERMISSION_PREFIX = "android.permission.";

    public static String simplifyPermissionName(String name) {
        return name.startsWith(STD_PERMISSION_PREFIX) ? name.substring(STD_PERMISSION_PREFIX.length()) : name;
    }

    public static void printPermissions(final String apkFilename) throws IOException {
        final AndroidManifest manifest = extractManifest(apkFilename);
        List<String> permissions = new ArrayList<>();
        for (String p : manifest.getPermissions())
            permissions.add(simplifyPermissionName(p));
        if (permissions.isEmpty()) {
            System.out.println("No permissions are requested in the manifest!");
            return;
        }
        System.out.printf("Permissions of %s:\n", apkFilename);
        for (String p : permissions)
            System.out.println(p);
        System.out.print("\nTo remove all of them you can pass rmperm the parameters:\n-r -s ");
        System.out.print(apkFilename);
        System.out.print(" -p ");
        System.out.println(String.join(",", permissions));
    }

    public static AndroidManifest extractManifest(final String apkFilename) throws IOException {
        final File tmpManifestFile = extractManifestToTemporaryFile(apkFilename);
        try {
            return new AndroidManifest(tmpManifestFile);
        } finally {
            tmpManifestFile.delete();
        }
    }

    private static File extractManifestToTemporaryFile(final String apkFilename) throws IOException {
        final File tmpManifestFile = File.createTempFile("AndroidManifest", null);
        tmpManifestFile.deleteOnExit();
        JarFile jf = new JarFile(apkFilename);
        OutputStream out = new FileOutputStream(tmpManifestFile);
        InputStream is = jf.getInputStream(jf.getEntry("AndroidManifest.xml"));
        copyAndClose(is, out);
        return tmpManifestFile;
    }

    private static void copyAndClose(InputStream is, OutputStream os) throws IOException {
        copy(is, os);
        is.close();
        os.close();
    }

    private static void copy(InputStream is, OutputStream os) throws IOException {
        final byte[] buf = new byte[512];
        int nRead;
        while ((nRead = is.read(buf)) != -1) {
            os.write(buf, 0, nRead);
        }
    }

    public static void removePermissions(String apkInFilename, String apkOutFilename, String... permissionsToRemove) throws IOException {
        AndroidManifest manifest = extractManifest(apkInFilename);
        for (String permission : permissionsToRemove) {
            boolean result = manifest.tryToRemovePermission(permission);
            if (!result)
                System.out.println("Couldn't remove " + permission);
        }
        final File tmpManifestFile = File.createTempFile("AndroidManifest", null);
        tmpManifestFile.deleteOnExit();
        try {
            manifest.write(tmpManifestFile);
            System.out.printf("Rewriting from %s to %s\n\n", apkInFilename, apkOutFilename);
            JarFile inputJar = new JarFile(apkInFilename);
            ZipOutputStream outputJar = new ZipOutputStream(new FileOutputStream(apkOutFilename));
            Enumeration<JarEntry> entries = inputJar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                final String name = entry.getName();
                if (name.equalsIgnoreCase("META-INF/MANIFEST.MF"))
                    continue;
                outputJar.putNextEntry(new ZipEntry(name));
                if (name.equalsIgnoreCase("AndroidManifest.xml")) {
                    FileInputStream newManifest = new FileInputStream(tmpManifestFile);
                    copy(newManifest, outputJar);
                    newManifest.close();
                } else {
                    final InputStream entryInputStream = inputJar.getInputStream(entry);
                    copy(entryInputStream, outputJar);
                    entryInputStream.close();
                }
                outputJar.closeEntry();
            }
            outputJar.close();
        } finally {
            tmpManifestFile.delete();
        }
    }
}
