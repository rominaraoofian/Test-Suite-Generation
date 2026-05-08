package de.uni_passau.fim.se2.sbse.suite_generation.instrumentation;

import java.util.Map;
import java.util.Set;

/**
 * Interface for tracing branch distances in the control flow of test case executions.
 *
 * @author Sebastian Schweikl
 */
public interface IBranchTracer {

    /**
     * Returns the set of traced branches.
     *
     * @return the set of branches
     */
    Set<IBranch> getBranches();

    /**
     * Returns a traced branch by its ID, or {@code null} if no such branch exists.
     *
     * @param id the ID of the branch to return
     * @return the branch with the specified ID, or {@code null} if there is branch with that ID
     */
    IBranch getBranchById(int id);

    /**
     * Returns the current branching distances. The map uses branch IDs to associate branches with
     * their corresponding distance.
     *
     * @return the current branch distances
     */
    Map<Integer, Double> getDistances();

    /**
     * Clears the recorded branch distances for a new test case execution.
     */
    void clear();
}
