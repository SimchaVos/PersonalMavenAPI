import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PackageMethodTest {
    @Test
    void threeDigitTest() {
        PackageMethod p1 = new PackageMethod("1.2.3", "method", 0l);
        PackageMethod p2 = new PackageMethod("1.2.4", "method", 1l);
        assertEquals(p1.compareTo(p2), -1);
    }

    @Test
    void threeDigitTestDifferentLevel() {
        PackageMethod p1 = new PackageMethod("1.2.3", "method", 0l);
        PackageMethod p2 = new PackageMethod("0.99.99", "method", 1l);
        assertEquals(p1.compareTo(p2), 1);
    }

    @Test
    void twoDigitTest() {
        PackageMethod p1 = new PackageMethod("1.2", "method", 0l);
        PackageMethod p2 = new PackageMethod("1.4", "method", 1l);
        assertEquals(p1.compareTo(p2), -1);
    }

    @Test
    void twoDigitTestThisIsBigger() {
        PackageMethod p1 = new PackageMethod("1.4", "method", 0l);
        PackageMethod p2 = new PackageMethod("1.1", "method", 1l);
        assertEquals(p1.compareTo(p2), 1);
    }

    @Test
    void oneDigitTest() {
        PackageMethod p1 = new PackageMethod("1", "method", 0l);
        PackageMethod p2 = new PackageMethod("2", "method", 1l);
        assertEquals(p1.compareTo(p2), -1);
    }
}