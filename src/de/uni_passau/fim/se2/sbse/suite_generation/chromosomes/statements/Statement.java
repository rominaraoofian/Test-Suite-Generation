package de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.statements;

/**
 * Represents a statement of a test case. Conceptually, every {@code Statement} can be mapped to
 * a corresponding statement in the Java language. Statements can be executed.
 *
 * @author Sebastian Schweikl
 */
public interface Statement extends Runnable {

    /**
     * Runs this statement using Java reflection.
     *
     * @see java.lang.reflect
     */
    @Override
    void run();

    /**
     * Returns the string representation of this statement as valid Java code (terminated by a
     * semicolon "{@code ;}").
     *
     * @return the Java code of this statement
     */
    String toString();
}
