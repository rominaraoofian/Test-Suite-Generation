package de.uni_passau.fim.se2.sbse.suite_generation.fitness_functions;

import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.MyChromosome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MyFitnessFunctionTest {

    private MyChromosome mockChromosome;
    private final int targetBranch = 10;

    @BeforeEach
    void setUp() {
        mockChromosome = mock(MyChromosome.class);
    }

    @Test
    void shouldReturnNormalizedDistanceWhenBranchIsReached() {
        Map<Integer, Double> distances = new HashMap<>();
        distances.put(targetBranch, 9.0);
        when(mockChromosome.call()).thenReturn(distances);
        MyFitnessFunction fitness = new MyFitnessFunction(targetBranch);
        double result = fitness.applyAsDouble(mockChromosome);
        assertEquals(0.9, result, 0.001);
    }

    @Test
    void shouldReturnTwoWhenBranchIsNotReached() {
        Map<Integer, Double> distances = new HashMap<>();
        distances.put(99, 1.0);
        when(mockChromosome.call()).thenReturn(distances);
        MyFitnessFunction fitness = new MyFitnessFunction(targetBranch);
        double result = fitness.applyAsDouble(mockChromosome);
        assertEquals(2.0, result);
    }

    @Test
    void isMinimizingShouldBeTrue() {
        MyFitnessFunction fitness = new MyFitnessFunction(targetBranch);
        assertTrue(fitness.isMinimizing());
    }
}