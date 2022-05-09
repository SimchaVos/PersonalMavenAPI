class PackageVersion {
    String version;
    String method;
    Long packageId;

    public PackageVersion(String version, String method, Long packageId) {
        this.version = version;
        this.method = method;
        this.packageId = packageId;
    }
}