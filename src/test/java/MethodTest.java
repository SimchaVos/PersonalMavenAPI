import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MethodTest {
    @Test
    void threeDigitTest() {
        Method p1 = new Method("1.2.3", "method", 0L);
        Method p2 = new Method("1.2.4", "method", 1L);
        assertEquals(p1.compareTo(p2), -1);
    }

    @Test
    void threeDigitTestDifferentLevel() {
        Method p1 = new Method("1.2.3", "method", 0L);
        Method p2 = new Method("0.99.99", "method", 1L);
        assertEquals(p1.compareTo(p2), 1);
    }

    @Test
    void twoDigitTest() {
        Method p1 = new Method("1.2", "method", 0L);
        Method p2 = new Method("1.4", "method", 1L);
        assertEquals(p1.compareTo(p2), -1);
    }

    @Test
    void twoDigitTestThisIsBigger() {
        Method p1 = new Method("1.4", "method", 0L);
        Method p2 = new Method("1.1", "method", 1L);
        assertEquals(p1.compareTo(p2), 1);
    }

    @Test
    void oneDigitTest() {
        Method p1 = new Method("1", "method", 0L);
        Method p2 = new Method("2", "method", 1L);
        assertEquals(p1.compareTo(p2), -1);
    }
}