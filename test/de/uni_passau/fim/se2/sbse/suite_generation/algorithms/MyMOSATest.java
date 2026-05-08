package de.uni_passau.fim.se2.sbse.suite_generation.algorithms;

import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.ChromosomeGenerator;
import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.MyChromosome;
import de.uni_passau.fim.se2.sbse.suite_generation.fitness_functions.FitnessFunction;
import de.uni_passau.fim.se2.sbse.suite_generation.instrumentation.IBranch;
import de.uni_passau.fim.se2.sbse.suite_generation.selection.Selection;
import de.uni_passau.fim.se2.sbse.suite_generation.stopping_conditions.StoppingCondition;
import de.uni_passau.fim.se2.sbse.suite_generation.utils.Pair;
import de.uni_passau.fim.se2.sbse.suite_generation.utils.Randomness;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MyMOSATest {

    private ChromosomeGenerator<MyChromosome> mockGenerator;
    private FitnessFunction<MyChromosome> mockFitness;
    private StoppingCondition mockStoppingCondition;
    private Selection<MyChromosome> mockSelection;
    private IBranch mockBranch;
    private MyChromosome mockChromo1, mockChromo2;
    private MockedStatic<Randomness> randomnessMock;
    private Random mockRandom;

    @BeforeEach
    void setUp() {
        mockGenerator = mock(ChromosomeGenerator.class);
        mockFitness = mock(FitnessFunction.class);
        mockStoppingCondition = mock(StoppingCondition.class);
        mockSelection = mock(Selection.class);
        mockBranch = mock(IBranch.class);
        mockChromo1 = mock(MyChromosome.class);
        mockChromo2 = mock(MyChromosome.class);
        mockRandom = mock(Random.class);
        randomnessMock = mockStatic(Randomness.class);
        randomnessMock.when(Randomness::random).thenReturn(mockRandom);
        when(mockBranch.getId()).thenReturn(1);
        when(mockChromo1.copy()).thenReturn(mockChromo1);
        when(mockChromo2.copy()).thenReturn(mockChromo2);
        when(mockChromo1.mutate()).thenReturn(mockChromo1);
        when(mockChromo2.mutate()).thenReturn(mockChromo2);
    }

    @AfterEach
    void tearDown() {
        randomnessMock.close();
    }

    @Test
    void shouldReplaceArchivedWithShorterChromosome() {
        when(mockChromo1.size()).thenReturn(10);
        when(mockChromo2.size()).thenReturn(2);
        when(mockChromo1.copy()).thenReturn(mockChromo1);
        when(mockChromo2.copy()).thenReturn(mockChromo2);
        when(mockFitness.applyAsDouble(any())).thenReturn(0.0);
        when(mockGenerator.get()).thenReturn(mockChromo1, mockChromo2);
        MyMOSA<MyChromosome> mosa = new MyMOSA<>(
                mockGenerator,
                List.of(mockFitness),
                mockStoppingCondition,
                mockSelection,
                List.of(mockBranch),
                2
        );

        MyMOSA<MyChromosome> spyMosa = spy(mosa);
        when(spyMosa.searchCanContinue()).thenReturn(true, true, false);
        List<MyChromosome> result = spyMosa.findSolution();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(mockChromo2));
    }


    @Test
    void preferenceSortingShouldHandleDominanceCorrectly() {
        when(mockFitness.applyAsDouble(mockChromo1)).thenReturn(0.1);
        when(mockFitness.applyAsDouble(mockChromo2)).thenReturn(0.8);
        when(mockGenerator.get()).thenReturn(mockChromo1, mockChromo2);
        MyMOSA<MyChromosome> mosa = new MyMOSA<>(mockGenerator, List.of(mockFitness),
                mockStoppingCondition, mockSelection, List.of(mockBranch), 2);

        MyMOSA<MyChromosome> spyMosa = spy(mosa);
        doReturn(true, false).when(spyMosa).searchCanContinue();
        when(mockSelection.apply(any())).thenReturn(mockChromo1);
        List<MyChromosome> result = spyMosa.findSolution();
        assertNotNull(result);
    }

    @Test
    void getStoppingConditionShouldReturnCorrectCondition() {
        MyMOSA<MyChromosome> mosa = new MyMOSA<>(
                mockGenerator,
                List.of(mockFitness),
                mockStoppingCondition,
                mockSelection,
                List.of(mockBranch),
                2
        );
        assertEquals(mockStoppingCondition, mosa.getStoppingCondition());
    }

}