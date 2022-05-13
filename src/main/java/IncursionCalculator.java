import eu.fasten.core.dbconnectors.PostgresConnector;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jooq.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class IncursionCalculator {
    /**
     * Gets DSLContext, requires *FASTENDB_PASS* environment variable in run configuration.
     *
     * @return the database context.
     * @throws Exception if it can not connect.
     */
    private static DSLContext getDbContext() throws Exception {
        return PostgresConnector.getDSLContext("jdbc:postgresql://localhost:5432/fasten_java", "fasten", false);
    }

    public static class Major {
        Long packageId;
        int majorVersion;
        int numberOfMethods;
        String packageName;

        public Major(Long packageId, int majorVersion) {
            this.packageId = packageId;
            this.majorVersion = majorVersion;
        }

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
            return this.packageName + "v" + this.majorVersion;
        }
    }

    public static void main(String[] args) throws Exception {
        long start = System.nanoTime();
        DSLContext context = getDbContext();
        Result<Record4<String, Long, String, String>> results = AnalysisHandler.findMethods(context);
        //Ideally sort results based on column fasten_uri

        Map<Long, Map<String, PriorityQueue<Method>>> packageIdMap = AnalysisHandler.createPackageIdMap(results);
        Map<Long, Set<DefaultArtifactVersion>> versionsPerPackageId = AnalysisHandler.getAllVersions(packageIdMap);
        Map<Long, Map<DefaultArtifactVersion, Integer>> methodsPerVersion = AnalysisHandler.getMethodsPerVersion(results);

        Map<Major, Integer> incursions = new HashMap<>();
        Map<Method, Set<DefaultArtifactVersion>> breakingMethods = new HashMap<>();

        for (Map<String, PriorityQueue<Method>> methods : packageIdMap.values()) {
            for (PriorityQueue<Method> versions : methods.values()) {
                Method oldest = versions.peek();
                Long packageId = oldest.packageId;
                DefaultArtifactVersion introduced = oldest.version;

                Set<DefaultArtifactVersion> higherVersions = new HashSet<>();

                // Identify all versions that have been released, which have a version number higher than the number at
                // which the method was introduced. Note that major releases don't count.
                for (DefaultArtifactVersion version : versionsPerPackageId.get(oldest.packageId)) {
                    if (version.getMajorVersion() == introduced.getMajorVersion() && version.compareTo(introduced) > 0) {
                        higherVersions.add(version);
                    }
                }
                // Every version in higherVersions should have a corresponding method record in the database (versions
                // priority queue).
                while (!versions.isEmpty()) {
                    DefaultArtifactVersion curr = versions.poll().version;
                    higherVersions.removeIf(curr::equals);
                }
                incursions.putIfAbsent(new Major(packageId, introduced.getMajorVersion(),
                        methodsPerVersion.get(packageId).get(introduced), oldest.packageName), 0);

                // Now we calculate all the incursions. Each time there are higherVersions which do not have a corresponding
                // method record, we increment the incursions of the package.
                if (!higherVersions.isEmpty()) {
                    Integer curr = incursions.get(new Major(packageId, introduced.getMajorVersion()));
                    incursions.put(new Major(packageId, introduced.getMajorVersion()), curr + 1);
                    breakingMethods.put(oldest, higherVersions);
                }
            }

        }
        System.out.println(incursions);
        System.out.println(breakingMethods);
        System.out.println("Execution time: " + (System.nanoTime() - start) / 1000000 + "ms");

        writeIncursionsToFile("C:\\Users\\simch\\Documents\\1fasten-docker-deployment-develop\\test-resources\\",
                incursions);
    }

    public static void writeIncursionsToFile(String path, Map<Major, Integer> incursions) throws IOException {
        FileWriter fw = new FileWriter(path + "incursions");
        for (Major major : incursions.keySet()) {
            fw.write(major + "," + incursions.get(major) + "/" + major.numberOfMethods + ",\n");
        }
        fw.close();
    }
    // also store signature of method incursing and name of package

}
