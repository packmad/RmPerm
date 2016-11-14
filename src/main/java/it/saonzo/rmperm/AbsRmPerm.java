package it.saonzo.rmperm;


import kellinwood.security.zipsigner.ZipSigner;

import java.io.*;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public abstract class AbsRmPerm {
    protected IOutput out;
    String inApkFilename;
    String outApkFilename;


    public AbsRmPerm(IOutput out, String inApkFilename, String outApkFilename) {
        this.out = out;
        this.inApkFilename = inApkFilename;
        this.outApkFilename = outApkFilename;
    }


    void signApk(File tmpApkFile) throws Exception {
        ZipSigner zipSigner = new ZipSigner();
        zipSigner.setKeymode("auto-testkey");
        String inputFilename = tmpApkFile.getCanonicalPath();
        out.printf(IOutput.Level.VERBOSE, "Signing %s into %s\n", inputFilename, outApkFilename);
        zipSigner.signZip(inputFilename, outApkFilename);
    }


    void writeApk(File tmpClassesDex, File tmpManifestFile, File outApkFile) throws IOException {
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

}
