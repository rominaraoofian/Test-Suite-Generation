package de.uni_passau.fim.se2.sbse.suite_generation.chromosomes;

import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.statements.MyAssignmentStatement;
import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.statements.MyConstructorStatement;
import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.statements.MyMethodStatement;
import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.statements.Statement;
import de.uni_passau.fim.se2.sbse.suite_generation.crossover.Crossover;
import de.uni_passau.fim.se2.sbse.suite_generation.mutation.Mutation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MyChromosomeTest {

    private Mutation<MyChromosome> mockMutation;
    private Crossover<MyChromosome> mockCrossover;

    @BeforeEach
    void setUp() {
        mockMutation = mock(Mutation.class);
        mockCrossover = mock(Crossover.class);
    }

    @Test
    void getStatementsShouldReturnStatementList() {
        List<Statement> statements = new ArrayList<>(List.of(mock(Statement.class)));
        MyChromosome chromosome = new MyChromosome(statements, mockMutation, mockCrossover);
        assertEquals(statements, chromosome.getStatements());
    }

    @Test
    void copyShouldReturnNewInstanceButWithSameStatements() {
        List<Statement> statements = new ArrayList<>(List.of(mock(Statement.class)));
        MyChromosome chromosome = new MyChromosome(statements, mockMutation, mockCrossover);
        MyChromosome copy = chromosome.copy();
        assertEquals(chromosome.getStatements(), copy.getStatements());
    }

    @Test
    void callShouldExecuteStatements(){
        MyConstructorStatement mockConstructor = mock(MyConstructorStatement.class);
        MyMethodStatement mockMethod = mock(MyMethodStatement.class);
        List<Statement> statements = List.of(mockConstructor, mockMethod);
        Object instance = new Object();
        when(mockConstructor.getResult()).thenReturn(instance);
        MyChromosome chromosome = new MyChromosome(statements, mockMutation, mockCrossover);
        chromosome.call();
        verify(mockConstructor, times(1)).run();
    }

    @Test
    void equalsShouldReturnTrueForSameStatements() {
        Statement s1 = mock(Statement.class);
        List<Statement> list1 = List.of(s1);
        List<Statement> list2 = List.of(s1);
        MyChromosome chromo1 = new MyChromosome(list1, mockMutation, mockCrossover);
        MyChromosome chromo2 = new MyChromosome(list2, mockMutation, mockCrossover);
        assertTrue(chromo1.equals(chromo2));
    }

    @Test
    void equalsShouldHandleDifferentObjects() {
        MyChromosome chromosome = new MyChromosome(new ArrayList<>(), mockMutation, mockCrossover);
        assertFalse(chromosome.equals(null));
        assertFalse(chromosome.equals("chromosome"));
    }

    @Test
    void callShouldInjectTargetIntoMethodStatements() {
        MyConstructorStatement mockConstructor = mock(MyConstructorStatement.class);
        MyMethodStatement mockMethod = mock(MyMethodStatement.class);
        List<Statement> statements = List.of(mockConstructor, mockMethod);
        Object instance = new Object();
        when(mockConstructor.getResult()).thenReturn(instance);
        MyChromosome chromosome = new MyChromosome(statements, mockMutation, mockCrossover);
        chromosome.call();
        verify(mockMethod).setTarget(instance);
    }

    @Test
    void callShouldInjectTargetIntoAssignmentStatements() {
        MyConstructorStatement mockConstructor = mock(MyConstructorStatement.class);
        MyAssignmentStatement mockAssignment = mock(MyAssignmentStatement.class);
        List<Statement> statements = List.of(mockConstructor, mockAssignment);
        Object instance = new Object();
        when(mockConstructor.getResult()).thenReturn(instance);
        MyChromosome chromosome = new MyChromosome(statements, mockMutation, mockCrossover);
        chromosome.call();
        verify(mockAssignment).setTarget(instance);
    }

    @Test
    void callShouldExceptionWhenStatementFails() {
        MyConstructorStatement mockConstructor = mock(MyConstructorStatement.class);
        List<Statement> statements = List.of(mockConstructor);
        doThrow(new RuntimeException("Test Error")).when(mockConstructor).run();
        MyChromosome chromosome = new MyChromosome(statements, mockMutation, mockCrossover);
        assertThrows(RuntimeException.class, chromosome::call);
    }

    @Test
    void hashCodeShouldWorkCorrectly() {
        List<Statement> statements = List.of(mock(Statement.class));
        MyChromosome chromosome = new MyChromosome(statements, mockMutation, mockCrossover);
        int initialHash = chromosome.hashCode();
        assertEquals(initialHash, chromosome.hashCode());
    }
    @Test
    void selfShouldReturnCorrectInstance() {
        MyChromosome chromosome = new MyChromosome(new ArrayList<>(), mockMutation, mockCrossover);
        assertEquals(chromosome, chromosome.self());
    }

}