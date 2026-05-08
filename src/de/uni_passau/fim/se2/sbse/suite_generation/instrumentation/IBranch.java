package de.uni_passau.fim.se2.sbse.suite_generation.instrumentation;

/**
 * Represents a branch in the control flow of a program. Every branch is uniquely identified via an
 * ID.
 *
 * @author Sebastian Schweikl
 */
public interface IBranch {

    /**
     * Returns the unique ID of this branch.
     *
     * @return the ID of this branch
     */
    int getId();
}
