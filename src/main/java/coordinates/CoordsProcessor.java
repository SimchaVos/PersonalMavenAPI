package coordinates;

import eu.f4sten.mavencrawler.utils.FileReader;
import eu.f4sten.pomanalyzer.data.MavenId;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class CoordsProcessor {

    public static void main(String[] args) throws Exception {
        String testResourcesPath = Paths.get("").toAbsolutePath().getParent().resolve("fasten-docker-deployment\\test-resources\\").toString();
        List<MavenId> input = readCoordsFile(testResourcesPath + "/mvn.coords.txt");
        FileReader fr = new FileReader();
        Set<MavenId> mavenIds = fr.readIndexFile(new File(testResourcesPath + "/nexus-maven-repository-index.gz"));

        System.out.println("Done reading the Maven Index.");
        List<MavenId> expandedMavenIds = extractAllVersions(input, mavenIds);

        writeCoordsFile(testResourcesPath, expandedMavenIds);

        System.out.println("Total entries in Maven Index read: " + mavenIds.size());
    }

    public static List<MavenId> extractAllVersions(List<MavenId> inputIds, Set<MavenId> maven) {
        List<MavenId> result = new ArrayList<>();

        for (MavenId currInput : inputIds) {
            for (MavenId mavenId : maven) {
                if (currInput.groupId.equals(mavenId.groupId) && currInput.artifactId.equals(mavenId.artifactId)) {
                    MavenId toAdd = new MavenId();
                    toAdd.groupId = mavenId.groupId;
                    toAdd.artifactId = mavenId.artifactId;
                    toAdd.version = mavenId.version;
                    result.add(toAdd);
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

    public static List<MavenId> readCoordsFile(String path) throws FileNotFoundException {
        Scanner sc = new Scanner(new File(path));
        sc.useDelimiter(":");
        List<MavenId> coords = new ArrayList<>();

        while (sc.hasNext()) {
            MavenId newPackage = new MavenId();
            newPackage.groupId = sc.next().replaceAll("^[\n\r]", "");
            newPackage.artifactId = sc.next();
            coords.add(newPackage);
            sc.useDelimiter("\n");
            sc.next();
            sc.useDelimiter(":");
        }
        sc.close();
        return coords;
    }
}
