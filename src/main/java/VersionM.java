import org.jetbrains.annotations.NotNull;

public class VersionM implements Comparable<VersionM> {
    int major, minor, patch, numberOfDigits;

    public VersionM(int major, int minor, int patch, int numberOfDigits) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.numberOfDigits = numberOfDigits;
    }

    @Override
    public int compareTo(@NotNull VersionM o) {
        if (this.numberOfDigits == 1) {
            return Integer.compare(this.major, o.major);
        } else if (this.numberOfDigits == 2) {
            if (this.major < o.major) {
                return -1;
            } else if (this.major > o.major) {
                return 1;
            } else return Integer.compare(this.minor, o.minor);
        } else {
            if (this.major < o.major) {
                return -1;
            } else if (this.major > o.major) {
                return 1;
            } else if (this.minor < o.minor) {
                return -1;
            } else if (this.minor > o.minor) {
                return 1;
            } else return Integer.compare(this.patch, o.patch);
        }
    }

    public Boolean equals(VersionM o) {
        return this.compareTo(o) == 0;
    }
}