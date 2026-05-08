package de.uni_passau.fim.se2.sbse.suite_generation.crossover;


import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.Chromosome;
import de.uni_passau.fim.se2.sbse.suite_generation.utils.Pair;

import java.util.function.BiFunction;

import static java.util.Objects.requireNonNull;

/**
 * The crossover operator recombines the genetic material of two given chromosomes (acting as
 * parents) and produces two new chromosomes (offspring) as a result. Often, crossover simply
 * chooses a random locus and exchanges the genes before and after that locus between the two
 * parents.
 *
 * @param <C> the type of chromosomes supported by this crossover operator
 * @author Sebastian Schweikl
 * @apiNote Usually, it is desired that the offspring of two chromosomes of type {@code C} is again
 * of the same type {@code C}. This requirement can be enforced at compile time by specifying a
 * recursive type parameter, here: {@code C extends Chromosome<C>}.
 */
@FunctionalInterface
public interface Crossover<C extends Chromosome<C>> extends BiFunction<C, C, Pair<C>> {

    /**
     * A crossover operator that returns the two given parent chromosomes as offspring without
     * actually modifying them.
     *
     * @param <C> the type of chromosomes
     * @return a crossover operator that returns the parents as offspring
     * @apiNote Can be useful for creating dummy chromosomes when writing unit tests.
     */
    static <C extends Chromosome<C>> Crossover<C> identity() {
        return new Crossover<>() {
            @Override
            public Pair<C> apply(final C c, final C d) {
                return Pair.of(c, d).map(C::copy);
            }

            @Override
            public String toString() {
                return "Identity crossover";
            }
        };
    }

    /**
     * Applies this crossover operator to the two given non-null parent chromosomes {@code parent1}
     * and {@code parent2}, and returns the resulting pair of offspring chromosomes.
     * <p>
     * Note: an offspring can equal one of its parents (in terms of {@link Chromosome#equals
     * equals()}). While not an absolute requirement, it is generally advisable parents and offspring
     * be different in terms of reference equality ({@code offspring != parent}) as it tends to
     * simplify the implementation of some search algorithms.
     *
     * @param parent1 a parent
     * @param parent2 another parent
     * @return the offspring formed by applying crossover to the two parents
     * @throws NullPointerException if an argument is {@code null}
     */
    @Override
    Pair<C> apply(final C parent1, final C parent2);

    /**
     * Applies crossover to the given pair of parent chromosomes and returns the resulting pair of
     * offspring chromosomes.
     *
     * @param parents the parent chromosomes
     * @return the offspring formed by applying crossover to the two parents
     * @throws NullPointerException if an argument is {@code null}
     * @apiNote This method is equivalent to {@link #apply(C, C)} but instead of taking the
     * parents as individual arguments it receives them as pair.
     */
    default Pair<C> apply(final Pair<? extends C> parents) {
        return requireNonNull(parents).reduce(this);
    }
}
