package de.uni_passau.fim.se2.sbse.suite_generation.utils;

import de.uni_passau.fim.se2.sbse.suite_generation.algorithms.GeneticAlgorithm;
import de.uni_passau.fim.se2.sbse.suite_generation.algorithms.MyMOSA;
import de.uni_passau.fim.se2.sbse.suite_generation.algorithms.MyRandomSearch;
import de.uni_passau.fim.se2.sbse.suite_generation.algorithms.SearchAlgorithmType;
import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.ChromosomeGenerator;
import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.MyChromosome;
import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.MyChromosomeGenerator;
import de.uni_passau.fim.se2.sbse.suite_generation.crossover.Crossover;
import de.uni_passau.fim.se2.sbse.suite_generation.crossover.MyCrossover;
import de.uni_passau.fim.se2.sbse.suite_generation.fitness_functions.FitnessFunction;
import de.uni_passau.fim.se2.sbse.suite_generation.fitness_functions.MyFitnessFunction;
import de.uni_passau.fim.se2.sbse.suite_generation.instrumentation.IBranch;
import de.uni_passau.fim.se2.sbse.suite_generation.instrumentation.IBranchTracer;
import de.uni_passau.fim.se2.sbse.suite_generation.mutation.Mutation;
import de.uni_passau.fim.se2.sbse.suite_generation.mutation.MyMutation;
import de.uni_passau.fim.se2.sbse.suite_generation.selection.RankSelection;
import de.uni_passau.fim.se2.sbse.suite_generation.selection.Selection;
import de.uni_passau.fim.se2.sbse.suite_generation.stopping_conditions.StoppingCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public class AlgorithmBuilder {
    /**
     * The default source randomness.
     */
    private final Random random;

    /**
     * The stopping condition to use.
     */
    private final StoppingCondition stoppingCondition;

    /**
     * The population size of the genetic algorithm.
     */
    private final int populationSize;

    /**
     * The reflected class for which we want to generate tests.
     */
    private final Class<?> testGenerationTarget;

    /**
     * Reference to the branch tracer that tells which branches have been taken by a test execution.
     */
    private final IBranchTracer branchTracer;

    /**
     * The set of branches that should be covered by the generated test suite.
     */
    private final Set<IBranch> branchesToCover;


    public AlgorithmBuilder(final Random random,
                            final StoppingCondition stoppingCondition,
                            final int populationSize,
                            final String classUnderTest,
                            final String packageUnderTest,
                            final IBranchTracer branchTracer)
            throws IllegalArgumentException {
        this.random = requireNonNull(random);
        this.stoppingCondition = requireNonNull(stoppingCondition);
        this.populationSize = populationSize;

        if (classUnderTest == null || classUnderTest.isBlank()) {
            throw new IllegalArgumentException("No CUT specified");
        }

        if (packageUnderTest == null || packageUnderTest.isBlank()) {
            throw new IllegalArgumentException("No PUT specified");
        }

        // On Windows it might be necessary to explicitly load the (instrumented) class under test.
        final String classToLoad = packageUnderTest + "." + classUnderTest;
        try {
            this.testGenerationTarget = Class.forName(classToLoad);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unable to load class: " + classToLoad, e);
        }

        this.branchTracer = requireNonNull(branchTracer);

        // Important: retrieve the set of branches AFTER the class has been loaded. (Otherwise it
        // would be empty.)
        this.branchesToCover = this.branchTracer.getBranches();
    }

    /**
     * Builds the specified search algorithm using the fields of this class.
     *
     * @param algorithm the algorithm to build
     * @return the algorithm
     */
    public GeneticAlgorithm<?> build(final SearchAlgorithmType algorithm) {
        return switch (algorithm) {
            case RANDOM_SEARCH -> buildRandomSearch();
            case MOSA -> buildMOSA();
        };
    }

    /**
     * Returns an instance of the MOSA search algorithm to generate tests for the target class.
     * The algorithm is constructed using the fields of this class.
     *
     * @return the search algorithm
     */
    private GeneticAlgorithm<?> buildMOSA() {
        MyCrossover<MyChromosome> crossover = new MyCrossover<>();
        MyMutation mutation = new MyMutation(null);
        MyChromosomeGenerator chromosomeGenerator = new MyChromosomeGenerator(testGenerationTarget, mutation, crossover);
        mutation.setGenerator(chromosomeGenerator);
        List<FitnessFunction<MyChromosome>> fitnessFunctions = new ArrayList<>();
        List<IBranch> branchList = new ArrayList<>(branchesToCover);
        for (IBranch branch : branchList) {
            fitnessFunctions.add(new MyFitnessFunction(branch.getId()));
        }
        Selection<MyChromosome> selection = new RankSelection<>(
                new MyFitnessFunction(-1).comparator(),
                populationSize,
                2.0,
                random
        );
        return new MyMOSA<>(
                chromosomeGenerator,
                fitnessFunctions,
                stoppingCondition,
                selection,
                branchList,
                populationSize
        );
        //throw new UnsupportedOperationException("Implement me!");
    }

    /**
     * Returns an instance of the Random Search algorithm to generate tests for the target class.
     * The algorithm is constructed using the fields of this class.
     * <p>
     * Instead of sampling a number of test suites at random and simply returning the best one, we
     * consider all sampled test cases, and reuse the MOSA archive to find the shortest covering
     * ones. These test cases are then returned as the overall result of Random Search.
     *
     * @return the search algorithm
     */
    private GeneticAlgorithm<?> buildRandomSearch() {
        MyCrossover<MyChromosome> crossover = new MyCrossover<>();
        MyMutation mutation = new MyMutation(null);
        MyChromosomeGenerator chromosomeGenerator = new MyChromosomeGenerator(testGenerationTarget, mutation, crossover);
        mutation.setGenerator(chromosomeGenerator);
        List<FitnessFunction<MyChromosome>> fitnessFunctions = new ArrayList<>();
        List<IBranch> branchList = new ArrayList<>(branchesToCover);
        for (IBranch branch : branchesToCover) {
            fitnessFunctions.add(new MyFitnessFunction(branch.getId()));
        }
        return new MyRandomSearch<>(chromosomeGenerator, fitnessFunctions, stoppingCondition, branchList);
        //throw new UnsupportedOperationException("Implement me!");
    }
}
