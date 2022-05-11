import eu.fasten.core.dbconnectors.PostgresConnector;
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

        public Major(Long packageId, int majorVersion) {
            this.packageId = packageId;
            this.majorVersion = majorVersion;
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
            return this.packageId + "v" + this.majorVersion;
        }
    }

    public static void main(String[] args) throws Exception {
        long start = System.nanoTime();
        DSLContext context = getDbContext();
        Result<Record3<Object, Object, Object>> results = AnalysisHandler.findMethods(context);
        //Ideally sort results based on column fasten_uri

        Map<Long, Map<String, PriorityQueue<Method>>> packageIdMap = AnalysisHandler.createPackageIdMap(results);
        Map<Long, Set<VersionM>> versionsPerPackageId = AnalysisHandler.getAllVersions(packageIdMap);

        Map<Major, Integer> incursions = new HashMap<>();
        Map<Method, Set<VersionM>> breakingMethods = new HashMap<>();

        for (Map<String, PriorityQueue<Method>> methods : packageIdMap.values()) {
            for (PriorityQueue<Method> versions : methods.values()) {
                Method oldest = versions.peek();
                Long packageId = oldest.packageId;
                VersionM introduced = oldest.version;

                Set<VersionM> higherVersions = new HashSet<>();

                // Identify all versions that have been released, which have a version number higher than the number at
                // which the method was introduced. Note that major releases don't count.
                for (VersionM version : versionsPerPackageId.get(oldest.packageId)) {
                    if (version.major != introduced.major) break;
                    if (version.numberOfDigits > 1) {
                        if (version.minor > introduced.minor) {
                            higherVersions.add(version);
                            break;
                        }
                        if (version.minor == introduced.minor && version.numberOfDigits > 2 && version.patch > introduced.patch) {
                            higherVersions.add(version);
                        }
                    }
                }
                // Every version in higherVersions should have a corresponding method record in the database (versions
                // priority queue).
                while (!versions.isEmpty()) {
                    VersionM curr = versions.poll().version;
                    higherVersions.removeIf(curr::equals);
                }
                incursions.putIfAbsent(new Major(packageId, introduced.major), 0);

                // Now we calculate all the incursions. Each time there are higherVersions which do not have a corresponding
                // method record, we increment the incursions of the package.
                if (!higherVersions.isEmpty()) {
                    Integer curr = incursions.get(new Major(packageId, introduced.major));
                    incursions.put(new Major(packageId, introduced.major), curr + 1);
                    breakingMethods.put(oldest, higherVersions);
                }
            }

        }
        System.out.println(incursions);
        System.out.println(breakingMethods);
        System.out.println("Execution time: " + (System.nanoTime() - start) / 1000000 + "ms");

        writeIncursionsToFile("C:\\Users\\simch\\Documents\\fasten-docker-deployment-develop\\test-resources\\",
                incursions);
    }

    public static void writeIncursionsToFile(String path, Map<Major, Integer> incursions) throws IOException {
        FileWriter fw = new FileWriter(path + "incursions");
        for (Major major : incursions.keySet()) {
            fw.write(major + "|" + incursions.get(major) + ",");
        }
        fw.close();
    }

}