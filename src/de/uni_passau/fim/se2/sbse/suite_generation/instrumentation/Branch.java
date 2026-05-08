package de.uni_passau.fim.se2.sbse.suite_generation.instrumentation;

import java.util.Objects;

/**
 * Represents a branch in the control flow of a program. We distinguish between {@link Entry
 * "root branches"} and {@link Decision decision branches}.
 *
 * @author Sebastian Schweikl
 */
abstract class Branch implements IBranch {

    /**
     * The globally unique identifier of this branch.
     */
    private final int id;

    /**
     * Constructs a new branch using the given ID, which must not be negative. It is the
     * responsibility of the caller to ensure that the given ID is unique.
     *
     * @param id the desired ID of the branch
     * @throws IllegalArgumentException if the ID is negative
     */
    Branch(final int id) throws IllegalArgumentException {
        if (id < 0) {
            throw new IllegalArgumentException("ID must not be negative");
        }

        this.id = id;
    }

    /**
     * Returns the ID of this branch.
     *
     * @return the ID
     */
    @Override
    public final int getId() {
        return id;
    }

    /*
     * The following implementations of equals() and hashCode() are appropriate and sufficient for
     * use in subclasses because the only "relevant" field that constitutes our notion of equality
     * is the id of a branch instance. The id is ensured to be globally unique when constructing a
     * new branch in the BranchDistanceMethodVisitor class. By construction, it can never happen
     * that two branches are assigned the same id but represent different branches, and similarly,
     * it can never happen that branches with different ids represent the same branch.
     */

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        final Branch that = (Branch) other;
        return this.getId() == that.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public String toString() {
        return "Branch(" + id + ")";
    }

    /**
     * Represents the "root branch" (i.e., the entry point) of a method, which is taken when the
     * method is invoked. Thus, root branches aren't real branches in the control flow graph of a
     * program (unlike decision branches) but it is still necessary to track them for the means of
     * registering them as coverage goal for test suite generation. Root branches as coverage goals
     * are incentives for the search to invoke methods, which is a necessary prerequisite to cover
     * decision branches (The vast majority of decision branches are located within methods), or
     * methods that do not contain branches at all.
     *
     * @author Sebastian Schweikl
     */
    static class Entry extends Branch {

        /**
         * The name of the method this root branch belongs to.
         */
        private final String methodName;

        /**
         * The descriptor of the method this root branch belongs to.
         */
        private final String descriptor;

        /**
         * The fully qualified name (in internal form) of the class that contains the current branch
         * node.
         */
        private final String className;

        /**
         * Creates a new root branch with the given ID for the specified method. The method is given
         * by the fully qualified name of its owner class, the name of the method itself, and the
         * descriptor of the method (the latter is required to distinguish between overloaded
         * methods).
         *
         * @param id         the ID of the root branch (must not be negative)
         * @param className  the fully qualified name of the class containing the root branch
         * @param methodName the name of the method to which this root branch should belong to
         * @param descriptor the descriptor of the method to which this root branch should belong
         *                   to
         * @throws IllegalArgumentException if one of the arguments is invalid (see above)
         */
        Entry(final int id, final String className, final String methodName,
                final String descriptor) throws IllegalArgumentException {
            super(id);

            if (methodName == null || methodName.isBlank()) {
                throw new IllegalArgumentException("Method name must not be null and not be blank");
            }

            if (descriptor == null || descriptor.isBlank()) {
                throw new IllegalArgumentException("Descriptor must not be null and not be blank");
            }

            if (className == null || className.isBlank()) {
                throw new IllegalArgumentException("Class name must not be null and not be blank");
            }

            this.methodName = methodName;
            this.descriptor = descriptor;
            this.className = className;
        }

        @Override
        public String toString() {
            final String branch = super.toString();
            return String.format("Root%s of %s:%s%s", branch, className, methodName, descriptor);
        }
    }

    /**
     * The {@code true} or {@code false} branch of a {@link Node node of diversion} in the
     * control flow of the program.
     *
     * @author Sebastian Schweikl
     */
    static class Decision extends Branch {

        /**
         * The node of diversion this branch originates from.
         */
        private final Node node;

        /**
         * Stores whether this is a {@code true} branch or a {@code false} branch. {@code true}
         * branches are taken when the branch condition of the source node evaluates to {@code
         * true}. Conversely, {@code false} branches are taken when the branch condition evaluates
         * to {@code false}. In Java byte code, branches are often implemented using jump
         * instructions. In that respect, a {@code true} branch is only taken when a jump is
         * <em>not</em> performed (i.e., the jump condition is {@code false}), and a {@code false}
         * branch is taken when a jump occurs (i.e., the jump condition is {@code true}). This
         * implies that a jump condition has to be the logical negation of the branch condition.
         */
        private final boolean value;

        /**
         * Constructs a new branch using the specified non-negative ID, the non-{@code null} branch
         * node as origin and the value telling whether this is a {@code true} branch or {@code
         * false} branch.
         *
         * @param id    a unique number to identify the branch
         * @param node  the node of diversion the branch originates from
         * @param value whether this is a {@code true} branch or {@code false} branch.
         * @throws IllegalArgumentException if an argument is invalid (see above)
         */
        Decision(final int id, final Node node, final boolean value)
                throws IllegalArgumentException {
            super(id);

            if (node == null) {
                throw new IllegalArgumentException("Only non-null branch nodes permitted");
            }

            this.node = node;
            this.value = value;
        }

        /**
         * Returns the node of diversion this branch originates from.
         *
         * @return the origin
         */
        public Node getNode() {
            return node;
        }

        @Override
        public String toString() {
            final String branch = super.toString();
            return String.format("Decision%s of %s:%s", branch, node, value ? "T" : "F");
        }
    }

    /**
     * A node of diversion in the control flow of a program.
     *
     * @param line      The line in the original Java source file where this branch node is located.
     * @param className The fully qualified name (in internal form) of the class that contains the current branch
     *                  node.
     */
        record Node(int line, String className) {

        /**
         * Constructs a new branch node using the given line number and class name as location
         * information. The line number refers to the original Java source file where the branch is
         * located. Line numbers must be positive. The specified class name must be the fully
         * qualified name (in internal form) of the class that contains the branch node. {@code
         * null} and blank class names are not permitted.
         *
         * @param line      the line number in the original Java source file where the branch node
         *                  is located
         * @param className the name of the class containing the currently considered branch
         * @throws IllegalArgumentException if {@code line} or {@code className} are invalid (see
         *                                  above)
         */
        Node {
            if (line < 1) {
                // Can happen if there's no debugging information (line number table) in the class
                // file.
                throw new IllegalArgumentException("Line number must be positive");
            }

            if (className == null || className.isBlank()) {
                throw new IllegalArgumentException("Class name must not be null and not be blank");
            }

        }

            @Override
            public boolean equals(final Object other) {
                if (this == other) {
                    return true;
                }

                if (other == null || getClass() != other.getClass()) {
                    return false;
                }

                final Node that = (Node) other;
                return this.line() == that.line() &&
                        this.className().equals(that.className());
            }

            @Override
            public int hashCode() {
                return Objects.hash(line(), className());
            }

            @Override
            public String toString() {
                return className + ":" + line;
            }
        }
}
