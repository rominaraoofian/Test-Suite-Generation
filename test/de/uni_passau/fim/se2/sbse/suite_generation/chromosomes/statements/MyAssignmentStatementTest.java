package de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.statements;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;

class MyAssignmentStatementTest {

    public static class TestTarget {
        public int intField;
        public String stringField;
        public double doubleField;
        public long longField;
    }

    private TestTarget target;

    @BeforeEach
    void setUp() {
        target = new TestTarget();
    }

    @Test
    void runShouldSetFieldValue() throws Exception {
        Field field = TestTarget.class.getField("intField");
        MyAssignmentStatement stmt = new MyAssignmentStatement(field, target, 42);
        stmt.run();
        assertEquals(42, target.intField);
    }

    @Test
    void runShouldThrowRuntimeException() throws Exception {
        Field field = TestTarget.class.getField("intField");
        MyAssignmentStatement stmt = new MyAssignmentStatement(field, null, 42);

        assertThrows(RuntimeException.class, stmt::run);
    }

    @Test
    void toStringShouldFormatNullCorrectly() throws Exception {
        Field field = TestTarget.class.getField("stringField");
        MyAssignmentStatement stmt = new MyAssignmentStatement(field, target, null);
        assertEquals("obj.stringField = null;", stmt.toString());
    }

    @Test
    void toStringShouldEscapeStrings() throws Exception {
        Field field = TestTarget.class.getField("stringField");
        MyAssignmentStatement stmt = new MyAssignmentStatement(field, target, "a\\b\"c");
        assertEquals("obj.stringField = \"a\\\\b\\\"c\";", stmt.toString());
    }

    @Test
    void toStringShouldAppendSuffixesForDoubleAndLong() throws Exception {
        Field dField = TestTarget.class.getField("doubleField");
        MyAssignmentStatement dStmt = new MyAssignmentStatement(dField, target, 5.5);
        Field lField = TestTarget.class.getField("longField");
        MyAssignmentStatement lStmt = new MyAssignmentStatement(lField, target, 100L);
        assertEquals("obj.doubleField = 5.5d;", dStmt.toString());
        assertEquals("obj.longField = 100L;", lStmt.toString());
    }

    @Test
    void setTargetShouldUpdateTargetInstance() throws Exception {
        Field field = TestTarget.class.getField("intField");
        TestTarget newTarget = new TestTarget();
        MyAssignmentStatement stmt = new MyAssignmentStatement(field, target, 10);
        stmt.setTarget(newTarget);
        stmt.run();
        assertEquals(10, newTarget.intField);
        assertEquals(0, target.intField);
    }
}