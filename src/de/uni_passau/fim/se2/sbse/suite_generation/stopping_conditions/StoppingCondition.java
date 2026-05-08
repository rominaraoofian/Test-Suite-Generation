package de.uni_passau.fim.se2.sbse.suite_generation.stopping_conditions;

/**
 * A stopping condition defines the budget allotted to a search algorithm. Very often, this budget
 * is defined in terms of number of fitness evaluations (this is useful when comparing algorithms)
 * or wall time. When the budget is exhausted, the search must stop. Some algorithms may choose to
 * stop earlier, e.g., when the best solution has been found or solutions are no longer improving.
 * <p>
 * Search algorithms and stopping conditions follow the Observer pattern. Stopping conditions (the
 * observers) are intended to be passed as parameters to a search algorithm (the subject). Doing so
 * subscribes these stopping conditions to the subject, which then notifies the registered observers
 * of certain events, such as fitness evaluations. Search algorithms query their stopping conditions
 * to find whether
 *
 * @author Sebastian Schweikl
 */
public interface StoppingCondition {

    /**
     * Notifies this stopping condition that the search has started. Intended to be called by the
     * search algorithm the stopping condition is subscribed to.
     */
    void notifySearchStarted();

    /**
     * Notifies this stopping condition that a fitness evaluation took place. Intended to be called
     * by the search algorithm the stopping condition is subscribed to.
     */
    void notifyFitnessEvaluation();

    /**
     * Notifies this stopping condition that one iteration of the algorithm has been performed.
     * The exact meaning of "iteration" depends on the search algorithm at hand. For example, for
     * NSGA2 one iteration equals one generation while for Random Search one iteration means taking
     * one sample. Intended to be called by the search algorithm the stopping condition is
     * subscribed to.
     */
    void notifyIteration();

    /**
     * Notifies this stopping condition that a number of fitness evaluations took place. Intended to
     * be called by the search algorithm the stopping condition is subscribed to.
     *
     * @param evaluations the number of evaluations, must not be negative
     * @throws IllegalArgumentException if the given number of evaluations is negative
     */
    default void notifyFitnessEvaluations(final int evaluations) throws IllegalArgumentException {
        if (evaluations < 0) {
            throw new IllegalArgumentException("Negative number of evaluations: " + evaluations);
        }

        for (int i = 0; i < evaluations; i++) {
            notifyFitnessEvaluation();
        }
    }

    /**
     * Tells whether the search algorithm must stop, i.e., the search budget has been exhausted. The
     * inverse of {@code searchCanContinue()}.
     *
     * @return {@code true} if the search must stop, {@code false} otherwise
     */
    boolean searchMustStop();

    /**
     * Tells whether the search is allowed to continue, i.e., there is still search budget left. The
     * inverse of {@code searchMustStop()}.
     *
     * @return {@code true} if the search can continue, {@code false} otherwise
     */
    default boolean searchCanContinue() {
        return !searchMustStop();
    }

    /**
     * Returns how much search budget has already been consumed by the search. The returned value
     * should be a percentage, i.e., a value in the interval [0,1]. But this is not an absolute
     * requirement, and implementations might choose to return different values if it makes sense
     * for them. In this case, however, it is recommended to clearly document their behavior.
     *
     * @return the amount of search budget consumed
     */
    double getProgress();
}
