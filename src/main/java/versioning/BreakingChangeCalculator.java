package versioning;

import coordinates.CoordsProcessor;
import eu.f4sten.pomanalyzer.data.MavenId;
import eu.fasten.core.dbconnectors.PostgresConnector;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jooq.*;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class BreakingChangeCalculator {
    /**
     * Gets DSLContext, requires *FASTENDB_PASS* environment variable in run configuration.
     *
     * @return the database context.
     * @throws Exception if it can not connect.
     */
    private static DSLContext getDbContext() throws Exception {
        return PostgresConnector.getDSLContext("jdbc:postgresql://monster.ewi.tudelft.nl:5432/fasten_java", "simch", false);
    }

    public static class Major {
        Long packageId;
        int majorVersion;
        int numberOfMethods;
        String packageName;

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

    public static class Incursion {
        int incursions;
        Set<Method> incursing;

        public Incursion() {
            this.incursions = 0;
            this.incursing = new HashSet<>();
        }

        @Override
        public String toString() {
            return "{ " + this.incursions + " incursions in " + this.incursing;
        }
    }

    public static void main(String[] args) throws Exception {
        long start = System.nanoTime();
        DSLContext context = getDbContext();

        List<MavenId> coords = CoordsProcessor.getExpandedCoords();
        Set<Result<Record4<String, Long, String, String>>> results = AnalysisHandler.findMethods(context, coords);

        Map<Long, Map<String, PriorityQueue<Method>>> packageIdMap = AnalysisHandler.createPackageIdMap(results);
        Map<Long, Set<DefaultArtifactVersion>> versionsPerPackageId = AnalysisHandler.getAllVersions(packageIdMap);
        Map<Long, Map<DefaultArtifactVersion, Integer>> methodsPerVersion = AnalysisHandler.getMethodsPerVersion(results);

        Map<Major, Incursion> incursions = new HashMap<>();

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
                Major major = new Major(packageId, introduced.getMajorVersion(),
                        methodsPerVersion.get(packageId).get(introduced), oldest.packageName);
                incursions.putIfAbsent(major, new Incursion());

                // Now we calculate all the incursions. Each time there are higherVersions which do not have a corresponding
                // method record, we increment the incursions of the package.
                if (!higherVersions.isEmpty()) {
                    Incursion incursion = incursions.get(major);
                    incursion.incursing.add(oldest);
                    incursion.incursions++;
                }
            }

        }
        System.out.println(incursions);
        System.out.println("Execution time: " + (System.nanoTime() - start) / 1000000 + "ms");

        writeIncursionsToFile(Paths.get("").toAbsolutePath().getParent().resolve("fasten-docker-deployment\\test-resources\\").toString(),
                incursions);
    }

    public static void writeIncursionsToFile(String path, Map<Major, Incursion> incursions) throws IOException {
        FileWriter fw = new FileWriter(path + "/incursions");
        for (Major major : incursions.keySet()) {
            fw.write(major + ":" + incursions.get(major).incursions + "/" + major.numberOfMethods + ":" + incursions.get(major).incursing + "\n");
        }
        fw.close();
    }
}
