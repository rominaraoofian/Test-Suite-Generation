package de.uni_passau.fim.se2.sbse.suite_generation.crossover;

import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.MyChromosome;
import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.statements.Statement;
import de.uni_passau.fim.se2.sbse.suite_generation.mutation.Mutation;

import de.uni_passau.fim.se2.sbse.suite_generation.utils.Pair;
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

class MyCrossoverTest {
    private Random mockRandom;
    private MyChromosome parent1;
    private MyChromosome parent2;
    private Mutation<MyChromosome> mockMutation;
    private Crossover<MyChromosome> mockCrossover;
    private MockedStatic<Randomness> randomnessMock;

    @BeforeEach
    void setUp() {
        mockRandom = mock(Random.class);
        parent1 = mock(MyChromosome.class);
        parent2 = mock(MyChromosome.class);
        mockMutation = mock(Mutation.class);
        mockCrossover = mock(Crossover.class);
        when(parent1.getMutation()).thenReturn(mockMutation);
        when(parent1.getCrossover()).thenReturn(mockCrossover);
        when(parent2.getMutation()).thenReturn(mockMutation);
        when(parent2.getCrossover()).thenReturn(mockCrossover);
        randomnessMock = mockStatic(Randomness.class);
        randomnessMock.when(Randomness::random).thenReturn(mockRandom);
    }

    @AfterEach
    void tearDown() {
        randomnessMock.close();
    }

    @Test
    void shouldReturnCopyOfParent1WhenPointIsZero() {
        when(parent1.getStatements()).thenReturn(List.of(mock(Statement.class)));
        when(parent2.getStatements()).thenReturn(List.of(mock(Statement.class)));
        when(parent1.copy()).thenReturn(parent1);
        when(parent2.copy()).thenReturn(parent2);
        when(mockRandom.nextInt(anyInt())).thenReturn(1);
        MyCrossover<MyChromosome> crossover = new MyCrossover<>();
        Pair<MyChromosome> result = crossover.apply(parent1, parent2);
        assertEquals(parent1, result.getFst());
    }

    @Test
    void crossoverShouldReturnCorrectOffspring() {
        Statement s1 = mock(Statement.class);
        Statement s2 = mock(Statement.class);
        Statement a1 = mock(Statement.class);
        Statement a2 = mock(Statement.class);
        when(parent1.getStatements()).thenReturn(new ArrayList<>(List.of(s1, s2)));
        when(parent2.getStatements()).thenReturn(new ArrayList<>(List.of(a1, a2)));
        when(mockRandom.nextInt(2)).thenReturn(2);
        when(mockRandom.nextInt(1)).thenReturn(0);
        MyCrossover<MyChromosome> crossover = new MyCrossover<>();
        Pair<MyChromosome> offsprings = crossover.apply(parent1, parent2);
        List<Statement> expected = List.of(s1, a2);
        assertEquals(expected, offsprings.getFst().getStatements());
    }

    @Test
    void offspringShouldBeLimitedToFiftyStatements() {
        List<Statement> longList = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            longList.add(mock(Statement.class));
        }
        when(parent1.getStatements()).thenReturn(new ArrayList<>(longList));
        when(parent2.getStatements()).thenReturn(new ArrayList<>(longList));
        when(mockRandom.nextInt(60)).thenReturn(10);
        when(mockRandom.nextInt(9)).thenReturn(4);
        MyCrossover<MyChromosome> crossover = new MyCrossover<>();
        Pair<MyChromosome> offsprings = crossover.apply(parent1, parent2);
        assertEquals(50, offsprings.getFst().getStatements().size());
    }

}