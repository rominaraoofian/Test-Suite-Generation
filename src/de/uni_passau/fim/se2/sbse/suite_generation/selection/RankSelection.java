package de.uni_passau.fim.se2.sbse.suite_generation.selection;


import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.Chromosome;
import de.uni_passau.fim.se2.sbse.suite_generation.utils.Randomness;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;



/**
 * Implements rank selection for populations of a user-specified constant size. The operator works
 * as follows:
 * <ol>
 *     <li>First, the population is sorted by their fitness, according to the comparator this
 *     selection operator was constructed with.</li>
 *     <li>Then, every individual is assigned a rank, starting at rank 1 for the worst
 *     individual and finishing with the highest rank for the best individual. Note that all
 *     individuals get a different rank, even if they have the same fitness value.</li>
 *     <li>Finally, the selection probability is linearly assigned to the individuals according
 *     to their rank.</li>
 * </ol>
 * <p>
 * Rank selection overcomes some issues of fitness-proportionate (roulette-wheel) selection
 * because it is unbiased whether the fittest candidate is, for example, 10 times fitter than the
 * next fittest or just 0.1 times fitter. Furthermore, rank selection provides a degree of control
 * over selective pressure that is not possible with fitness-proportionate selection.
 * <p>
 * In particular, users of this class can choose the extent to which selective pressure is exercised
 * by specifying the so called <em>selection bias</em>. For populations of size {@code N > 1} the
 * best individual is assigned rank {@code N} and the worst individual rank {@code 1}. With this,
 * the selection probability {@code p(i)} for an individual with rank {@code 1 <= i <= N} is
 * computed as follows:
 * <pre>{@code
 * p(i) = (2 - c + 2 * (c - 1) * (i - 1) / (N - 1)) / N
 * }</pre>
 * The parameter {@code 1 < c <= 2} is the user-specified selection bias. Here, {@code p(1) = (2 -
 * c) / N} is the probability of the worst individual to be selected, and {@code p(N) = c / N} the
 * probability of the best individual to be selected. Since all individuals were assigned a
 * different rank, the selection probability is different for every individual, even for these with
 * the same fitness value. Note that once the population size has been set, the rank based selection
 * operator cannot be applied to populations of different size as the computed probabilities would
 * no longer match.
 * <p>
 * By default, when no user-specified selection bias is given, the operator assumes a sane default
 * of {@code c = 2 * N / (N + 1)}. This way,
 * <pre>{@code
 * p(1) =     2 / (N * (N + 1)) = 1 / (1 + 2 + 3 + ... + N)
 * p(i) = i * 2 / (N * (N + 1)) = i / (1 + 2 + 3 + ... + N) = i * p(1)
 * p(N) = N * 2 / (N * (N + 1)) = 2 / (N + 1)
 * }</pre>
 * This means the best individual is {@code N} times as likely to be selected as the worst
 * individual, the second best individual {@code N - 1} times as likely as the worst, and so on.
 * @param <C> the type of chromosomes supported by this operator
 */
public class RankSelection<C extends Chromosome<C>> implements Selection<C> {

    /**
     * Constructs a new rank selection operator that uses the given comparator to rank the
     * individuals that are part of a population with the specified fixed size, and applies the
     * specified selection bias when picking individuals. Note that the selection bias must be in
     * the interval {@code [1, 2]}.
     *
     * @param comparator the comparator for ranking individuals
     * @param size       the size of the population the selection operator should be applied to
     * @param bias       the selection bias to exercise
     * @param random     the source of randomness
     */
    private final Comparator<C> comparator;
    private final int size;
    private final double bias;
    private final Random random;

    public RankSelection(final Comparator<C> comparator, final int size, final double bias, final Random random) {
        if (bias < 1.0 || bias > 2.0) {
            throw new IllegalArgumentException("not in the [1,2] interval");
        }
        this.comparator = comparator;
        this.size = size;
        this.bias = bias;
        this.random = Randomness.random();
        //throw new UnsupportedOperationException("Implement me!");
    }

    /**
     * Chooses an individual from the given population using rank selection.
     *
     * @param population the population of chromosomes from which to select
     * @return the selected individual
     */
    @Override
    public C apply(final List<C> population) {

        if (population == null) {
            throw new IllegalArgumentException("Population is null");
        }
        if (population.isEmpty()){
            throw new IllegalArgumentException("Population is empty");
        }
        if (population.size() != size) {
            throw new IllegalArgumentException("Population size and size are different");
        }
        List<C> sortedPopulation = new ArrayList<>(population);
        sortedPopulation.sort(comparator);
        int numberOfSortedPopulation = sortedPopulation.size();
        if (numberOfSortedPopulation == 1) {
            return sortedPopulation.getFirst();
        }
        double nextProbability = 0.0;
        double randomNumber = random.nextDouble();
        for (int i = 1; i <= numberOfSortedPopulation; i++) {
            double p_i = (2 - bias + 2 * (bias - 1) * (i - 1) / (numberOfSortedPopulation - 1)) / numberOfSortedPopulation;
            nextProbability += p_i;

            if (randomNumber <= nextProbability) {
                return sortedPopulation.get(i - 1);
            }
        }
        return sortedPopulation.get(numberOfSortedPopulation - 1);
        //throw new UnsupportedOperationException("Implement me!");
    }
}
