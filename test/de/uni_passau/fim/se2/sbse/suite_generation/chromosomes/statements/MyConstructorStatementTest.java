package de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.statements;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import static org.junit.jupiter.api.Assertions.*;

class MyConstructorStatementTest {

    public static class DummyClass {
        public String value;
        public DummyClass() {}
        public DummyClass(String s, double d, long l) { this.value = s; }
    }

    @Test
    void runShouldCreateNewInstance() throws Exception {
        Constructor<?> constructor = DummyClass.class.getConstructor();
        MyConstructorStatement stmt = new MyConstructorStatement(constructor, new Object[0]);
        stmt.run();
        assertNotNull(stmt.getResult());
        assertInstanceOf(DummyClass.class, stmt.getResult());
    }

    @Test
    void runShouldThrowRuntimeException() throws Exception {
        Constructor<?> constructor = DummyClass.class.getConstructor(String.class, double.class, long.class);
        MyConstructorStatement stmt = new MyConstructorStatement(constructor, new Object[]{1, 2, 3});
        assertThrows(RuntimeException.class, stmt::run);
    }

    @Test
    void toStringShouldFormatNoArgConstructor() throws Exception {
        Constructor<?> constructor = DummyClass.class.getConstructor();
        MyConstructorStatement stmt = new MyConstructorStatement(constructor, new Object[0]);
        assertEquals("DummyClass obj = new DummyClass();", stmt.toString());
    }

    @Test
    void toStringShouldHandleComplexParameters() throws Exception {
        Constructor<?> constructor = DummyClass.class.getConstructor(String.class, double.class, long.class);
        Object[] params = new Object[]{"a\"b\\c", 5.5, 100L};
        MyConstructorStatement stmt = new MyConstructorStatement(constructor, params);
        String expected = "DummyClass obj = new DummyClass(\"a\\\"b\\\\c\", 5.5d, 100L);";
        assertEquals(expected, stmt.toString());
    }

    @Test
    void toStringShouldHandleNullParameters() throws Exception {
        Constructor<?> constructor = DummyClass.class.getConstructor(String.class, double.class, long.class);
        Object[] params = new Object[]{null, 0.0, 0L};
        MyConstructorStatement stmt = new MyConstructorStatement(constructor, params);
        assertTrue(stmt.toString().contains("(null, 0.0d, 0L);"));
    }
}