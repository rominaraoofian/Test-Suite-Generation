package de.uni_passau.fim.se2.sbse.suite_generation.stopping_conditions;

/**
 * A stopping condition that defines the search budget in terms of the maximum number of fitness
 * evaluations.
 * <p>
 * Whenever an algorithm samples a new solution and computes its fitness, the stopping condition
 * shall be notified of this. For example, random search samples just one new solution per
 * iteration, and would notify the stopping condition only of fitness evaluations for this newly
 * sampled solution. On the other hand, NSGA-II or MOSA create an entire new offspring population
 * per generation and must compute the fitness values for all chromosomes of the new population.
 * Thus, we would have <i>population size × number of objectives</i> many notifications per
 * generation.
 * <p>
 * Fitness functions and chromosomes may be implemented in such a way that they cache fitness
 * values. This has no impact on the notification of this stopping condition. A random search might
 * sample the same "new" value twice, and would still be notified twice, regardless if the fitness
 * value of this "new" solution has already been cached or not. Caching can have a positive impact
 * on the runtime and thus on the {@code MaxTime} stopping condition, though.
 *
 * @author Sebastian Schweikl
 */
public final class MaxFitnessEvaluations implements StoppingCondition {

    /**
     * The maximum number of fitness evaluations.
     */
    private final int maximumFitnessEvaluations;

    /**
     * The current number of fitness evaluations.
     */
    private int fitnessEvaluations;

    /**
     * Creates a new stopping condition using the given number of fitness evaluations as search
     * budget.
     *
     * @param maxFitnessEvaluations the number of fitness evaluations
     */
    public MaxFitnessEvaluations(final int maxFitnessEvaluations) {
        if (maxFitnessEvaluations <= 0) {
            throw new IllegalArgumentException("fitness evaluations must be positive");
        }
        this.maximumFitnessEvaluations = maxFitnessEvaluations;
        this.fitnessEvaluations = Integer.MAX_VALUE;
    }

    /**
     * Creates a new stopping condition using the given number of fitness evaluations as search
     * budget.
     *
     * @param maxFitnessEvaluations the number of fitness evaluations
     * @return the stopping condition
     */
    public static MaxFitnessEvaluations of(final int maxFitnessEvaluations) {
        return new MaxFitnessEvaluations(maxFitnessEvaluations);
    }

    /**
     * Returns the maximum number of fitness evaluations for this stopping condition.
     *
     * @return the maximum number of fitness evaluations
     */
    public int getMaximumFitnessEvaluations() {
        return maximumFitnessEvaluations;
    }

    @Override
    public void notifySearchStarted() {
        fitnessEvaluations = 0;
    }

    @Override
    public void notifyFitnessEvaluation() {
        fitnessEvaluations++;
    }

    @Override
    public void notifyIteration() {
        // Not relevant for this stopping condition, so do nothing.
    }

    @Override
    public void notifyFitnessEvaluations(final int evaluations) throws IllegalArgumentException {
        if (evaluations < 0) {
            throw new IllegalArgumentException("Negative number of evaluations: " + evaluations);
        }

        fitnessEvaluations += evaluations;
    }

    @Override
    public boolean searchMustStop() {
        return (fitnessEvaluations >= maximumFitnessEvaluations);
    }

    @Override
    public double getProgress() {
        return fitnessEvaluations / (double) maximumFitnessEvaluations;
    }

    @Override
    public String toString() {
        return String.format("%s(%d)", getClass().getSimpleName(), maximumFitnessEvaluations);
    }
}
