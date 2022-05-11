import eu.f4sten.mavencrawler.utils.FileReader;
import eu.f4sten.pomanalyzer.data.MavenId;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class CoordsProcessor {

    public static void main(String[] args) throws Exception {
        long time1 = System.nanoTime() / 1000000;

        String testResourcesPath = "C:\\Users\\simch\\Documents\\fasten-docker-deployment-develop\\test-resources\\";
        List<MavenId> input = readCoordsFile(testResourcesPath + "mvn.coords.quality-analyzer.txt");

        FileReader fr = new FileReader();
        Set<MavenId> mavenIds = fr.readIndexFile(new File(testResourcesPath + "nexus-maven-repository-index.gz"));
        long time2 = System.nanoTime() / 1000000;
        System.out.println("Done reading the Maven Index.");
        List<MavenId> expandedMavenIds = extractAllVersions(input, mavenIds);
        long time3 = System.nanoTime() / 1000000;
        System.out.println("Done extracting all versions.");

        writeCoordsFile(testResourcesPath, expandedMavenIds);
        long time4 = System.nanoTime() / 1000000;

        System.out.println("Reading file: " + (time2 - time1) + "ms");
        System.out.println("Extracting versions: " + (time3 - time2) + "ms");
        System.out.println("Writing file: " + (time4 - time3) + "ms");
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
