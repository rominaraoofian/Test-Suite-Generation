package de.uni_passau.fim.se2.sbse.suite_generation.utils;

import java.util.Random;

// This class is a utility class that provides a single source of randomness for the entire application.
public class Randomness {

    /**
     * Upper limit when generating random parameter values for function calls.
     */
    public static final int MAX_INT = (1 << 10) - 1;

    /**
     * Lower limit when generating random parameter values for function calls.
     */
    public static final int MIN_INT = ~MAX_INT;

    // Internal source of randomness.
    private static final Random random = new Random();

    private Randomness() {
        // private constructor to prevent instantiation.
    }

    /**
     * Returns the source of randomness.
     *
     * @return randomness
     */
    public static Random random() {
        return random;
    }
}
