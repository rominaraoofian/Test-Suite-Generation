package de.uni_passau.fim.se2.sbse.suite_generation.chromosomes;

import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.statements.MyConstructorStatement;
import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.statements.Statement;
import de.uni_passau.fim.se2.sbse.suite_generation.crossover.Crossover;
import de.uni_passau.fim.se2.sbse.suite_generation.mutation.Mutation;
import de.uni_passau.fim.se2.sbse.suite_generation.utils.Randomness;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MyChromosomeGeneratorTest {

    private Random mockRandom;
    private Mutation<MyChromosome> mockMutation;
    private Crossover<MyChromosome> mockCrossover;
    private MockedStatic<Randomness> randomnessMock;
    private MyChromosomeGenerator generator;

    public static class DummyClass {
        public int publicField;
        private int privateField;
        public DummyClass() {}
        public DummyClass(int x) {}
        public void publicMethod(int x) {}
        private void privateMethod() {}
    }

    @BeforeEach
    void setUp() {
        mockRandom = mock(Random.class);
        mockMutation = mock(Mutation.class);
        mockCrossover = mock(Crossover.class);

        randomnessMock = mockStatic(Randomness.class);
        randomnessMock.when(Randomness::random).thenReturn(mockRandom);

        generator = new MyChromosomeGenerator(DummyClass.class, mockMutation, mockCrossover);
    }

    @AfterEach
    void tearDown() {
        randomnessMock.close();
    }

    @Test
    void getShouldGenerateChromosomeWithStatements() {
        when(mockRandom.nextInt(2)).thenReturn(0);
        when(mockRandom.nextInt(30)).thenReturn(5);
        when(mockRandom.nextBoolean()).thenReturn(true, false);
        when(mockRandom.nextInt(anyInt())).thenReturn(0);
        MyChromosome chromosome = generator.get();
        assertNotNull(chromosome);
        assertTrue(chromosome.getStatements().size() > 1);
        assertInstanceOf(MyConstructorStatement.class, chromosome.getStatements().get(0));
    }


    @Test
    void generateRandomFieldAssignmentShouldReturnNullIfNoPublicFields() {
        class PrivateOnly { private int x; }
        MyChromosomeGenerator privateGen = new MyChromosomeGenerator(PrivateOnly.class, mockMutation, mockCrossover);
        when(mockRandom.nextInt(anyInt())).thenReturn(0);
        Statement stmt = privateGen.generateRandomFieldAssignment(new PrivateOnly());
        assertNull(stmt);
    }

    @Test
    void generateRandomMethodCallShouldHandleSelfReference(){
        class SelfRef { public void test(SelfRef other) {} }
        MyChromosomeGenerator selfGen = new MyChromosomeGenerator(SelfRef.class, mockMutation, mockCrossover);
        when(mockRandom.nextInt(anyInt())).thenReturn(0);
        SelfRef target = new SelfRef();
        Statement stmt = selfGen.generateRandomMethodCall(target);
        assertNotNull(stmt);
    }

    @Test
    void generateRandomSingleParameterShouldCoverAllPrimitives() {
        when(mockRandom.nextDouble()).thenReturn(0.5);
        when(mockRandom.nextInt(anyInt())).thenReturn(10);
        assertTrue(generator.generateRandomSingleParameter(double.class) instanceof Double);
        assertTrue(generator.generateRandomSingleParameter(float.class) instanceof Float);
        assertTrue(generator.generateRandomSingleParameter(long.class) instanceof Long);
        assertEquals((byte) 0, generator.generateRandomSingleParameter(byte.class));
        assertEquals((short) 0, generator.generateRandomSingleParameter(short.class));
        assertEquals(' ', generator.generateRandomSingleParameter(char.class));
    }

    @Test
    void generateRandomStringShouldGenerateCorrectString() {
        when(mockRandom.nextInt(20)).thenReturn(0);
        when(mockRandom.nextInt(126 - 32 + 1)).thenReturn(34 - 32, 92 - 32, 65 - 32);
        String result = generator.generateRandomString();
        assertEquals("A", result);
    }

    @Test
    void generateRandomSingleParameterShouldReturnNullForWrappers() {
        when(mockRandom.nextDouble()).thenReturn(0.01);
        assertNull(generator.generateRandomSingleParameter(Integer.class));
        assertNull(generator.generateRandomSingleParameter(Boolean.class));
        assertNull(generator.generateRandomSingleParameter(String.class));
        assertNull(generator.generateRandomSingleParameter(Double.class));
        assertNull(generator.generateRandomSingleParameter(Float.class));
        assertNull(generator.generateRandomSingleParameter(Long.class));
    }

}