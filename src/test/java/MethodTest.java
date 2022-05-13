import kotlin.random.Random;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MethodTest {
    @Test
    void threeDigitTest() {
        Method p1 = new Method("1.2.3", "method", 0L, "package");
        Method p2 = new Method("1.2.4", "method", 1L, "package");
        assertTrue(p1.compareTo(p2) < 0);
    }

    @Test
    void threeDigitTestDifferentLevel() {
        Method p1 = new Method("1.2.3", "method", 0L, "package");
        Method p2 = new Method("0.99.99", "method", 1L, "package");
        assertTrue(p1.compareTo(p2) > 0);
    }

    @Test
    void twoDigitTest() {
        Method p1 = new Method("1.2", "method", 0L, "package");
        Method p2 = new Method("1.4", "method", 1L, "package");
        assertTrue(p1.compareTo(p2) < 0);
    }

    @Test
    void twoDigitTestThisIsBigger() {
        Method p1 = new Method("1.4", "method", 0L, "package");
        Method p2 = new Method("1.1", "method", 1L, "package");
        assertTrue(p1.compareTo(p2) > 0);
    }

    @Test
    void stringInVersionNumber() {
        Method p1 = new Method("1.1.1-beta-2", "method", 0L, "package");
        Method p2 = new Method("1.1.1-alpha", "method", 1L, "package");
        assertTrue(p1.compareTo(p2) > 0);
    }

    @Test
    void defaultArtifactVersion() {
        DefaultArtifactVersion version1 = new DefaultArtifactVersion("1.1.2-beta-2");
        DefaultArtifactVersion version2 = new DefaultArtifactVersion("1.1.2");
        assertTrue(version1.compareTo(version2) < 0);
    }

    @Test
    void stringVersusNumber() {
        Method p1 = new Method("1.1.1-beta-2", "method", 0L, "package");
        Method p2 = new Method("1.1.1", "method", 1L, "package");
        assertTrue(p1.compareTo(p2) < 0);
    }

    @Test
    void oneDigitTest() {
        Method p1 = new Method("1", "method", 0L, "package");
        Method p2 = new Method("2", "method", 1L, "package");
        assertTrue(p1.compareTo(p2) < 0);
    }
}