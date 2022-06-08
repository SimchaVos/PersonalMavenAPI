package versioning;

import coordinates.CoordsProcessor;
import eu.f4sten.pomanalyzer.data.MavenId;
import eu.fasten.core.dbconnectors.PostgresConnector;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jooq.*;
import versioning.entities.BreakingChange;
import versioning.entities.Major;
import versioning.entities.Method;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class BreakingChangeCalculator {
    /**
     * Gets DSLContext, requires *FASTEN_DBPASS* environment variable in run configuration.
     *
     * @return the database context.
     * @throws Exception if it can not connect.
     */
    private static DSLContext getDbContext() throws Exception {
        return PostgresConnector.getDSLContext("jdbc:postgresql://localhost:5432/fasten_java",
            "fasten", false);
    }

    public static void main(String[] args) throws Exception {
        long start = System.nanoTime();
        DSLContext context = getDbContext();

        List<MavenId> coords = CoordsProcessor.readCoordsFile(Paths.get("").toAbsolutePath() + "/src/main/resources/mvn.expanded_coords.txt");
        Set<Result<Record5<String, Long, String, String, Long>>> results = AnalysisHandler.findMethods(context, coords);

        Map<String, Map<String, PriorityQueue<Method>>> packageIdMap = AnalysisHandler.createPackageIdMap(results);
        Map<Long, Set<DefaultArtifactVersion>> versionsPerPackageId = AnalysisHandler.getAllVersions(packageIdMap);
        Map<Long, Map<DefaultArtifactVersion, Integer>> methodsPerVersion = AnalysisHandler.getMethodsPerVersion(results);


        calculateMethodAddition(start, packageIdMap, versionsPerPackageId, methodsPerVersion);
    }

    public static void calculateMethodAddition(long start, Map<String, Map<String, PriorityQueue<Method>>> packageIdMap,
                                               Map<Long, Set<DefaultArtifactVersion>> versionsPerPackageId,
                                               Map<Long, Map<DefaultArtifactVersion, Integer>> methodsPerVersion) throws Exception {

        Map<Major, BreakingChange> incursions = new HashMap<>();

        for (Map<String, PriorityQueue<Method>> methods : packageIdMap.values()) {
            for (PriorityQueue<Method> versions : methods.values()) {
                PriorityQueue<Method> reverse = new PriorityQueue<>(Collections.reverseOrder());
                reverse.addAll(versions);

                Method newest = versions.peek();
                Long packageId = newest.packageId;
                DefaultArtifactVersion found = newest.version;

                Set<DefaultArtifactVersion> lowerVersions = new HashSet<>();

                // Identify all versions that have been released, which have a version number higher than the number at
                // which the method was introduced. Note that major releases don't count.
                for (DefaultArtifactVersion version : versionsPerPackageId.get(newest.packageId)) {
                    if (version.getMajorVersion() == found.getMajorVersion() &&
                            version.getMajorVersion() == found.getMinorVersion() &&
                            version.compareTo(found) < 0) {
                        lowerVersions.add(version);
                    }
                }
                if (lowerVersions.size() > 0) {
                    int a = 1;
                }
                // Every version in higherVersions should have a corresponding method record in the database (versions
                // priority queue).
                while (!versions.isEmpty()) {
                    DefaultArtifactVersion curr = versions.poll().version;
                    lowerVersions.removeIf(curr::equals);
                }
                Major major = new Major(packageId, found.getMajorVersion(),
                        methodsPerVersion.get(packageId).get(found), newest.packageName);
                incursions.putIfAbsent(major, new BreakingChange());

                // Now we calculate all the incursions. Each time there are higherVersions which do not have a corresponding
                // method record, we increment the incursions of the package.
                if (!lowerVersions.isEmpty()) {
                    BreakingChange incursion = incursions.get(major);
                    incursion.incursing.add(newest);
                    incursion.incursions++;
                }
            }

        }
        System.out.println(incursions);
        System.out.println("Execution time: " + (System.nanoTime() - start) / 1000000 + "ms");

        writeBreakingChangesToFile(incursions);
    }

    public static void calculateMethodRemove(long start, Map<String, Map<String, PriorityQueue<Method>>> packageIdMap,
                                             Map<Long, Set<DefaultArtifactVersion>> versionsPerPackageId,
                                             Map<Long, Map<DefaultArtifactVersion, Integer>> methodsPerVersion) throws Exception {
        Map<Major, BreakingChange> incursions = new HashMap<>();

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
                incursions.putIfAbsent(major, new BreakingChange());

                // Now we calculate all the incursions. Each time there are higherVersions which do not have a corresponding
                // method record, we increment the incursions of the package.
                if (!higherVersions.isEmpty()) {
                    BreakingChange incursion = incursions.get(major);
                    incursion.incursing.add(oldest);
                    incursion.incursions++;
                }
            }

        }
        System.out.println(incursions);
        System.out.println("Execution time: " + (System.nanoTime() - start) / 1000000 + "ms");

        writeBreakingChangesToFile(incursions);
    }

    public static void writeBreakingChangesToFile(Map<Major, BreakingChange> incursions) throws IOException {
        FileWriter fw = new FileWriter(Paths.get("").toAbsolutePath()+ "/src/main/resources/incursions.txt");
        fw.write("Skip the first line when parsing this file. The format of this file is as follows: groupId:artifactId:majorVersion:#BC/#totalMethods:[callable.IDs with BC]\n");
        for (Major major : incursions.keySet()) {
            fw.write(major + ":" + incursions.get(major).incursions + "/" + major.numberOfMethods + ":" + incursions.get(major).incursing + "\n");
        }
        fw.close();
    }
}
