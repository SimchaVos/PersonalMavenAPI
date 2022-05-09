import java.util.Arrays;

class PackageMethod implements Comparable<PackageMethod> {
    String version;
    String method;
    Long packageId;

    public PackageMethod(String version, String method, Long packageId) {
        this.version = version;
        this.method = method;
        this.packageId = packageId;
    }

    /**
     * If the version number of this is smaller than the other's version number, it will return -1.
     * @param o
     * @return
     */
    @Override
    public int compareTo(PackageMethod o) {
        int[] first_split = Arrays.stream(this.version.split("\\.")).mapToInt(Integer::parseInt).toArray();
        int[] second_split = Arrays.stream(o.version.split("\\.")).mapToInt(Integer::parseInt).toArray();

        if (first_split.length == 1) {
            return Integer.compare(first_split[0], second_split[0]);
        }
        else if (first_split.length == 2) {
            if (first_split[0] < second_split[0]) {
                return -1;
            } else if (first_split[0] > second_split[0]) {
                return 1;
            } else return Integer.compare(first_split[1], second_split[1]);
        }
        else {
            if (first_split[0] < second_split[0]) {
                return -1;
            } else if (first_split[0] > second_split[0]) {
                return 1;
            } else if (first_split[1] < second_split[1]) {
                return -1;
            } else if (first_split[1] > second_split[1]) {
                return 1;
            } else return Integer.compare(first_split[2], second_split[2]);
        }
    }
}