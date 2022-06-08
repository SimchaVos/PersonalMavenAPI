package versioning.entities;

import java.util.Objects;

public class Major {
    public Long packageId;
    public int majorVersion;
    public int numberOfMethods;
    public String packageName;

    public Major(Long packageId, int majorVersion, int numberOfMethods, String packageName) {
        this.packageId = packageId;
        this.majorVersion = majorVersion;
        this.numberOfMethods = numberOfMethods;
        this.packageName = packageName;
    }

    @Override
    public boolean equals(Object o) {
        return this.packageId.equals(((Major) o).packageId) && this.majorVersion == ((Major) o).majorVersion;
    }

    @Override
    public int hashCode() {
        return Objects.hash(packageId, majorVersion);
    }

    @Override
    public String toString() {
        return this.packageName + ":" + this.majorVersion;
    }
}
