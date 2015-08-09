package it.unige.dibris.rmperm;

class Permissions {
    private static final String STD_PERMISSION_PREFIX = "android.permission.";

    public static String simplifyPermissionName(String name) {
        return name.startsWith(STD_PERMISSION_PREFIX) ? name.substring(STD_PERMISSION_PREFIX.length()) : name;
    }

    public static String fullPermissionName(String name) {
        return name.indexOf('.') == -1 ? STD_PERMISSION_PREFIX + name : name;
    }
}
