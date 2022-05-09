import eu.fasten.core.data.metadatadb.codegen.enums.Access;
import eu.fasten.core.dbconnectors.PostgresConnector;
import org.jetbrains.annotations.NotNull;
import org.jooq.*;
import org.jooq.impl.DSL;

import java.util.*;


public class Main {
    /**
     * Gets DSLContext, requires *FASTENDB_PASS* environment variable in run configuration.
     *
     * @return the database context.
     * @throws Exception if it can not connect.
     */
    private static DSLContext getDbContext() throws Exception {
        return PostgresConnector.getDSLContext("jdbc:postgresql://localhost:5432/fasten_java", "fasten", false);
    }

    public static void main(String[] args) throws Exception {
        DSLContext context = getDbContext();
        Result<Record3<Object, Object, Object>> results = AnalysisHandler.findMethods(context);
        //Ideally sort results based on column fasten_uri

        Map<Long, Map<String, PriorityQueue<PackageMethod>>> packageIdMap = AnalysisHandler.createPackageIdMap(results);
        Map<Long, Set<VersionM>> versionsPerPackageId = AnalysisHandler.getAllVersions(packageIdMap);

        Map<Long, Integer> incursions = new HashMap<>();

        for (Map<String, PriorityQueue<PackageMethod>> methods : packageIdMap.values()) {
            for (PriorityQueue<PackageMethod> versions : methods.values()) {
                Long packageId = versions.peek().packageId;
                VersionM introduced = versions.peek().version;
                // Find all minor and patch releases higher than the introduced. They should be featured in versions.
                Set<VersionM> higherVersions = new HashSet<>();
                for (VersionM version : versionsPerPackageId.get(versions.peek().packageId)) {
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
                // Every version in higherVersions should be featured in the versions PQ.
                while (!versions.isEmpty()) {
                    VersionM curr = versions.poll().version;
                    higherVersions.removeIf(curr::equals);
                }
                incursions.putIfAbsent(packageId, 0);
                if (!higherVersions.isEmpty()) {
                    Integer curr = incursions.get(packageId);
                    incursions.put(packageId, curr + 1);
                }
            }

        }
        System.out.println(incursions);
    }

}
