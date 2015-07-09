package it.rmperm;

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


public class PermissionLoader {

    private static final Pattern permPattern = Pattern.compile("^Permission:(android.permission.[A-Z_]*)$");
    private static final Pattern methodPattern = Pattern.compile("<(?<defClass>(?:\\w|\\.)+): (?<retType>(?:\\w|\\.)+) (?<name>(?:\\w|\\.)+)\\((?<params>[^)]*)\\)>.*");
    private Path permPath;
    private File permFile;

    public Hashtable<String, List<DexMethod>> permissionToMethods = new Hashtable<>();

    public PermissionLoader(String filePathPermissions) {
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
                        permissionToMethods.put(permission, new ArrayList<DexMethod>());
                }
                else {
                    Matcher methodMatcher = methodPattern.matcher(line);
                    if (methodMatcher.matches()) {
                        String definingClass = fromJavaTypeToDalvikType(methodMatcher.group("defClass"));
                        String returnType = fromJavaTypeToDalvikType(methodMatcher.group("retType"));
                        String name = methodMatcher.group("name");
                        List<String> params = parseAndConvertIntoDalvikTypes(methodMatcher.group("params"));
                        DexMethod dm = new DexMethod(permission, definingClass, name, params, returnType);
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

    /**
     * Given a DexMethod (without its permission) search and return the same DexMethod with its permission.
     * @param dexMethod
     * @return the founded DexMethod , null otherwise
     */
    public DexMethod getContain(DexMethod dexMethod) {
        for (List<DexMethod> dml : permissionToMethods.values()) {
            for (DexMethod dm : dml) {
                if (dm.equals(dexMethod)) { // equals on DexMethod doesn't matter about permission info
                    return dm;
                }
            }
        }
        return null;
    }

    public static List<String> parseAndConvertIntoDalvikTypes(String csvParams) {
        List<String> list = new ArrayList<String>();
        String[] params = csvParams.split(",");
        for (String s : params) {
            if (!s.equals(""))
                list.add(fromJavaTypeToDalvikType(s));
        }
        return list;
    }

    public static String fromJavaTypeToDalvikType(String jType) {
        switch (jType) {
            case "void":
                return "V";
            case "boolean":
                return "Z";
            case "byte":
                return "B";
            case "short":
                return "S";
            case "char":
                return "C";
            case "int":
                return "I";
            case "long":
                return "J";
            case "float":
                return "F";
            case "double":
                return "D";
            default:
                return "L" + jType.replace(".", "/") + ";";
        }
    }
}
