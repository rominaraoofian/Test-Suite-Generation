package de.uni_passau.fim.se2.sbse.suite_generation;


import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.Chromosome;
import de.uni_passau.fim.se2.sbse.suite_generation.chromosomes.statements.Statement;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.IntSummaryStatistics;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.summarizingInt;

/**
 * A test suite consists of a set of test cases. In turn, every test case consists of a list of
 * statements.
 *
 * @author Sebastian Schweikl
 */
class TestSuite {

    /**
     * Default indentation.
     */
    private static final String indentation = " ".repeat(4);

    /**
     * Default test annotation.
     */
    private static final String testAnnotation = "@Test";

    /**
     * System-dependent line separator.
     */
    private static final String newLine = System.lineSeparator();

    /**
     * Statement-terminator.
     */
    private static final String terminator = ";" + newLine;

    /**
     * By convention, the name of the test suite for a class {@code Foo} is {@code FooTest}.
     */
    private static final String testSuiteSuffix = "Test";

    /**
     * Prefix for test case names.
     */
    private static final String testCasePrefix = "test";

    /**
     * Where test suites are put.
     */
    private static final String testSourcesRoot = "test";

    /**
     * Default set of imports required to run a test suite.
     */
    private static final String[] imports = {
            "org.junit.jupiter.api.Assertions",
            "org.junit.jupiter.api.Test"
    };

    /**
     * Current test suite count (i.e., how many test suites have we generated and written to disk
     * so far.)
     */
    private static int currentTestSuite = 0;

    /**
     * List of test cases (in chromosome representation) the test suite comprises.
     */
    private final List<? extends Chromosome<?>> testCases;

    /**
     * The branch coverage achieved by the test suite.
     */
    private final double branchCoverage;

    /**
     * Name of the class under test.
     */
    private final String classUnderTest;

    /**
     * Name of the package under test.
     */
    private final String packageUnderTest;

    /**
     * Name of the test suite for the class under test.
     */
    private final String testSuiteName;

    /**
     * Builder to turn the chromosome representation of a test suite into Java source code.
     */
    private final TestSuiteCodeBuilder builder;

    /**
     * Path to the test suite in the filesystem.
     */
    private final String testSuitePath;

    /**
     * Creates a new test suite for the given class under test in the package under test using the
     * given list of test cases achieving the specified coverage.
     *
     * @param classUnderTest   the class under test
     * @param packageUnderTest package where the class under test is located
     * @param testCases        the test cases the test suite shall comprise
     * @param branchCoverage   the branch coverage achieved by the test cases
     * @throws IllegalArgumentException if the list of test cases is empty
     * @throws NullPointerException     if an argument is {@code null}
     */
    TestSuite(final String classUnderTest,
              final String packageUnderTest,
              final List<? extends Chromosome<?>> testCases,
              final double branchCoverage)
            throws IllegalArgumentException, NullPointerException {
        requireNonNull(testCases);
        if (testCases.isEmpty()) {
            throw new IllegalArgumentException("require at least one test case");
        }
        this.testCases = testCases;

        this.classUnderTest = requireNonNull(classUnderTest);
        this.packageUnderTest = requireNonNull(packageUnderTest);

        this.branchCoverage = branchCoverage;

        requireNonNull(classUnderTest);
        this.testSuiteName = getTestSuiteName(classUnderTest);

        this.builder = new TestSuiteCodeBuilder();
        this.testSuitePath = String.format("%s/%s/%s.java",
                testSourcesRoot, packageUnderTest.replace('.', '/'), testSuiteName);
    }

    /**
     * Generates a fresh name for the test suite we're about to write to disk.
     *
     * @param classUnderTest the name of the class under test for which to generate the test suite
     * @return a fresh name for the test suite
     */
    private static String getTestSuiteName(final String classUnderTest) {
        return classUnderTest + testSuiteSuffix + (currentTestSuite++);
    }

    /**
     * Writes the test suite to disk, after prepending the given string as single-line comment to
     * the test suite code.
     *
     * @param singleLineHeader single line header
     */
    void write(final String singleLineHeader) {
        final var testSuiteFile = new File(testSuitePath);
        final var testSuiteDir = testSuiteFile.getParentFile();
        if (!testSuiteDir.exists()) {
            testSuiteDir.mkdirs();
        }

        final String code = String.format("// %s%n%s", singleLineHeader, builder.toCode());
        try (final var fw = new FileWriter(testSuiteFile)) {
            fw.write(code);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the branch coverage achieved by this test suite.
     *
     * @return the branch coverage
     */
    public double getBranchCoverage() {
        return branchCoverage;
    }

    /**
     * Returns the number of test cases in this test suite.
     *
     * @return the number of test cases
     */
    public int getTestCount() {
        return testCases.size();
    }

    /**
     * Creates statistics about the number of statements per test case.
     *
     * @return statement statistics
     */
    public IntSummaryStatistics getStatementStats() {
        return testCases.stream().collect(summarizingInt(t -> t.getStatements().size()));
    }

    /**
     * Utility class for turning a {@code TestSuite} into Java source code.
     */
    private class TestSuiteCodeBuilder {

        /**
         * A unique number for the currently considered test case of the current test suite.
         */
        private int currentTestCase = 0;

        private StringBuilder sb;

        private String nextTestCaseSignature() {
            return "void " + testCasePrefix + (currentTestCase++) + "()";
        }

        private void appendPackageDeclaration() {
            sb.append("package ").append(packageUnderTest).append(terminator);
            sb.append(newLine);
        }

        private void appendImports() {
            sb.append("import ").append(packageUnderTest).append('.').append(classUnderTest)
                    .append(terminator);
            for (final String imp : imports) {
                sb.append("import ").append(imp).append(terminator);
            }
            sb.append(newLine);
        }

        private void beginClassDeclaration() {
            sb.append("class ").append(testSuiteName).append(" {").append(newLine).append(newLine);
        }

        private void appendTestCase(final Chromosome<?> testCase) {
            sb.append(indentation).append(testAnnotation).append(newLine);
            sb.append(indentation).append(nextTestCaseSignature()).append(" {").append(newLine);
            for (final Statement statement : testCase) {
                sb.append(indentation).append(indentation).append(statement).append(newLine);
            }
            sb.append(indentation).append('}').append(newLine).append(newLine);
        }

        private void appendTestCases() {
            testCases.forEach(this::appendTestCase);
        }

        private void endClassDeclaration() {
            sb.append('}');
        }

        /**
         * Turns this test suite into Java Code.
         *
         * @return the test suite as Java code
         */
        private String toCode() {
            sb = new StringBuilder();
            appendPackageDeclaration();
            appendImports();
            beginClassDeclaration();
            appendTestCases();
            endClassDeclaration();
            return sb.toString();
        }
    }
}
