package de.uni_passau.fim.se2.sbse.suite_generation.selection;

import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.MyChromosome;
import de.uni_passau.fim.se2.sbse.suite_generation.utils.Randomness;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RankSelectionTest {

    private Random mockRandom;
    private MockedStatic<Randomness> randomnessMock;
    private Comparator<MyChromosome> fitnessComparator;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        mockRandom = mock(Random.class);
        fitnessComparator = Comparator.comparingDouble(c -> 1.0);
        randomnessMock = mockStatic(Randomness.class);
        randomnessMock.when(Randomness::random).thenReturn(mockRandom);
    }

    @AfterEach
    void tearDown() {
        randomnessMock.close();
    }

    @Test
    void constructorShouldThrowExceptionForInvalidBias() {
        assertThrows(IllegalArgumentException.class, () -> new RankSelection<>(fitnessComparator, 10, 0.9, mockRandom));
    }

    @Test
    void applyShouldThrowExceptionForNullAndSizeMisMatch() {
        RankSelection<MyChromosome> selection = new RankSelection<>(fitnessComparator, 2, 1.5, mockRandom);
        assertThrows(IllegalArgumentException.class, () -> selection.apply(null));
        assertThrows(IllegalArgumentException.class, () -> selection.apply(List.of(mock(MyChromosome.class))));
    }

    @Test
    void applyShouldHandleSingleIndividual() {
        MyChromosome chromosome = mock(MyChromosome.class);
        RankSelection<MyChromosome> selection = new RankSelection<>(fitnessComparator, 1, 1.5, mockRandom);
        assertEquals(chromosome, selection.apply(List.of(chromosome)));
    }

    @Test
    void applyShouldSelectBasedOnRankProbabilities() {
        MyChromosome worst = mock(MyChromosome.class);
        MyChromosome best = mock(MyChromosome.class);
        List<MyChromosome> population = new ArrayList<>(List.of(best, worst));
        Comparator<MyChromosome> comp = (c1, c2) -> c1 == worst ? -1 : 1;
        RankSelection<MyChromosome> selection = new RankSelection<>(comp, 2, 2.0, mockRandom);
        when(mockRandom.nextDouble()).thenReturn(0.5);
        MyChromosome selected = selection.apply(population);
        assertEquals(best, selected);
    }
}