package it.saonzo.rmperm;

import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by simo on 17/11/16.
 */
public class RmPermissionsTest {
    private static final String dexWithCustomMethods = "/home/simo/IdeaProjects/BatchRmPerm/src/main/resources/custom.dex";
    Set<String> perms = new HashSet<>();
    String inputApk = "/home/simo/AndroidStudioProjects/MyApplication/app/build/outputs/apk/app-debug.apk";
    String outApk = "/home/simo/diocan.apk";

    @Before
    public void setUp() throws Exception {
        perms.add("android.permission.ACCESS_WIFI_STATE");
    }

    @Test
    public void testLoadCustomDex() throws Exception {

        RmPermissions rmPermissions = new RmPermissions(new ConsoleOutput(IOutput.Level.DEBUG), perms, inputApk, outApk, dexWithCustomMethods);
        rmPermissions.removePermissions();

    }

}