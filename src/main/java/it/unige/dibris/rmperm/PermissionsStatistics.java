package it.unige.dibris.rmperm;

import javax.naming.OperationNotSupportedException;
import java.io.File;
import java.io.IOException;
import java.util.*;


public class PermissionsStatistics {
    private final Map<String, List<String>> appnameToPerms = new HashMap<>();
    private final Map<String, PermOcc> permToOccurrences = new HashMap<>();
    private final List<PermOcc> _permOccOrderedList = new ArrayList<>();
    private String _folderWithApks;
    private double _avg;
    private double _variance;
    private double _stdDeviation;
    private MaxMin _maxMin;


    public double getAvg() {
        return _avg;
    }

    public double getVariance() {
        return _variance;
    }

    public double getStandardDeviation() {
        return _stdDeviation;
    }

    public MaxMin getMaxMin() {
        return _maxMin;
    }

    public List<PermOcc> getPermissionsUsage() {
        return _permOccOrderedList;
    }

    public PermissionsStatistics(File folderWithApks) {
        if (folderWithApks.isDirectory()) {
            _folderWithApks = folderWithApks.toString();
            for (final File file : folderWithApks.listFiles()) {
                if (file.isFile()) {
                    String name = file.getName();
                    if (name.substring(name.lastIndexOf(".")).equals(".apk")) {
                        analyzesApk(file.toString());
                    }
                }
            }
            for (List<String> perms : appnameToPerms.values()) {
                for (String p : perms) {
                    if (permToOccurrences.containsKey(p)) {
                        permToOccurrences.get(p).incrementOcc();
                    }
                    else {
                        permToOccurrences.put(p, new PermOcc(p));
                    }
                }
            }
            calculateStatistics();
        }
    }

    private void analyzesApk (String apk) {
        try {
            AndroidManifest manifest = AndroidManifest.extractManifest(apk);
            List<String> permissions = new ArrayList<>();
            for (String p : manifest.getPermissions())
                permissions.add(Permissions.simplifyPermissionName(p));
            appnameToPerms.put(apk, permissions);
        } catch (IOException e) {
            //TODO stampa errore
        }
    }


    private void calculateStatistics() {
        double sum = 0.0;
        for (List<String> perms : appnameToPerms.values()) {
            sum += perms.size();
        }
        _avg = sum / appnameToPerms.size();

        sum = 0.0;
        for (List<String> perms : appnameToPerms.values()) {
            sum += Math.pow((perms.size() - _avg), 2);
        }
        _variance = sum / (appnameToPerms.size()-1);
        _stdDeviation = Math.sqrt(_variance);

        int max=0, min=Integer.MAX_VALUE, occ;
        String maxName, minName;
        maxName = minName = "";
        for (String permName : appnameToPerms.keySet()) {
            List<String> perms = appnameToPerms.get(permName);
            occ = perms.size();
            if (occ > max) {
                max = occ;
                maxName = permName;
            }
            if (occ < min) {
                min = occ;
                minName = permName;
            }
        }
        _maxMin = new MaxMin(maxName, max, minName, min);


        for (PermOcc po : permToOccurrences.values()) {
            _permOccOrderedList.add(po);
        }
        _permOccOrderedList.sort(new PermOcc.OccurrencesComparator());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Statistics of APK's permissions stored in: '" + _folderWithApks + "'\n\n" +
                "nOfAPKs='" + appnameToPerms.size() + "'\n" +
                "avg='" + _avg + "'\n" +
                "variance='" + _variance + "'\n" +
                "stdDeviation='" + _stdDeviation + "'\n" +
                _maxMin + "\n\n" +
                "PERMISSION -> OCCURRENCES\n"
        );
        for (PermOcc po : _permOccOrderedList) {
            sb.append(po.toString());
        }
        return sb.toString();
    }
}


class MaxMin {
    private String _maxName;
    private int _max;
    private String _minName;
    private int _min;

    public MaxMin(String maxName, int max, String minName, int min) {
        this._maxName = maxName;
        this._max = max;
        this._minName = minName;
        this._min = min;
    }

    @Override
    public String toString() {
        return "APK that requires more permissions='" + _maxName + "'. Requiring='" + _max + "' permissions\n" +
                "APK that requires less permissions='" + _minName + "'. Requiring='" + _min + "' permissions\n";
    }
}


class PermOcc implements Comparable<PermOcc> {
    private String _permName;
    private int _occurrences;

    public PermOcc(String permName) {
        this._permName = permName;
        this._occurrences = 1;
    }

    public void incrementOcc() {
        _occurrences++;
    }

    public String getPermName() {
        return _permName;
    }

    public int getOccurrences() {
        return _occurrences;
    }

    @Override
    public int compareTo(PermOcc po) {
        if (_occurrences == po.getOccurrences()) {
            return 0;
        }
        if (_occurrences > po.getOccurrences()) {
            return 1;
        }
        return -1;
    }

    static class OccurrencesComparator implements Comparator<PermOcc> {
        @Override
        public int compare(PermOcc o1, PermOcc o2) {
            return -1 * o1.compareTo(o2); // descending order
        }
    }

    @Override
    public String toString() {
        return _permName + " -> " + _occurrences + "\n";
    }
}