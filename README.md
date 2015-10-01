# RmPerm

Remove permissions from an APK file replacing the methods that use certain permissions with [custom ones](https://github.com/simoneaonzo/CustomApp).

You can download the JAR from [here]()

I also released an [Android app]() that uses it, but it is really disgusting, any help will be appreciated!

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
  * --custom-methods *custom.apk*
  * --output *output.apk*
3. You want to obtain statistics from a set of APKs stored in the same folder:
  * --statistics *folder/*
