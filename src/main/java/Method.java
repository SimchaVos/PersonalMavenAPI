import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.util.Arrays;

class Method implements Comparable<Method> {
    DefaultArtifactVersion version;
    String method;
    Long packageId;

    @Override
    public String toString() {
        return this.packageId + ":" + this.method + "@v" + this.version;
    }

    public Method(String version, String method, Long packageId) {
        int[] splitVersion = Arrays.stream(version.split("\\.")).mapToInt(Integer::parseInt).toArray();

        int major = splitVersion[0];
        int minor = splitVersion.length > 1 ? splitVersion[1] : -1;
        int patch = splitVersion.length > 2 ? splitVersion[2] : -1;

        this.version = new VersionM(major, minor, patch, splitVersion.length);

        this.method = method;
        this.packageId = packageId;
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