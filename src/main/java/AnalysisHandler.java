import eu.fasten.core.data.metadatadb.codegen.enums.Access;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Record3;
import org.jooq.Result;
import org.jooq.impl.DSL;

import java.util.*;

public class AnalysisHandler {
    public static Map<Long, List<PackageVersion>> createPackageIdMap(Result<Record3<Object, Object, Object>> results) {
        Map<Long, List<PackageVersion>> packageIdMap = new HashMap<>();

        //           Method, PackageID, Version
        for (Record3<Object, Object, Object> record : results) {
            String method = (String) record.value1();
            Long packageId = (Long) record.value2();
            String version = (String) record.value3();
            packageIdMap.computeIfAbsent(packageId, k -> new ArrayList<>());

            packageIdMap.get(packageId).add(new PackageVersion(version, method, packageId));
        }

        return packageIdMap;
    }

    public static Map<Long, Set<String>> getAllVersions(Map<Long, List<PackageVersion>> packageIdMap) {
        Map<Long, Set<String>> versionsPerPackageId = new HashMap<>();

        for (List<PackageVersion> versions : packageIdMap.values()) {
            for (PackageVersion version : versions) {
                versionsPerPackageId.computeIfAbsent(version.packageId, k -> new HashSet<>());
                versionsPerPackageId.get(version.packageId).add(version.version);
            }
        }
        return versionsPerPackageId;
    }
    public static int compareTo(String first, String second) {
        int[] first_split = Arrays.stream(first.split("\\.")).mapToInt(Integer::parseInt).toArray();
        int[] second_split = Arrays.stream(second.split("\\.")).mapToInt(Integer::parseInt).toArray();
        if (first_split[0] < second_split[0]) {
            return 1;
        }
        else if (first_split[0] > second_split[0]) {
            return -1;
        }
        else if (first_split[1] < second_split[1]) {
            return 1;
        }
        else if (first_split[1] > second_split[1]) {
            return -1;
        }
        else return Integer.compare(second_split[2], first_split[2]);
    }

    public static @NotNull
    Result<Record3<Object, Object, Object>> findMethods(DSLContext context) {
        return context.select(DSL.field("fasten_uri"), DSL.field("package_id"), DSL.field("version"))
                .from("callables")
                .join("modules").on(DSL.field("module_id").eq(DSL.field("modules.id")))
                .join("package_versions").on(DSL.field("package_version_id").eq(DSL.field("package_versions.id")))
                .where(DSL.field("callables.access").eq(Access.public_)).and(DSL.field("defined").eq(true))
                .fetch();
    }

}
