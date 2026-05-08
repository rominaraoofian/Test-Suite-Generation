package de.uni_passau.fim.se2.sbse.suite_generation.algorithms;

import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.ChromosomeGenerator;
import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.MyChromosome;
import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.statements.Statement;
import de.uni_passau.fim.se2.sbse.suite_generation.fitness_functions.FitnessFunction;
import de.uni_passau.fim.se2.sbse.suite_generation.instrumentation.IBranch;
import de.uni_passau.fim.se2.sbse.suite_generation.stopping_conditions.StoppingCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MyRandomSearchTest {

    private ChromosomeGenerator<MyChromosome> mockGenerator;
    private FitnessFunction<MyChromosome> mockFitness;
    private StoppingCondition mockStoppingCondition;
    private IBranch mockBranch;
    private MyChromosome mockChromosome;

    @BeforeEach
    void setUp() {
        mockGenerator = mock(ChromosomeGenerator.class);
        mockFitness = mock(FitnessFunction.class);
        mockStoppingCondition = mock(StoppingCondition.class);
        mockBranch = mock(IBranch.class);
        mockChromosome = mock(MyChromosome.class);
        when(mockBranch.getId()).thenReturn(1);
        when(mockChromosome.copy()).thenReturn(mockChromosome);
        when(mockChromosome.getStatements()).thenReturn(new ArrayList<>(List.of(mock(Statement.class), mock(Statement.class), mock(Statement.class), mock(Statement.class), mock(Statement.class))));
    }

    @Test
    void findSolutionShouldArchiveAndMinimizeChromosome() {
        MyChromosome longChromo = mock(MyChromosome.class);
        MyChromosome shortChromo = mock(MyChromosome.class);
        when(longChromo.getStatements()).thenReturn(List.of(mock(Statement.class), mock(Statement.class)));
        when(shortChromo.getStatements()).thenReturn(List.of(mock(Statement.class)));
        when(longChromo.copy()).thenReturn(longChromo);
        when(shortChromo.copy()).thenReturn(shortChromo);
        MyRandomSearch<MyChromosome> randomSearch = new MyRandomSearch<>(
                mockGenerator,
                List.of(mockFitness),
                mockStoppingCondition,
                List.of(mockBranch)
        );
        MyRandomSearch<MyChromosome> spySearch = spy(randomSearch);
        doReturn(true, true, false).when(spySearch).searchCanContinue();
        when(mockGenerator.get()).thenReturn(longChromo, shortChromo);
        when(mockFitness.applyAsDouble(any())).thenReturn(0.0);
        List<MyChromosome> results = spySearch.findSolution();
        assertEquals(1, results.size());
        assertEquals(shortChromo, results.get(0));
    }

    @Test
    void findSolutionShouldNotArchiveWhenDistanceIsNonZero() {
        MyRandomSearch<MyChromosome> randomSearch = new MyRandomSearch<>(
                mockGenerator,
                List.of(mockFitness),
                mockStoppingCondition,
                List.of(mockBranch)
        );

        MyRandomSearch<MyChromosome> spySearch = spy(randomSearch);
        doReturn(true, false).when(spySearch).searchCanContinue();
        when(mockGenerator.get()).thenReturn(mockChromosome);
        when(mockFitness.applyAsDouble(any())).thenReturn(0.001);
        List<MyChromosome> results = spySearch.findSolution();
        assertTrue(results.isEmpty());
    }

    @Test
    void getStoppingConditionShouldReturnCorrectCondition() {
        MyRandomSearch<MyChromosome> randomSearch = new MyRandomSearch<>(
                mockGenerator,
                List.of(mockFitness),
                mockStoppingCondition,
                List.of(mockBranch)
        );
        assertEquals(mockStoppingCondition, randomSearch.getStoppingCondition());
    }
}