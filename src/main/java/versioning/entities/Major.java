package versioning.entities;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.util.Objects;

public class Major {
    public Long packageId;
    public int majorVersion;
    public int numberOfMethods;
    public String packageName;

    public Major(Long packageId, DefaultArtifactVersion version, int numberOfMethods, String packageName) {
        this.packageId = packageId;
        if (version.getMajorVersion() == 0) {
            this.majorVersion = Integer.parseInt(version.toString().split("\\.")[0]);
        }
        else {
            this.majorVersion = version.getMajorVersion();
        }
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
