package de.uni_passau.fim.se2.sbse.suite_generation;

import de.uni_passau.fim.se2.sbse.suite_generation.algorithms.GeneticAlgorithm;
import de.uni_passau.fim.se2.sbse.suite_generation.algorithms.SearchAlgorithmType;
import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.Chromosome;
import de.uni_passau.fim.se2.sbse.suite_generation.instrumentation.BranchTracer;
import de.uni_passau.fim.se2.sbse.suite_generation.instrumentation.InstrumentingAgent;
import de.uni_passau.fim.se2.sbse.suite_generation.stopping_conditions.MaxFitnessEvaluations;
import de.uni_passau.fim.se2.sbse.suite_generation.utils.AlgorithmBuilder;
import de.uni_passau.fim.se2.sbse.suite_generation.utils.Randomness;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.IntSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.Callable;

import static java.util.stream.Collectors.*;


public class Main implements Callable<Integer> {

    private int populationSize;

    @CommandLine.Option(
            names = {"-c", "--class"},
            description = "The name of the class under test.",
            required = true)
    private String className;

    @CommandLine.Option(
            names = {"-p", "--package"},
            description = "The package containing the class under test.",
            defaultValue = "de.uni_passau.fim.se2.sbse.suite_generation.examples")
    private String packageName;

    @CommandLine.Option(
            names = {"-f", "--max-evaluations"},
            description = "The maximum number of fitness evaluations each algorithm should perform.",
            defaultValue = "500")
    private int maxEvaluations;

    @CommandLine.Option(
            names = {"-z", "--size"},
            description = "The population size of the genetic algorithm.",
            defaultValue = "10")
    private void setSize(int size) {
        if (size < 2) {
            throw new IllegalArgumentException("Population size too small: " + size);
        }

        if ((size & 1) != 0) {
            throw new IllegalArgumentException("Population size must be even: " + size);
        }

        this.populationSize = size;
    }

    @CommandLine.Option(
            names = {"-r", "--repetitions"},
            description = "The number of search repetitions to perform.",
            defaultValue = "10")
    private int repetitions;

    @CommandLine.Option(
            names = {"-s", "--seed"},
            description = "Use a fixed RNG seed.")
    private void setSeed(int seed) {
        Randomness.random().setSeed(seed);
    }

    @CommandLine.Parameters(
            paramLabel = "algorithms",
            description = "The search algorithms to use.",
            arity = "1...",
            converter = AlgorithmConverter.class)
    private List<SearchAlgorithmType> algorithms;


    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    /**
     * Executes the specified search algorithms and prints a summary of the obtained results.
     *
     * @return The exit code of the application
     */
    public Integer call() {
        if (!InstrumentingAgent.isInstrumented()) {
            System.err.println("No instrumentation was performed");
            return 1;
        }

        AlgorithmBuilder builder = new AlgorithmBuilder(Randomness.random(), new MaxFitnessEvaluations(maxEvaluations),
                populationSize, className, packageName, BranchTracer.getInstance());
        for (final var algorithm : algorithms) {
            System.out.println("Running " + algorithm);
            final var search = builder.build(algorithm);
            final long start = System.currentTimeMillis();
            final var testSuites = repeat(search);
            final long duration = System.currentTimeMillis() - start;
            write(search, algorithm, testSuites, duration);
        }

        // Required to make sure that all threads (test case executions) are terminated.
        System.exit(0);
        return 0;
    }

    private List<TestSuite> repeat(final GeneticAlgorithm<?> search) {
        final var testSuites = new ArrayList<TestSuite>(repetitions);

        for (int i = 0; i < repetitions; i++) {
            System.out.println("Repetition " + (i + 1) + " of " + repetitions);
            final var testCases = search.findSolution();
            final double branchCoverage = computeBranchCoverage(testCases);
            final var testSuite = new TestSuite(className, packageName, testCases, branchCoverage);
            testSuites.add(testSuite);
        }

        return testSuites;
    }

    /**
     * Computes the cumulated branch coverage of the given test cases.
     *
     * @param testCases the test cases for which to compute coverage
     * @return the branch coverage of the given test cases
     */
    private double computeBranchCoverage(final List<? extends Chromosome<?>> testCases) {
        BranchTracer.getInstance().clear();

        final var distances = new LinkedHashMap<Integer, Double>();

        for (final var testCase : testCases) {
            try {
                testCase.call();
                for (final var entry : BranchTracer.getInstance().getDistances().entrySet()) {
                    final int branchID = entry.getKey();
                    final double distance = entry.getValue();
                    distances.merge(branchID, distance, Math::min);
                }
            } catch (RuntimeException e) {
                System.err.printf("Erroneous test case:%n%s", testCase);
                e.printStackTrace();
            }
        }

        final long coveredBranches = distances.values().stream()
                .filter(d -> d == 0.0)
                .count();

        final double totalBranches = BranchTracer.getInstance().getBranches().size();
        return (double) coveredBranches / totalBranches;
    }

    private void write(GeneticAlgorithm<?> search, SearchAlgorithmType algorithm, List<TestSuite> testSuites, long duration) {
        final String header = "Test suite generated using " + algorithm;
        testSuites.forEach(t -> t.write(header));
        final String statistics = makeStatisticsString(search, algorithm, testSuites, duration);
        System.out.println(statistics);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
     private String makeStatisticsString(
            final GeneticAlgorithm<?> search,
            final SearchAlgorithmType algorithm,
            final List<TestSuite> testSuites,
            final long duration) {
        final var coverageStats = testSuites.stream()
                .collect(summarizingDouble(TestSuite::getBranchCoverage));
        final double maxCoverage = coverageStats.getMax();
        final double minCoverage = coverageStats.getMin();
        final double avgCoverage = coverageStats.getAverage();
        final double avgRuntime = (double) duration / (double) repetitions;

        final var sb = new StringBuilder("Statistics for ").append(algorithm)
                .append(" and ").append(className).append("\n");

        sb.append(" > Average runtime: ").append(formatTime(avgRuntime)).append("\n");
        sb.append(" > Coverage:\n")
                .append("   * min: ").append(minCoverage).append('\n')
                .append("   * avg: ").append(avgCoverage).append('\n')
                .append("   * max: ").append(maxCoverage).append('\n');

        sb.append(" > Consumed Search Budget: ").append(search.getStoppingCondition().getProgress()).append('\n');

        final var testCountStats = testSuites.stream()
                .collect(summarizingInt(TestSuite::getTestCount));
        final int maxTestCases = testCountStats.getMax();
        final int minTestCases = testCountStats.getMin();
        final double avgTestCases = testCountStats.getAverage();

        final var stmtStats = testSuites.stream()
                .map(TestSuite::getStatementStats).toList();

        final var stmtCountStats = stmtStats.stream()
                .collect(summarizingLong(IntSummaryStatistics::getSum));
        final long minStatementsPerSuite = stmtCountStats.getMin();
        final long maxStatementsPerSuite = stmtCountStats.getMax();
        final double avgStatementsPerSuite = stmtCountStats.getAverage();

        final long minStatementsPerTestCase = stmtStats.stream()
                .mapToInt(IntSummaryStatistics::getMin)
                .min()
                .getAsInt(); // always present by construction

        final long maxStatementsPerTestCase = stmtStats.stream()
                .mapToInt(IntSummaryStatistics::getMax)
                .max()
                .getAsInt(); // always present by construction

        final double avgStatementsPerTestCase = stmtStats.stream()
                .mapToDouble(IntSummaryStatistics::getAverage)
                .average()
                .getAsDouble(); // always present by construction

        sb.append(" > Number of Test Cases:\n")
                .append("   * min: ").append(minTestCases).append('\n')
                .append("   * avg: ").append(avgTestCases).append('\n')
                .append("   * max: ").append(maxTestCases).append('\n');

        sb.append(" > Number of Statements per Test Suite:\n")
                .append("   * min: ").append(minStatementsPerSuite).append('\n')
                .append("   * avg: ").append(avgStatementsPerSuite).append('\n')
                .append("   * max: ").append(maxStatementsPerSuite).append('\n');

        sb.append(" > Number of Statements per Test Case:\n")
                .append("   * min: ").append(minStatementsPerTestCase).append('\n')
                .append("   * avg: ").append(avgStatementsPerTestCase).append('\n')
                .append("   * max: ").append(maxStatementsPerTestCase).append('\n');

        return sb.toString();
    }

    /**
     * Formats the given duration in milliseconds as {@code HH:MM:SS:ssss}.
     *
     * @param durationInMillis the duration in milliseconds
     * @return the formatted duration
     */
    private static String formatTime(final long durationInMillis) {
        final long millis = durationInMillis % 1000;
        final long seconds = (durationInMillis / 1000) % 60;
        final long minutes = (durationInMillis / (1000 * 60)) % 60;
        final long hours = (durationInMillis / (1000 * 60 * 60)) % 24;
        return String.format("%02d:%02d:%02d.%d", hours, minutes, seconds, millis);
    }

    /**
     * Formats the given duration in milliseconds as {@code HH:MM:SS:ssss}. The fractional part of
     * the duration is removed.
     *
     * @param fractionalMillis the duration
     * @return the formatted duration
     */
    private static String formatTime(final double fractionalMillis) {
        return formatTime((long) fractionalMillis);
    }

}


/**
 * Converts supplied cli parameters to the respective {@link SearchAlgorithmType}.
 */
final class AlgorithmConverter implements CommandLine.ITypeConverter<SearchAlgorithmType> {
    @Override
    public SearchAlgorithmType convert(String algorithm) {
        return switch (algorithm.toUpperCase()) {
            case "RS" -> SearchAlgorithmType.RANDOM_SEARCH;
            case "MOSA" -> SearchAlgorithmType.MOSA;
            default -> throw new IllegalArgumentException("The algorithm '" + algorithm + "' is not a valid option.");
        };
    }
}
