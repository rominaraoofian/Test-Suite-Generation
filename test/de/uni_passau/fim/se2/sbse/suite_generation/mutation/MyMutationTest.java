package de.uni_passau.fim.se2.sbse.suite_generation.mutation;

import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.MyChromosome;
import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.MyChromosomeGenerator;
import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.statements.MyConstructorStatement;
import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.statements.Statement;
import de.uni_passau.fim.se2.sbse.suite_generation.crossover.Crossover;
import de.uni_passau.fim.se2.sbse.suite_generation.utils.Randomness;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MyMutationTest {

    private Random mockRandom;
    private MyChromosome parent;
    private MyChromosomeGenerator mockGenerator;
    private MyConstructorStatement mockConstructor;
    private MockedStatic<Randomness> randomnessMock;

    @BeforeEach
    void setUp() {
        mockRandom = mock(Random.class);
        parent = mock(MyChromosome.class);
        mockGenerator = mock(MyChromosomeGenerator.class);
        mockConstructor = mock(MyConstructorStatement.class);
        when(parent.getMutation()).thenReturn(mock(Mutation.class));
        when(parent.getCrossover()).thenReturn(mock(Crossover.class));
        randomnessMock = mockStatic(Randomness.class);
        randomnessMock.when(Randomness::random).thenReturn(mockRandom);
    }

    @AfterEach
    void tearDown() {
        randomnessMock.close();
    }

    @Test
    void applyShouldKeepStatements() {
        List<Statement> statements = new ArrayList<>(List.of(mockConstructor, mock(Statement.class)));
        when(parent.getStatements()).thenReturn(statements);
        when(mockRandom.nextDouble()).thenReturn(0.8, 0.8, 0.8);
        MyMutation mutation = new MyMutation(mockGenerator);
        MyChromosome offspring = mutation.apply(parent);
        assertEquals(2, offspring.getStatements().size());
    }

    @Test
    void applyShouldReplaceStatement() {
        Statement oldStmt = mock(Statement.class);
        Statement newStmt = mock(Statement.class);
        List<Statement> statements = new ArrayList<>(List.of(mockConstructor, oldStmt));
        when(parent.getStatements()).thenReturn(statements);
        when(mockConstructor.getResult()).thenReturn(new Object());
        when(mockRandom.nextDouble()).thenReturn(0.8, 0.1, 0.9);
        when(mockRandom.nextBoolean()).thenReturn(true);
        when(mockGenerator.generateRandomMethodCall(any())).thenReturn(newStmt);
        MyMutation mutation = new MyMutation(mockGenerator);
        MyChromosome offspring = mutation.apply(parent);
        assertEquals(newStmt, offspring.getStatements().get(1));
    }

    @Test
    void applyShouldAddStatements() {
        List<Statement> statements = new ArrayList<>(List.of(mockConstructor));
        Statement addedStmt = mock(Statement.class);
        when(parent.getStatements()).thenReturn(statements);
        when(mockConstructor.getResult()).thenReturn(new Object());
        when(mockRandom.nextDouble()).thenReturn(0.01, 0.9);
        when(mockRandom.nextBoolean()).thenReturn(true);
        when(mockGenerator.generateRandomMethodCall(any())).thenReturn(addedStmt);
        MyMutation mutation = new MyMutation(mockGenerator);
        MyChromosome offspring = mutation.apply(parent);
        assertEquals(2, offspring.getStatements().size());
    }

    @Test
    void applyShouldNotAddMoreThanFiftyStatements() {
        List<Statement> statements = new ArrayList<>();
        statements.add(mockConstructor);
        for(int i=0; i<49; i++) {
            statements.add(mock(Statement.class));
        }
        when(parent.getStatements()).thenReturn(statements);
        when(mockConstructor.getResult()).thenReturn(new Object());
        when(mockRandom.nextDouble()).thenReturn(0.5);
        MyMutation mutation = new MyMutation(mockGenerator);
        MyChromosome offspring = mutation.apply(parent);
        assertEquals(50, offspring.getStatements().size());
    }

    @Test
    void applyShouldDeleteStatement() {
        Statement deleteStatement = mock(Statement.class);
        List<Statement> statements = new ArrayList<>(List.of(mockConstructor, deleteStatement, mock(Statement.class)));
        when(parent.getStatements()).thenReturn(statements);
        when(mockConstructor.getResult()).thenReturn(new Object());
        when(mockRandom.nextDouble()).thenReturn(0.1, 0.8, 0.9, 0.9, 0.9);
        MyMutation mutation = new MyMutation(mockGenerator);
        MyChromosome offspring = mutation.apply(parent);
        assertEquals(2, offspring.getStatements().size());
    }

    @Test
    void applyShouldHandleNullStatement() {
        Statement oldStmt = mock(Statement.class);
        List<Statement> statements = new ArrayList<>(List.of(mockConstructor, oldStmt));
        when(parent.getStatements()).thenReturn(statements);
        when(mockConstructor.getResult()).thenReturn(new Object());
        when(mockRandom.nextDouble()).thenReturn(0.8, 0.1, 0.9);
        when(mockRandom.nextBoolean()).thenReturn(true);
        when(mockGenerator.generateRandomMethodCall(any())).thenReturn(null);
        MyMutation mutation = new MyMutation(mockGenerator);
        MyChromosome offspring = mutation.apply(parent);
        assertEquals(oldStmt, offspring.getStatements().get(1));
    }

    @Test
    void shouldSetGeneratorWork() {
        MyMutation mutation = new MyMutation(mockGenerator);
        MyChromosomeGenerator newGenerator = mock(MyChromosomeGenerator.class);
        mutation.setGenerator(newGenerator);
        assertNotNull(mutation);
    }




}