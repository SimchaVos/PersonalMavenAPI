package coordinates;

import eu.f4sten.mavencrawler.utils.FileReader;
import eu.f4sten.pomanalyzer.data.MavenId;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class CoordsProcessor {
    public static void main(String[] args) throws Exception {
        writeCoordsFile(Paths.get("").toAbsolutePath().getParent().resolve("fasten-docker-deployment\\test-resources\\").toString(), getExpandedCoords());
    }

    public static List<MavenId> getExpandedCoords() throws Exception {
        String testResourcesPath = Paths.get("").toAbsolutePath().getParent().resolve("fasten-docker-deployment\\test-resources\\").toString();
        List<MavenId> input = readCoordsFile(testResourcesPath + "/artifacts.txt");
        FileReader fr = new FileReader();
        Set<MavenId> mavenIds = fr.readIndexFile(new File(testResourcesPath + "/nexus-maven-repository-index.gz"));

        System.out.println("Done reading the Maven Index.");
        List<MavenId> expandedMavenIds = extractAllVersions(input, mavenIds);
        System.out.println("Total entries in Maven Index read: " + mavenIds.size());

        return expandedMavenIds;
    }

    public static List<MavenId> extractAllVersions(List<MavenId> inputIds, Set<MavenId> maven) {
        List<MavenId> result = new ArrayList<>();

        for (MavenId currInput : inputIds) {
            for (MavenId mavenId : maven) {
                DefaultArtifactVersion inp_v = new DefaultArtifactVersion(currInput.version);
                DefaultArtifactVersion mav_v = new DefaultArtifactVersion(mavenId.version);
                if (currInput.groupId.equals(mavenId.groupId) && currInput.artifactId.equals(mavenId.artifactId) && (inp_v.compareTo(mav_v) <= 0)) {
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
        FileWriter fw = new FileWriter(path + "/mvn.expanded_coords.txt");
        for (MavenId mavenId : mavenIds) {
            fw.write(mavenId.groupId + "." + mavenId.artifactId + ":" + mavenId.version);
        }
        fw.close();
    }

    public static List<MavenId> readCoordsFile(String path) throws FileNotFoundException {
        Scanner sc = new Scanner(new File(path));
        sc.useDelimiter(":");
        List<MavenId> coords = new ArrayList<>();

        while (sc.hasNext()) {
            MavenId newPackage = new MavenId();
            String[] split = sc.nextLine().split(":");

            newPackage.groupId = split[0];
            newPackage.artifactId = split[1];
            newPackage.version = split[2];

            coords.add(newPackage);
        }
        sc.close();
        return coords;
    }
}
