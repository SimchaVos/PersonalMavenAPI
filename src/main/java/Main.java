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

        Map<Long, PriorityQueue<PackageMethod>> packageIdMap = AnalysisHandler.createPackageIdMap(results);
        Map<Long, Set<String>> versionsPerPackageId = AnalysisHandler.getAllVersions(packageIdMap);

        Map<Long, Integer> incursions = new HashMap<>();

        for (PriorityQueue<PackageMethod> packageMethod : packageIdMap.values()) {
            System.out.println(packageMethod);
        }

//        while (!results.isEmpty()) {
//            Record2<Object, Object> r = results.get(0);
//            for (int i = 1; i < results.size(); i++) {
//                Record2<Object, Object> curr = results.get(i);
//                if (curr.value1().equals(r.value1())) {
//
//                }
//            }
//        }
    }

}
