package versioning;

import coordinates.CoordsProcessor;
import eu.f4sten.pomanalyzer.data.MavenId;
import eu.fasten.core.dbconnectors.PostgresConnector;
import kotlin.random.Random;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jooq.*;
import versioning.entities.BreakingChange;
import versioning.entities.Major;
import versioning.entities.Method;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

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

                for (Method curr : reverse) {
                    Method newest = curr;
                    Long packageId = newest.packageId;
                    if (newest.packageName.equals("io.micronaut:micronaut-aop")) {
                        int a = 1;
                    }
                    DefaultArtifactVersion found = newest.version;

                    PriorityQueue<DefaultArtifactVersion> lowerVersions = new PriorityQueue<>(Collections.reverseOrder());

                    // Identify all versions that have been released, which have a version number higher than the number at
                    // which the method was introduced. Note that major releases don't count.
                    for (DefaultArtifactVersion version : versionsPerPackageId.get(newest.packageId)) {
                        if (version.getMajorVersion() == found.getMajorVersion() &&
                                version.getMajorVersion() == found.getMinorVersion() &&
                                version.compareTo(found) < 0) {
                            lowerVersions.add(version);
                        }
                    }

                    // Every version in higherVersions should have a corresponding method record in the database (versions
                    // priority queue).
                    DefaultArtifactVersion oneLower = getLowerVersion(reverse.stream().map(x -> x.version).collect(Collectors.toSet()), newest.version);

                    Major major = new Major(packageId, found.getMajorVersion(),
                            methodsPerVersion.get(packageId).get(found), newest.packageName);
                    incursions.putIfAbsent(major, new BreakingChange());

                    if (lowerVersions.isEmpty()) continue;

                    // Now we calculate all the incursions. Each time there are higherVersions which do not have a corresponding
                    // method record, we increment the incursions of the package.
                    DefaultArtifactVersion totalLower = lowerVersions.poll();
                    if (oneLower != null && !oneLower.equals(totalLower)) {
                        BreakingChange incursion = incursions.get(major);
                        incursion.incursing.add(newest);
                        incursion.incursions++;
                    }
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
                for (Method curr : versions) {
                    Method oldest = curr;
                    Long packageId = oldest.packageId;
                    DefaultArtifactVersion introduced = oldest.version;
                    PriorityQueue<Method> versionsWithMethod = new PriorityQueue<>(versions);
                    PriorityQueue<DefaultArtifactVersion> higherVersionsTotal = new PriorityQueue<>();

                    // Identify all versions that have been released, which have a version number higher than the number at
                    // which the method was introduced. Note that major releases don't count.
                    for (DefaultArtifactVersion version : versionsPerPackageId.get(oldest.packageId)) {
                        if (version.getMajorVersion() == introduced.getMajorVersion() && version.compareTo(introduced) > 0) {
                            higherVersionsTotal.add(version);
                        }
                    }

                    DefaultArtifactVersion nextVersionWithMethod = getHigherVersion(versionsWithMethod.stream().map(x -> x.version).collect(Collectors.toSet()), introduced);

                    Major major = new Major(packageId, introduced.getMajorVersion(),
                            methodsPerVersion.get(packageId).get(introduced), oldest.packageName);
                    incursions.putIfAbsent(major, new BreakingChange());

                    if (higherVersionsTotal.isEmpty()) continue;

                    // Now we calculate all the incursions. Each time there are higherVersions which do not have a corresponding
                    // method record, we increment the incursions of the package.
                    DefaultArtifactVersion nextVersionTotal = higherVersionsTotal.poll();
                    if (!nextVersionTotal.equals(nextVersionWithMethod)) {
                        BreakingChange incursion = incursions.get(major);
                        incursion.incursing.add(oldest);
                        incursion.incursions++;
                    }
                }
            }

        }
        System.out.println(incursions);
        System.out.println("Execution time: " + (System.nanoTime() - start) / 1000000 + "ms");

        writeBreakingChangesToFile(incursions);
    }

    public static DefaultArtifactVersion getLowerVersion(Set<DefaultArtifactVersion> lowerVersions, DefaultArtifactVersion version) {
        PriorityQueue<DefaultArtifactVersion> pq = new PriorityQueue<>(Collections.reverseOrder());
        pq.addAll(lowerVersions);

        DefaultArtifactVersion curr = pq.poll();
        while (!curr.equals(version)) {
            curr = pq.poll();
        }
        return pq.poll();
    }

    public static DefaultArtifactVersion getHigherVersion(Set<DefaultArtifactVersion> lowerVersions, DefaultArtifactVersion version) {
        PriorityQueue<DefaultArtifactVersion> pq = new PriorityQueue<>();
        pq.addAll(lowerVersions);

        DefaultArtifactVersion curr = pq.poll();
        while (!curr.equals(version)) {
            curr = pq.poll();
        }
        return pq.poll();
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
