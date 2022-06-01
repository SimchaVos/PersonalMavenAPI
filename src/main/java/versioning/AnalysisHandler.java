package versioning;

import eu.f4sten.pomanalyzer.data.MavenId;
import eu.fasten.core.data.metadatadb.codegen.enums.Access;
import eu.fasten.core.data.metadatadb.codegen.tables.Callables;
import eu.fasten.core.data.metadatadb.codegen.tables.PackageVersions;
import eu.fasten.core.data.metadatadb.codegen.tables.Packages;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Record4;
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
    public static Map<Long, Map<String, PriorityQueue<Method>>> createPackageIdMap(Set<Result<Record4<String, Long, String, String>>> results) {
        Map<Long, Map<String, PriorityQueue<Method>>> packageIdMap = new HashMap<>();

        //           Method, PackageID, Version, Name
        for (Result<Record4<String, Long, String, String>> result : results) {
            for (Record4<String, Long, String, String> record : result) {
                String method = record.value1();
                Long packageId = record.value2();
                String version = record.value3();
                packageIdMap.computeIfAbsent(packageId, k -> new HashMap<>());
                packageIdMap.get(packageId).computeIfAbsent(method, k -> new PriorityQueue<>());
                packageIdMap.get(packageId).get(method).add(new Method(version, method, packageId, record.value4()));
            }
        }

        return packageIdMap;
    }

    /**
     * Gets all the versions of every package, and returns those in a map.
     * @param packageIdMap
     * @return
     */
    public static Map<Long, Set<DefaultArtifactVersion>> getAllVersions(Map<Long, Map<String, PriorityQueue<Method>>> packageIdMap) {
        Map<Long, Set<DefaultArtifactVersion>> versionsPerPackageId = new HashMap<>();

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

    public static @NotNull
    Set<Result<Record4<String, Long, String, String>>> findMethods(DSLContext context, List<MavenId> mavenIds) {
        Set<Result<Record4<String, Long, String, String>>> results = new HashSet<Result<Record4<String, Long, String, String>>>();
        for (MavenId mavenId : mavenIds) {
            results.add(findMethodsPerArtifact(context, mavenId));
        }
        return results;
    }

    /**
     * Runs the SQL query which finds every method of every package in the database.
     * @param context
     * @return
     */
    public static @NotNull
    Result<Record4<String, Long, String, String>> findMethodsPerArtifact(DSLContext context, MavenId mavenId) {
        return context.select(Callables.CALLABLES.FASTEN_URI, PackageVersions.PACKAGE_VERSIONS.PACKAGE_ID,
                        PackageVersions.PACKAGE_VERSIONS.VERSION, Packages.PACKAGES.PACKAGE_NAME)
                .from("callables")
                .join("modules").on(DSL.field("module_id").eq(DSL.field("modules.id")))
                .join("package_versions").on(DSL.field("package_version_id").eq(DSL.field("package_versions.id")))
                .join("packages").on(DSL.field("package_versions.package_id").eq(DSL.field("packages.id")))
                .and(DSL.field("packages.package_name").eq(mavenId.groupId+":"+mavenId.artifactId))
                .and(DSL.field("package_versions.version").eq(mavenId.version))
                .and(DSL.field("callables.access").eq(Access.public_))
                .and(DSL.field("defined").eq(true))
                .and(DSL.field("callables.is_internal_call").eq(true))
                .and(DSL.field("callables.fasten_uri").notLike("%$Lambda.%"))
                .fetch();
    }

    //                                                                                               Method, PackageID, Version
    public static Map<Long, Map<DefaultArtifactVersion, Integer>> getMethodsPerVersion(Set<Result<Record4<String, Long, String, String>>> results) {
        Map<Long, Map<DefaultArtifactVersion, Integer>> versions = new HashMap<>();

        for (Result<Record4<String, Long, String, String>> result : results) {
            for (Record4<String, Long, String, String> record : result) {
                Long packageId = record.value2();
                DefaultArtifactVersion version = new DefaultArtifactVersion(record.value3());
                versions.putIfAbsent(packageId, new HashMap<>());
                versions.get(packageId).putIfAbsent(version, 0);
                int i = versions.get(packageId).get(version);
                versions.get(packageId).put(version, i + 1);
            }
        }

        return versions;
    }
}
