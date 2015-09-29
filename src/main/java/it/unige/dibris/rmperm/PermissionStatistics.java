package it.unige.dibris.rmperm;

import java.io.File;
import java.io.IOException;
import java.util.*;


class PermissionStatistics {
    private final Map<String, List<String>> _appnameToPerms = new HashMap<>();
    private final Map<String, List<String>> _permToAppnames = new HashMap<>();
    private final Map<String, PermOcc> _permToOccurrences = new HashMap<>();
    private final List<PermOcc> _permOccOrderedList = new ArrayList<>();
    private String _folderWithApks;
    private double _avg;
    private double _variance;
    private double _stdDeviation;
    private MaxMin _maxMin;
    private final IOutput out;


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

    public PermissionStatistics(File folderWithApks, IOutput out) {
        this.out = out;
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
            for (List<String> perms : _appnameToPerms.values()) {
                for (String p : perms) {
                    if (_permToOccurrences.containsKey(p)) {
                        _permToOccurrences.get(p).incrementOcc();
                    }
                    else {
                        _permToOccurrences.put(p, new PermOcc(p));
                    }
                }
            }
            calculateStatistics();
        }
        else {
            out.printf(IOutput.Level.ERROR, "You can also get statistics of APKs into a folder! Check your path!\n");
        }
    }

    private void analyzesApk (String apk) {
        try {
            AndroidManifest manifest = AndroidManifest.extractManifest(apk);
            List<String> permissions = new ArrayList<>();
            for (String p : manifest.getPermissions()) {
                String simplePerm = Permissions.simplifyPermissionName(p);
                permissions.add(simplePerm);
                if (!_permToAppnames.containsKey(simplePerm)) {
                    _permToAppnames.put(simplePerm, new ArrayList<String>());
                }
                if (!_permToAppnames.get(simplePerm).contains(apk)) {
                    _permToAppnames.get(simplePerm).add(apk);
                }
            }
            _appnameToPerms.put(apk, permissions);
        } catch (IOException e) {
            out.printf(IOutput.Level.ERROR, "Can't read the APK: '"+ apk + "'\n");
        }
    }


    private void calculateStatistics() {
        double sum = 0.0;
        for (List<String> perms : _appnameToPerms.values()) {
            sum += perms.size();
        }
        _avg = sum / _appnameToPerms.size();

        sum = 0.0;
        for (List<String> perms : _appnameToPerms.values()) {
            sum += Math.pow((perms.size() - _avg), 2);
        }
        _variance = sum / (_appnameToPerms.size()-1);
        _stdDeviation = Math.sqrt(_variance);

        int max=0, min=Integer.MAX_VALUE, occ;
        String maxName, minName;
        maxName = minName = "";
        for (String permName : _appnameToPerms.keySet()) {
            List<String> perms = _appnameToPerms.get(permName);
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


        for (PermOcc po : _permToOccurrences.values()) {
            _permOccOrderedList.add(po);
        }
        _permOccOrderedList.sort(new PermOcc.OccurrencesComparator());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Statistics of APK's permissions stored in: '" + _folderWithApks + "'\n\n" +
                "nOfAPKs='" + _appnameToPerms.size() + "'\n" +
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