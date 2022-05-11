import eu.fasten.core.data.metadatadb.codegen.enums.Access;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Record3;
import org.jooq.Result;
import org.jooq.impl.DSL;

import java.util.*;

public class AnalysisHandler {

    /**
     * Puts together a map, which maps the ID of a package to a map containing the method string and a priority
     * queue which contains all the method records.
     * @param results
     * @return
     */
    public static Map<Long, Map<String, PriorityQueue<Method>>> createPackageIdMap(Result<Record3<Object, Object, Object>> results) {
        Map<Long, Map<String, PriorityQueue<Method>>> packageIdMap = new HashMap<>();

        //           Method, PackageID, Version
        for (Record3<Object, Object, Object> record : results) {
            String method = (String) record.value1();
            Long packageId = (Long) record.value2();
            String version = (String) record.value3();
            packageIdMap.computeIfAbsent(packageId, k -> new HashMap<>());
            packageIdMap.get(packageId).computeIfAbsent(method, k -> new PriorityQueue<>());//(Collections.reverseOrder()));
            packageIdMap.get(packageId).get(method).add(new Method(version, method, packageId));
        }

        return packageIdMap;
    }

    /**
     * Gets all the versions of every package, and returns those in a map.
     * @param packageIdMap
     * @return
     */
    public static Map<Long, Set<VersionM>> getAllVersions(Map<Long, Map<String, PriorityQueue<Method>>> packageIdMap) {
        Map<Long, Set<VersionM>> versionsPerPackageId = new HashMap<>();

        for (Map<String, PriorityQueue<Method>> methods : packageIdMap.values()) {
            for (PriorityQueue<Method> versions : methods.values()) {
                for (Method version : versions) {
                    versionsPerPackageId.computeIfAbsent(version.packageId, k -> new HashSet<>());
                    versionsPerPackageId.get(version.packageId).add(version.version);
                }
            }
        }
        return versionsPerPackageId;
    }

    /**
     * Runs the SQL query which finds every method of every package in the database.
     * @param context
     * @return
     */
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
