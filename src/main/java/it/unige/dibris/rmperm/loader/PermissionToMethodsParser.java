package it.unige.dibris.rmperm.loader;

import it.unige.dibris.rmperm.DexMethod;
import it.unige.dibris.rmperm.IOutput;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PermissionToMethodsParser {

    private static final Pattern permPattern = Pattern.compile("^Permission:(android.permission.[A-Z_]*)$");
    private static final Pattern methodPattern = Pattern.compile("<(?<defClass>(?:\\w|\\.)+): (?<retType>(?:\\w|\\.)+) (?<name>(?:\\w|\\.)+)\\((?<params>[^)]*)\\)>.*");

    public static Hashtable<String, List<DexMethod>> loadMapping(IOutput out) throws IOException {
        final Hashtable<String, List<DexMethod>> permissionToMethods = new Hashtable<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(PermissionToMethodsParser.class.getResourceAsStream("/jellybean_publishedapimapping")));
        String line;
        String permission = null;
        while ((line = reader.readLine()) != null) {
            Matcher permMatcher = permPattern.matcher(line);
            if (permMatcher.matches()) {
                permission = permMatcher.group(1);
                if (!permissionToMethods.containsKey(permission))
                    permissionToMethods.put(permission, new ArrayList<>());
            } else {
                Matcher methodMatcher = methodPattern.matcher(line);
                if (methodMatcher.matches()) {
                    String definingClass = DexMethod.fromJavaTypeToDalvikType(methodMatcher.group("defClass"));
                    String returnType = DexMethod.fromJavaTypeToDalvikType(methodMatcher.group("retType"));
                    String name = methodMatcher.group("name");
                    List<String> params = DexMethod.parseAndConvertIntoDalvikTypes(methodMatcher.group("params"));
                    DexMethod dm = new DexMethod(definingClass, name, params, returnType);
                    assert permission!=null;
                    permissionToMethods.get(permission).add(dm);
                }
            }
        }
        out.printf(IOutput.Level.DEBUG, String.format("Loaded %s API mappings for %d permissions\n", permissionToMethods.size(), permissionToMethods.keySet().size()));
        return permissionToMethods;
    }

}
