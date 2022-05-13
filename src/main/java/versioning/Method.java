package versioning;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.util.Arrays;

class Method implements Comparable<Method> {
    DefaultArtifactVersion version;
    String method;
    Long packageId;
    String packageName;

    @Override
    public String toString() {
        return this.packageId + ":" + this.method + "@v" + this.version;
    }

    public Method(String version, String method, Long packageId, String packageName) {
        this.version = new DefaultArtifactVersion(version);
        this.method = method;
        this.packageId = packageId;
        this.packageName = packageName;
    }

    /**
     * If the version number of this is smaller than the other's version number, it will return -1.
     * @param o
     * @return
     */
    @Override
    public int compareTo(Method o) {
        return this.version.compareTo(o.version);
    }
}