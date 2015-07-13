package it.rmperm.loader;

import it.rmperm.meth.AbstractDexMethod;
import it.rmperm.meth.DexPermMethod;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class AllMethodsLoader {

    private static final Pattern permPattern = Pattern.compile("^Permission:(android.permission.[A-Z_]*)$");
    private static final Pattern methodPattern = Pattern.compile("<(?<defClass>(?:\\w|\\.)+): (?<retType>(?:\\w|\\.)+) (?<name>(?:\\w|\\.)+)\\((?<params>[^)]*)\\)>.*");
    private Path permPath;
    private File permFile;

    public Hashtable<String, List<DexPermMethod>> permissionToMethods = new Hashtable<>();

    public AllMethodsLoader(String filePathPermissions) {
        Path permPath = Paths.get(filePathPermissions);
        //int i=0;
        try {
            BufferedReader reader = Files.newBufferedReader(permPath, Charset.defaultCharset());
            String line;
            String permission = "";
            while ( (line = reader.readLine()) != null ) {
                Matcher permMatcher = permPattern.matcher(line);
                if (permMatcher.matches()) {
                    permission = permMatcher.group(1);
                    if (!permissionToMethods.containsKey(permission))
                        permissionToMethods.put(permission, new ArrayList<>());
                }
                else {
                    Matcher methodMatcher = methodPattern.matcher(line);
                    if (methodMatcher.matches()) {
                        String definingClass = AbstractDexMethod.fromJavaTypeToDalvikType(methodMatcher.group("defClass"));
                        String returnType = AbstractDexMethod.fromJavaTypeToDalvikType(methodMatcher.group("retType"));
                        String name = methodMatcher.group("name");
                        List<String> params = AbstractDexMethod.parseAndConvertIntoDalvikTypes(methodMatcher.group("params"));
                        DexPermMethod dm = new DexPermMethod(definingClass, name, params, returnType, permission);
                        permissionToMethods.get(permission).add(dm);

                    }
                }
                //System.out.println(line);
            }
            System.out.println("PermissionDB loaded '" + filePathPermissions + "' without errors.");
        }catch (IOException e) {
            e.printStackTrace();
        }

    }

    public Hashtable<String, List<DexPermMethod>> getPermissionToMethods() {
        return permissionToMethods;
    }


    /**
     * Given a DexPermMethod (without its permission) search and return the same DexPermMethod with its permission.
     * @param dexPermMethod
     * @return the founded DexPermMethod , null otherwise
     */
    public DexPermMethod getContain(DexPermMethod dexPermMethod) {
        for (List<DexPermMethod> dml : permissionToMethods.values()) {
            for (DexPermMethod dm : dml) {
                if (dm.equals(dexPermMethod)) { // equals on DexPermMethod doesn't matter about permission info
                    return dm;
                }
            }
        }
        return null;
    }

}
