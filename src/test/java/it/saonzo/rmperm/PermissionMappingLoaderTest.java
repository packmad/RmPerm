package it.saonzo.rmperm;

import org.jf.dexlib2.iface.reference.MethodReference;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class PermissionMappingLoaderTest {
    private PermissionMappingLoader permissionMappingLoader;

    @Test
    public void loadMappingTest01() throws Exception {
        permissionMappingLoader = new PermissionMappingLoader(new ConsoleOutput(IOutput.Level.DEBUG));
        Set<String> allPerms = new HashSet<>(60);
        allPerms.add("android.permission.ACCESS_WIFI_STATE");
        allPerms.add("android.permission.EXPAND_STATUS_BAR");
        allPerms.add("android.permission.CHANGE_WIFI_MULTICAST_STATE");
        allPerms.add("android.permission.USE_CREDENTIALS");
        allPerms.add("com.android.browser.permission.READ_HISTORY_BOOKMARKS");
        allPerms.add("android.permission.RECEIVE_SMS");
        allPerms.add("android.permission.NFC");
        allPerms.add("com.android.voicemail.permission.ADD_VOICEMAIL");
        allPerms.add("android.permission.CLEAR_APP_CACHE");
        allPerms.add("android.permission.BLUETOOTH_ADMIN");
        allPerms.add("android.permission.ACCESS_MOCK_LOCATION");
        allPerms.add("android.permission.WRITE_SMS");
        allPerms.add("android.permission.WRITE_CALENDAR");
        allPerms.add("android.permission.ACCESS_FINE_LOCATION");
        allPerms.add("android.permission.CAMERA");
        allPerms.add("android.permission.READ_SYNC_STATS");
        allPerms.add("android.permission.SYSTEM_ALERT_WINDOW");
        allPerms.add("android.permission.READ_PHONE_STATE");
        allPerms.add("android.permission.WRITE_USER_DICTIONARY");
        allPerms.add("android.permission.TRANSMIT_IR");
        allPerms.add("android.permission.RECEIVE_MMS");
        allPerms.add("android.permission.MODIFY_AUDIO_SETTINGS");
        allPerms.add("android.permission.MANAGE_ACCOUNTS");
        allPerms.add("android.permission.WRITE_CONTACTS");
        allPerms.add("android.permission.RECORD_AUDIO");
        allPerms.add("android.permission.WRITE_SETTINGS");
        allPerms.add("android.permission.VIBRATE");
        allPerms.add("android.permission.ACCESS_NETWORK_STATE");
        allPerms.add("android.permission.BROADCAST_STICKY");
        allPerms.add("android.permission.SET_TIME_ZONE");
        allPerms.add("android.permission.RESTART_PACKAGES");
        allPerms.add("android.permission.CHANGE_NETWORK_STATE");
        allPerms.add("android.permission.READ_SMS");
        allPerms.add("android.permission.REORDER_TASKS");
        allPerms.add("android.permission.FLASHLIGHT");
        allPerms.add("android.permission.ACCESS_COARSE_LOCATION");
        allPerms.add("android.permission.ACCESS_LOCATION_EXTRA_COMMANDS");
        allPerms.add("android.permission.RECEIVE_WAP_PUSH");
        allPerms.add("android.permission.WRITE_SYNC_SETTINGS");
        allPerms.add("android.permission.AUTHENTICATE_ACCOUNTS");
        allPerms.add("com.android.browser.permission.WRITE_HISTORY_BOOKMARKS");
        allPerms.add("android.permission.READ_CALL_LOG");
        allPerms.add("android.permission.READ_CALENDAR");
        allPerms.add("android.permission.READ_PROFILE");
        allPerms.add("android.permission.WAKE_LOCK");
        allPerms.add("android.permission.SET_WALLPAPER_HINTS");
        allPerms.add("android.permission.GET_PACKAGE_SIZE");
        allPerms.add("android.permission.GET_ACCOUNTS");
        allPerms.add("android.permission.READ_CONTACTS");
        allPerms.add("android.permission.DISABLE_KEYGUARD");
        allPerms.add("android.permission.CHANGE_WIFI_STATE");
        allPerms.add("android.permission.BLUETOOTH");
        allPerms.add("android.permission.INTERNET");
        allPerms.add("android.permission.KILL_BACKGROUND_PROCESSES");
        allPerms.add("android.permission.RECEIVE_BOOT_COMPLETED");
        allPerms.add("android.permission.USE_SIP");
        allPerms.add("android.permission.GET_TASKS");
        allPerms.add("android.permission.SEND_SMS");
        allPerms.add("android.permission.SET_WALLPAPER");
        allPerms.add("android.permission.READ_SYNC_SETTINGS");
        Map<MethodReference, Set<String>> result = permissionMappingLoader.loadMapping(allPerms);
        Assert.assertEquals(result.keySet().size(), 11189);
    }

    @Test
    public void loadMappingTest02() throws Exception {
        permissionMappingLoader = new PermissionMappingLoader(new ConsoleOutput(IOutput.Level.DEBUG));
        Set<String> wifistate = new HashSet<>(1);
        wifistate.add("android.permission.ACCESS_WIFI_STATE");
        Map<MethodReference, Set<String>> result = permissionMappingLoader.loadMapping(wifistate);
        Assert.assertEquals(result.keySet().size(), 275);
    }
}