import eu.f4sten.mavencrawler.utils.FileReader;
import eu.f4sten.pomanalyzer.data.MavenId;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class CoordsProcessor {

    public static void main(String[] args) throws Exception {
        String testResourcesPath = "C:\\Users\\simch\\Documents\\fasten-docker-deployment-develop\\test-resources\\";
        List<MavenGA> input = readCoordsFile(testResourcesPath + "mvn.coords.txt");

        FileReader fr = new FileReader();
        Set<MavenId> mavenIds = fr.readIndexFile(new File(testResourcesPath + "nexus-maven-repository-index.gz"));
        System.out.println("Done reading the Maven Index.");
        Map<MavenGA, Set<String>> map = convertToMap(mavenIds);
        System.out.println("Done converting to map.");
        List<MavenId> expandedMavenIds = extractAllVersions(input, map);
        System.out.println("Done extracting all versions.");

        writeCoordsFile(testResourcesPath, expandedMavenIds);
    }

    public static class MavenGA {
        String groupId;
        String artifactId;

        public MavenGA(String groupId, String artifactId) {
            this.groupId = groupId;
            this.artifactId = artifactId;
        }

        @Override
        public boolean equals(Object o) {
            return this.groupId.equals(((MavenGA) o).groupId) && this.artifactId.equals(((MavenGA) o).artifactId);
        }

        @Override
        public int hashCode() {
            return groupId.hashCode() + artifactId.hashCode();
        }
    }

    public static Map<MavenGA, Set<String>> convertToMap(Set<MavenId> mavenIds) {
        Map<MavenGA, Set<String>> map = new HashMap<>();

        for (MavenId mavenId : mavenIds) {
            MavenGA curr = new MavenGA(mavenId.groupId, mavenId.artifactId);
            map.putIfAbsent(curr, new HashSet<>());
            map.get(curr).add(mavenId.version);
        }
        return map;
    }

    public static List<MavenId> extractAllVersions(List<MavenGA> inputIds, Map<MavenGA, Set<String>> maven) {
        List<MavenId> result = new ArrayList<>();

        for (MavenGA currInput : inputIds) {
            if (maven.containsKey(currInput)) {
                for (String version : maven.get(currInput)) {
                    MavenId toBeAdded = new MavenId();
                    toBeAdded.groupId = currInput.groupId;
                    toBeAdded.artifactId = currInput.artifactId;
                    toBeAdded.version = version;
                    result.add(toBeAdded);
                }
            }
        }
        return result;
    }

    public static void writeCoordsFile(String path, List<MavenId> mavenIds) throws IOException {
        FileWriter fw = new FileWriter(path + "mvn.expanded_coords.txt");
        for (MavenId mavenId : mavenIds) {
            fw.write(mavenId.groupId + "." + mavenId.artifactId + ":" + mavenId.version + "|{\"groupId\":\""
                    + mavenId.groupId + "\",\"artifactId\":\"" + mavenId.artifactId + "\",\"version\":\""
                    + mavenId.version + "\"}\n");
        }
        fw.close();
    }

    public static List<MavenGA> readCoordsFile(String path) throws FileNotFoundException {
        Scanner sc = new Scanner(new File(path));
        sc.useDelimiter(":");
        List<MavenGA> coords = new ArrayList<>();

        while (sc.hasNext()) {
            MavenGA newPackage = new MavenGA(sc.next().replaceAll("^[\n\r]", ""), sc.next());
            coords.add(newPackage);
            sc.useDelimiter("\n");
            sc.next();
            sc.useDelimiter(":");
        }
        sc.close();
        return coords;
    }
}
