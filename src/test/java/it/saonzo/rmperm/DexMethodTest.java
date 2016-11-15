package it.saonzo.rmperm;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;


public class DexMethodTest {

    @Test
    public void test01Constructor() throws Exception {
        String test01 = "Landroid/app/backup/BackupAgent$BackupServiceBinder;-doRestore-(Landroid/os/ParcelFileDescriptor; I Landroid/os/ParcelFileDescriptor; I Landroid/app/backup/IBackupManager;)V";
        DexMethod dexMethod = new DexMethod(test01);
        Assert.assertEquals("Landroid/app/backup/BackupAgent$BackupServiceBinder;", dexMethod.getDefiningClass());
        Assert.assertEquals("doRestore", dexMethod.getName());
        Assert.assertEquals("V", dexMethod.getReturnType());
        String[] expected = {"Landroid/os/ParcelFileDescriptor;", "I", "Landroid/os/ParcelFileDescriptor;", "I", "Landroid/app/backup/IBackupManager;"};
        Assert.assertEquals(Arrays.asList(expected), dexMethod.getParameterTypes());
    }

    @Test
    public void test02Constructor() throws Exception {
        String test02 = "Lcom/android/server/backup/BackupManagerService$PerformBackupTask;-handleTimeout-()V";
        DexMethod dexMethod = new DexMethod(test02);
        Assert.assertEquals("Lcom/android/server/backup/BackupManagerService$PerformBackupTask;", dexMethod.getDefiningClass());
        Assert.assertEquals("handleTimeout", dexMethod.getName());
        Assert.assertEquals("V", dexMethod.getReturnType());
        String[] expected = {};
        Assert.assertEquals(Arrays.asList(expected), dexMethod.getParameterTypes());
    }

    @Test
    public void test() throws Exception {

    }
}