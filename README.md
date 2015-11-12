# RmPerm

RmPerm is a command line tool, written in Java, for remove the permissions from an APK file replacing (using [dexlib2](https://github.com/JesusFreke/smali/tree/master/dexlib2)) the API methods that use certain permissions with custom ones.

You can create your custom methods [in this way](https://github.com/simoneaonzo/CustomApp).

You can download the JAR from [here](https://dl.dropboxusercontent.com/u/35859278/RmPerm.jar).

I also released an [Android app](https://github.com/simoneaonzo/AndRmPerm) that uses it, it's GUI is disgusting, any help will be appreciated!

## Usage

```
usage: rmperm [-c <APK/DEX-filename>] [-d] [-h] [-i <APK-filename>] -l |
-r | -s <Folder-path> [-o <APK-filename>] [-p <CSV permission
names>] [-v]
-c,--custom-methods <APK/DEX-filename> APK/Dex filename of custom
classes
-d,--debug Debug output (implies -v)
-h,--help Print this help
-i,--input <APK-filename> Input APK file
-l,--list List permissions
-o,--output <APK-filename> Output APK filename
-p,--permissions <CSV permission names> Permissions to remove
-r,--remove Remove permissions
-s,--statistics <Folder-path> Statistics of contained APKs
-v,--verbose Verbose output
```

There are three kinds of scenario:

1. You want to see what permissions  are required by the APK:
  * --input *file.apk*
  * --list
2. You want to remove certain permissions from the APK:
  * --input *file.apk*
  * --remove
  * --permissions *PERM1,PERM2,...,PERMN*
  * --custom-methods *custom.apk | classes.dex*
  * --output *output.apk*
3. You want to obtain statistics from a set of APKs stored in the same folder:
  * --statistics *folder/*


## How it works?

Consider a method *m* invoked on a *c* instance of type *C* that returns an object *Or* of type *Tr*, invoked with *n* arguments *a1, ..., an* respectively of type *T1, ..., Tn*

**Tr Or = c.m(T1 a1, ... , Tn an)**

If *Tr = void* the call can be easily removed, otherwise you have to define a static method (so you don't need an instance for invoke it) with the same name (*m* in our case) in a class *H* that returns the same object (*Or* of type *Tr*), invoked with *n+1* arguments *a1, ..., an, an+1* the first of them of type *C*.

**Tr Or = H.m(C c, T1 a1, ... , Tn an)**

In this way for remove a permissionis is sufficient to create a class *H* with all methods of permissions you want to remove.


## Example

Probably you have read anything of what I've written, ok I give to you an example with the method *isWifiEnabled* that use the permission ACCESS_WIFI_STATE:
```
WifiManager wifi = (WifiManager) getSystemService(Context.WIFI SERVICE);
boolean b = wifi.isWifiEnabled();
if (b) {...}
```

You define this method (you can find more of them [here](https://github.com/simoneaonzo/CustomApp/blob/master/app/src/main/java/com/custom/customapp/CustomMethods.java)):
```
@CustomMethodClass
public class CustomMethods {
    @MethodPermission(permission = "android.permission.ACCESS_WIFI_STATE", defClass ="android.net.wifi.WifiManager")
    public static boolean isWifiEnabled(WifiManager wm) {
        return false;
    }
}
```
Here I do something simple but you can instrument, log, hack, crack, trick, track... do what you want!


Finally RmPerm transform it into:
```
WifiManager wifi = (WifiManager) getSystemService(Context.WIFI SERVICE);
boolean b = CustomMethods.isWifiEnabled(wifi);
```

