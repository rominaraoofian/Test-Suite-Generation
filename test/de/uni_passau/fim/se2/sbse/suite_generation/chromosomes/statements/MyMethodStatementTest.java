package de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.statements;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

class MyMethodStatementTest {

    public static class DummyClass {
        public boolean methodCalled = false;
        public void testMethod(String s, double d, long l) { methodCalled = true; }
        public void selfRefMethod(DummyClass other) {}
    }

    private DummyClass target;

    @BeforeEach
    void setUp() {
        target = new DummyClass();
    }

    @Test
    void runShouldInvokeMethod() throws Exception {
        Method method = DummyClass.class.getMethod("testMethod", String.class, double.class, long.class);
        MyMethodStatement stmt = new MyMethodStatement(method, target, new Object[]{"test", 1.0, 1L});
        stmt.run();
        assertTrue(target.methodCalled);
    }

    @Test
    void runShouldThrowRuntimeException() throws Exception {
        Method method = DummyClass.class.getMethod("testMethod", String.class, double.class, long.class);
        MyMethodStatement stmt = new MyMethodStatement(method, null, new Object[]{"test", 1.0, 1L});
        assertThrows(RuntimeException.class, stmt::run);
    }

    @Test
    void toStringShouldHandleComplexParameters() throws Exception {
        Method method = DummyClass.class.getMethod("testMethod", String.class, double.class, long.class);
        Object[] params = new Object[]{"a\"b\\c", 5.5, 100L};
        MyMethodStatement stmt = new MyMethodStatement(method, target, params);
        assertEquals("obj.testMethod(\"a\\\"b\\\\c\", 5.5d, 100L);", stmt.toString());
    }

    @Test
    void toStringShouldHandleSelfReference() throws Exception {
        Method method = DummyClass.class.getMethod("selfRefMethod", DummyClass.class);
        MyMethodStatement stmt = new MyMethodStatement(method, target, new Object[]{target});
        assertEquals("obj.selfRefMethod(obj);", stmt.toString());
    }

    @Test
    void toStringShouldHandleNullParameters() throws Exception {
        Method method = DummyClass.class.getMethod("testMethod", String.class, double.class, long.class);
        Object[] params = new Object[]{null, 0.0, 0L};
        MyMethodStatement stmt = new MyMethodStatement(method, target, params);
        assertTrue(stmt.toString().contains("(null, 0.0d, 0L);"));
    }

    @Test
    void setTargetShouldUpdateTargetInstance() throws Exception {
        Method method = DummyClass.class.getMethod("testMethod", String.class, double.class, long.class);
        DummyClass newTarget = new DummyClass();
        MyMethodStatement stmt = new MyMethodStatement(method, target, new Object[]{"", 0.0, 0L});
        stmt.setTarget(newTarget);
        stmt.run();
        assertTrue(newTarget.methodCalled);
        assertFalse(target.methodCalled);
    }
}