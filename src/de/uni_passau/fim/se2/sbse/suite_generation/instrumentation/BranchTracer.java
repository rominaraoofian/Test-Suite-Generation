package de.uni_passau.fim.se2.sbse.suite_generation.instrumentation;

import de.uni_passau.fim.se2.sbse.suite_generation.instrumentation.Branch.Decision;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.*;

import static de.uni_passau.fim.se2.sbse.suite_generation.instrumentation.Branch.Entry;
import static java.util.Objects.requireNonNull;
import static org.objectweb.asm.Opcodes.*;

/**
 * Facilitates the instrumentation of Java byte codes with the aim of tracing decision and root
 * branches in the control flow, and computing the distances for these branches in the instrumented
 * code.
 * <p>
 * The branch distance is a heuristic to evaluate how far a predicate (in our case as part of a
 * conditional jump instruction) is away from obtaining its opposite value. Naturally, the branch
 * distance is computed on the node of diversion in the control flow graph. Since the branching
 * distance is merely a heuristic, there can be different ways to define it. Commonly, we compute
 * the absolute difference between the operands of the predicate, and add some arbitrary but
 * positive constant value to it if desired. This approach guarantees that the branching distance is
 * never negative.
 * <p>
 * The Java Virtual Machine has distinct sets of instructions that conditionally branch on
 * comparisons with data of type {@code int} and reference types. Conditional branches on
 * comparisons between data of types {@code boolean}, {@code byte}, {@code char}, and {@code short}
 * are performed using {@code int} comparison instructions. A conditional branch on a comparison
 * between data of types {@code long}, {@code float}, or {@code double} is initiated using an
 * instruction that compares the data and produces an {@code int} result of the comparison (this is
 * similar in principle to the workings of methods like {@link Comparable#compareTo(Object)
 * compareTo()}). A subsequent {@code int} comparison instruction tests this result and effects the
 * conditional branch.
 * <p>
 * As a consequence, and without loss of generality, we can limit ourselves to the following kinds
 * of control transfer instructions:
 * <ul>
 *     <li>unary comparisons of an {@code int} with 0,</li>
 *     <li>binary comparisons of two arbitrary {@code int}s,</li>
 *     <li>unary comparisons of a reference with {@code null}, and</li>
 *     <li>binary comparisons of two arbitrary references.</li>
 * </ul>
 * <p>
 * This gives rise to the following definition of branch distance:
 * <table>
 * <tr><th>Predicate</td><th>Result</th><th>Distance True</th><th>Distance False</th></tr>
 * <tr><td>{@code i == j}</td><td>{@code true} </td><td>{@code 0}</td><td>{@code 1}</td></tr>
 * <tr><td>{@code i == j}</td><td>{@code false}</td><td>{@code |i - j|}</td><td>{@code 0}</td></tr>
 * <tr><td>{@code i != j}</td><td>{@code true} </td><td>{@code 0}</td><td>{@code |i - j|}</td></tr>
 * <tr><td>{@code i != j}</td><td>{@code false}</td><td>{@code 1}</td><td>{@code 0}</td></tr>
 * <tr><td>{@code i < j}</td><td>{@code true} </td><td>{@code 0}</td><td>{@code j - i}</td></tr>
 * <tr><td>{@code i < j}</td><td>{@code false}</td><td>{@code i - j + 1}</td><td>{@code 0}</td></tr>
 * <tr><td>{@code i <= j}</td><td>{@code true}</td><td>{@code 0}</td><td>{@code j - i + 1}</td></tr>
 * <tr><td>{@code i <= j}</td><td>{@code false}</td><td>{@code i - j}</td><td>{@code 0}</td></tr>
 * <tr><td>{@code i > j}</td><td colspan="3">See Branch Distance for {@code j < i}</td></tr>
 * <tr><td>{@code i >= j}</td><td colspan="3">See Branch Distance for {@code j <= i}</td></tr>
 * <tr><td>{@code o == p}</td><td>{@code true} </td><td>{@code 0}</td><td>{@code 1}</td></tr>
 * <tr><td>{@code o == p}</td><td>{@code false}</td><td>{@code 1}</td><td>{@code 0}</td></tr>
 * <tr><td>{@code o != p}</td><td>{@code true} </td><td>{@code 0}</td><td>{@code 1}</td></tr>
 * <tr><td>{@code o != p}</td><td>{@code false}</td><td>{@code 1}</td><td>{@code 0}</td></tr>
 * </table>
 * where {@code i} and {@code j} are of type {@code int}, and {@code o} and {@code p} are
 * references.
 * <p>
 * Root branches are also handled by this class as they are of particular interest in the context
 * of automated test generation (esp. to handle methods without conditional branches). Therefore, we
 * also consider the root branch of every method for distance computation. To this, we define the
 * branch distance of a root branch as {@code 0} if the branch has been taken (i.e., the method has
 * been called), and {@code 1} otherwise.
 * <p>
 * It is desirable to keep the branch distance computation as lightweight as possible in order not
 * to burden the instrumented code with too much runtime overhead. For example, many expensive calls
 * to {@code Math.abs()} can be replaced with cheap subtractions (as has been done in the table
 * above) when it can be guaranteed that the result will always be non-negative. In addition, one
 * should avoid deeply nested calls and expensive recursive calls whenever possible. In general, one
 * has to carefully weigh which language features to use. If in doubt, one should construct a
 * minimum working example and examine the generated byte code using {@code javap -c} or similar
 * tools.
 */
public final class BranchTracer implements IBranchTracer {

    /**
     * The fully qualified name of this class in internal form, where dots ("{@code .}") are
     * replaced with slashes ("{@code /}").
     */
    private static final String BRANCH_TRACER = Type.getInternalName(BranchTracer.class);

    /**
     * Types required to construct method descriptors.
     */
    private static final Type
            VOID_T = Type.VOID_TYPE,
            INT_T = Type.INT_TYPE,
            OBJ_T = Type.getType(Object.class),
            BRANCH_TRACER_T = Type.getType(BranchTracer.class);

    /**
     * The name of the method the instrumented code should invoke when a branch was taken,
     * {@link #passedBranch(int)}.
     */
    private static final String PASSED_BRANCH = "passedBranch";

    /**
     * The name of the method {@link #getInstance()}.
     */
    private static final String GET_INSTANCE = "getInstance";

    /**
     * Descriptor for {@link #getInstance()};
     */
    private static final String GET_INSTANCE_D = Type.getMethodDescriptor(BRANCH_TRACER_T);

    /**
     * Stores the branches for which we want to compute branch distance. A branch can be retrieved
     * via its ID. That is, for a valid key {@code id} it holds that
     * <pre>{@code
     * branches.get(id).getId() == id
     * }</pre>
     */
    private final Map<Integer, IBranch> branches = new LinkedHashMap<>();

    /**
     * Stores the actual branch distances for the branches. Can be retrieved via the ID of the
     * branch. Whether the retrieved distance is a {@code true} distance or {@code false} distance
     * depends on the branch at hand is a {@code true} branch or {@code false} branch.
     */
    private final Map<Integer, Double> distances = new LinkedHashMap<>();

    private BranchTracer() {
        // Private constructor to prevent instantiation of class.
    }

    /**
     * Uses the given non-{@code null} method visitor to insert instructions into the byte codes of
     * methods so that the instrumented code computes the branching distance of the specified {@code
     * true} branch and {@code false} branch. Both branches must share the same branch node as
     * origin, and it is assumed that the method visitor is currently visiting this node. The
     * original control transfer instruction {@code opcode} is retained in the instrumented code in
     * order not to change the original behavior of the program.
     *
     * @param mv          the method visitor with which to instrument
     * @param trueBranch  the {@code true} branch for which to instrument
     * @param falseBranch the {@code false} branch for which to instrument
     * @param opcode      the control transfer instruction of the branch node
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if the {@code trueBranch} and {@code falseBranch} have
     *                                  different origins
     */
    void instrumentBranchNode(
            final MethodVisitor mv,
            final Decision trueBranch,
            final Decision falseBranch,
            final int opcode) {
        requireNonNull(mv);
        requireNonNull(trueBranch);
        requireNonNull(falseBranch);

        if (!trueBranch.getNode().equals(falseBranch.getNode())) {
            throw new IllegalArgumentException("branches have different origins");
        }

        switch (opcode) {
            /*
             * Comparisons of an integer i at the top of the operand stack with 0:
             * i == 0, i != 0, i < 0, i <= 0, and i > 0
             */
            case IFEQ, IFNE, IFLT, IFLE, IFGT, IFGE -> // i >= 0
                    instrumentBranchNode(mv, trueBranch, falseBranch, opcode, Arity.UNARY, INT_T);


            /*
             * Comparisons of two integers i and j at the top of the operand stack:
             * i == j, i != j, i < j, l <= j, and i > j
             */
            case IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPLE, IF_ICMPGT, IF_ICMPGE -> // i >= j
                    instrumentBranchNode(mv, trueBranch, falseBranch, opcode, Arity.BINARY, INT_T);


            /*
             * Comparison of the reference r at the top of the operand stack with null:
             * r == null and  r != null
             */
            case IFNULL, IFNONNULL -> instrumentBranchNode(mv, trueBranch, falseBranch, opcode, Arity.UNARY, OBJ_T);


            /*
             * Comparisons of two references r and s at the top of the operand stack:
             * r == s and r != s
             */
            case IF_ACMPEQ, IF_ACMPNE -> instrumentBranchNode(mv, trueBranch, falseBranch, opcode, Arity.BINARY, OBJ_T);

            // The two remaining instructions are GOTO and JSR. Do nothing because these
            // instructions represent unconditional jumps for which branching distance
            // computation does not make a lot of sense.
        }
    }

    /**
     * Uses the given method visitor to perform instrumentation of the given branches node with the
     * aim of computing the branching distance for a comparison {@code opcode} of the specified
     * {@code arity} where the operand(s) of the comparison are of the given {@code type}. The
     * original control transfer instruction is retained in the instrumented code in order not to
     * change the original behavior of the program.
     *
     * @param mv          the method visitor with which to instrument
     * @param trueBranch  the {@code true} branch for which to instrument
     * @param falseBranch the {@code false} branch for which to instrument
     * @param opcode      the control transfer instruction of the branch node
     * @param arity       the arity of the comparison
     * @param type        the type of the operand(s) to the comparison
     */
    private void instrumentBranchNode(
            final MethodVisitor mv,
            final Decision trueBranch,
            final Decision falseBranch,
            final int opcode,
            final Arity arity,
            final Type type) {
        final int trueID = trueBranch.getId();
        final int falseID = falseBranch.getId();

        branches.put(trueID, trueBranch);
        branches.put(falseID, falseBranch);

        /*
         * The JVM is a so-called stack machine, which means that its instructions manipulate a
         * push-down stack of operands. Before we can insert an instruction that invokes the
         * "passedBranch" method, we have to make sure that the operand stack is in an appropriate
         * state for that method call. That is, we have to prepare the stack by pushing all required
         * method parameters onto it. In particular, this includes the operands of the jump
         * instruction itself, among others. When the method call returns, its parameters are popped
         * off the stack, including the original operands for the jump instruction. After that, the
         * jump instruction will be executed, expecting to find the now popped-off operands on top
         * of the stack. Obviously, this is a problem because the instrumented program will now no
         * longer work correctly. To avoid this problem, we must first and foremost duplicate the
         * operands of the jump instruction before preparing the operand stack for the method call.
         * By construction, this will leave the operand stack in a valid state for the jump
         * instruction after the inserted method call returns.
         */


        // The descriptor of the method responsible for computing the branching distance (has to be
        // invoked by the instrumented code).
        final String descriptor;

        switch (arity) {
            case UNARY -> {
                descriptor = Type.getMethodDescriptor(VOID_T, type, INT_T, INT_T, INT_T);

                // Prepare the operand stack for the "passedBranch" method call.

                // [..., operand]
                mv.visitInsn(DUP);
                // [..., operand, operand]
                mv.visitMethodInsn(INVOKESTATIC, BRANCH_TRACER, GET_INSTANCE, GET_INSTANCE_D, false);
                // [..., operand, operand, reference]
                mv.visitInsn(SWAP);
                // [..., operand, reference, operand]
            }

            case BINARY -> {
                descriptor = Type.getMethodDescriptor(VOID_T, type, type, INT_T, INT_T, INT_T);

                // Prepare the operand stack for the "passedBranch" method call.

                // [..., operand1, operand2]
                mv.visitInsn(DUP2);
                // [..., operand1, operand2, operand1, operand2]
                mv.visitMethodInsn(INVOKESTATIC, BRANCH_TRACER, GET_INSTANCE, GET_INSTANCE_D, false);
                // [..., operand1, operand2, operand1, operand2, reference]
                mv.visitInsn(DUP_X2);
                // [..., operand1, operand2, reference, operand1, operand2, reference]
                mv.visitInsn(POP);
                // [..., operand1, operand2, reference, operand1, operand2]
            }
            default -> throw new IllegalStateException("Invalid arity " + arity);
        }

        // Prepare the operand stack for the method call by pushing all the remaining required
        // parameters onto the stack. (The order in which they are pushed onto the stack must match
        // the order in which they are declared by the "passedBranch" method.)

        // [..., operand(s), reference, operand(s)]
        mv.visitLdcInsn(opcode);
        // [..., operand(s), reference, operand(s), opcode]
        mv.visitLdcInsn(trueID);
        // [..., operand(s), reference, operand(s), opcode, trueID]
        mv.visitLdcInsn(falseID);
        // [..., operand(s), reference, operand(s), opcode, trueID, falseID]

        // Call the "passedBranch" method so that the instrumented code performs the branch distance
        // computation. When this method returns, the operands of the original jump instruction are
        // now at the top of the stack and the jump instruction can execute as normal.
        mv.visitMethodInsn(INVOKEVIRTUAL, BRANCH_TRACER, PASSED_BRANCH, descriptor, false);
        // [..., operand(s)]
    }

    /**
     * Uses the given non-{@code null} method visitor to insert instructions into the byte codes of
     * methods so that the instrumented code computes the branching distance of the specified
     * non-{@code null} root branch. It is assumed that the method visitor is currently visiting the
     * beginning of the method which the given root branch belongs to.
     *
     * @param mv         the method visitor with which to instrument
     * @param rootBranch the root branch for which to instrument
     */
    void instrumentMethodEntry(final MethodVisitor mv, final Entry rootBranch) {
        requireNonNull(mv);
        requireNonNull(rootBranch);

        mv.visitMethodInsn(INVOKESTATIC, BRANCH_TRACER, GET_INSTANCE,
                Type.getMethodDescriptor(BRANCH_TRACER_T), false);

        final int id = rootBranch.getId();
        branches.put(id, rootBranch);
        mv.visitLdcInsn(id);

        final String descriptor = Type.getMethodDescriptor(VOID_T, INT_T);
        mv.visitMethodInsn(INVOKEVIRTUAL, BRANCH_TRACER, PASSED_BRANCH, descriptor, false);
    }

    /**
     * Computes the branching distance for a conditional integer comparison with 0. Called by the
     * instrumented code each time such a decision node is taken.
     *
     * @param i           the integer value to compare to 0
     * @param opcode      the opcode specifying the comparison to perform
     * @param trueBranch  the unique number identifying the {@code true branch} of the comparison
     * @param falseBranch the unique number identifying the {@code false branch} of the comparison
     * @see <a href="https://docs.oracle.com/javase/specs/jvms/se11/html/jvms-2.html#jvms-2.11.7">
     * The Java Virtual Machine Specification -- §2.11.7 Control Transfer Instructions
     * </a>
     * @see <a href="https://docs.oracle.com/javase/specs/jvms/se11/html/jvms-6.html#jvms-6.5">
     * The Java Virtual Machine Specification -- §6.5. Instructions
     * </a>
     */
    @SuppressWarnings("unused") // Will be called by the instrumented code.
    // NOTE: The name and signature of this method must not be changed because they're hard-coded
    // in instrumentMethodEntry() and instrumentBranchNode().
    public void passedBranch(final int i, final int opcode, final int trueBranch, final int falseBranch) {
        final double distanceTrue;
        final double distanceFalse;


        switch (opcode){
            case IFEQ:
                if (i == 0) {
                    distanceTrue = 0.0;
                    distanceFalse = 1.0;
                }
                else{
                    distanceTrue = (i < 0) ? -1.0 * i : 1.0 * i;
                    distanceFalse = 0.0;
                }
                break;
            case IFNE:
                if (i != 0){
                    distanceTrue = 0.0;
                    distanceFalse = (i < 0) ? -1.0 * i : 1.0 * i;;
                }
                else{
                    distanceTrue = 1.0;
                    distanceFalse = 0.0;
                }
                break;
            case IFLT:
                if (i < 0){
                    distanceTrue = 0.0;
                    distanceFalse = -1.0 * (i);
                }
                else{
                    distanceTrue = i + 1.0;
                    distanceFalse = 0.0;
                }
                break;
            case IFLE:
                if (i <= 0){
                    distanceTrue = 0.0;
                    distanceFalse = -1.0 * (i) + 1;
                }
                else{
                    distanceTrue = i;
                    distanceFalse = 0.0;
                }
                break;
            case IFGT:
                if (i > 0){
                    distanceTrue = 0.0;
                    distanceFalse = i;
                }
                else{
                    distanceTrue = -1.0 * (i) + 1;
                    distanceFalse = 0.0;
                }
                break;
            case IFGE:
                if (i >= 0){
                    distanceTrue = 0.0;
                    distanceFalse = 1.0 * i + 1.0;
                }
                else{
                    distanceTrue = -1.0 * (i);
                    distanceFalse = 0.0;
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown opcode: " + opcode);
        }
        traceBranchDistance(trueBranch, distanceTrue, falseBranch, distanceFalse);
        //throw new UnsupportedOperationException("Implement me!");

        // Uncomment once you have implemented the method.
        //traceBranchDistance(trueBranch, distanceTrue, falseBranch, distanceFalse);
    }

    /**
     * Computes the branching distance for a conditional comparison of two integers. Called by the
     * instrumented code each time such a decision node is taken.
     *
     * @param i           the first integer to compare
     * @param j           the second integer to compare
     * @param opcode      the opcode specifying the comparison to perform
     * @param trueBranch  the unique number identifying the {@code true} branch of the comparison
     * @param falseBranch the unique number identifying the {@code false} branch of the comparison
     * @see <a href="https://docs.oracle.com/javase/specs/jvms/se11/html/jvms-2.html#jvms-2.11.7">
     * The Java Virtual Machine Specification -- §2.11.7 Control Transfer Instructions
     * </a>
     * @see <a href="https://docs.oracle.com/javase/specs/jvms/se11/html/jvms-6.html#jvms-6.5">
     * The Java Virtual Machine Specification -- 6.5. Instructions
     * </a>
     */
    @SuppressWarnings("unused") // Will be called by the instrumented code.
    // NOTE: The name and signature of this method must not be changed because they're hard-coded
    // in instrumentMethodEntry() and instrumentBranchNode().
    public void passedBranch(final int i, final int j, final int opcode, final int trueBranch,
                             final int falseBranch) {
        final double distanceTrue;
        final double distanceFalse;
        // i == j, i != j, i < j, l <= j, i > j
        // IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPLE, IF_ICMPGT, IF_ICMPGE

        switch (opcode){
            case IF_ICMPEQ:
                if (i == j){
                    distanceTrue = 0.0;
                    distanceFalse = 1.0;
                }
                else{
                    distanceTrue = (i < j) ? (double) j - i : (double) i - j;;
                    distanceFalse = 0.0;
                }
                break;
            case IF_ICMPNE:
                if (i != j){
                    distanceTrue = 0.0;
                    distanceFalse = (i < j) ? (double) j - i : (double) i - j;
                }
                else{
                    distanceTrue = 1.0;
                    distanceFalse = 0.0;
                }
                break;
            case IF_ICMPLT:
                if (i < j){
                    distanceTrue = 0.0;
                    distanceFalse = (double) j - (double) i;
                }
                else{
                    distanceTrue = (double) i - (double) j + 1.0;
                    distanceFalse = 0.0;
                }
                break;
            case IF_ICMPLE:
                if (i <= j){
                    distanceTrue = 0.0;
                    distanceFalse = (double) j - (double) i + 1.0;
                }
                else{
                    distanceTrue = (double) i - (double) j;
                    distanceFalse = 0.0;
                }
                break;
            case IF_ICMPGT:
                if (i > j){
                    distanceTrue = 0.0;
                    distanceFalse = (double) i - (double) j;
                }
                else{
                    distanceTrue =  (double) j - (double) i + 1.0;
                    distanceFalse = 0.0;
                }
                break;
            case IF_ICMPGE:
                if (i >= j){
                    distanceTrue = 0.0;
                    distanceFalse = (double) i - (double) j + 1.0;
                }
                else{
                    distanceTrue = (double) j - (double) i;
                    distanceFalse = 0.0;
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown opcode: " + opcode);
        }
        traceBranchDistance(trueBranch, distanceTrue, falseBranch, distanceFalse);
        //throw new UnsupportedOperationException("Implement me!");

        // Uncomment once you have implemented the method.
        //traceBranchDistance(trueBranch, distanceTrue, falseBranch, distanceFalse);
    }

    /**
     * Computes the branching distance for a conditional reference comparison with {@code null}.
     * Called by the instrumented code each time such a decision node is taken.
     *
     * @param o           the reference to compare to {@code null}
     * @param opcode      the opcode specifying the comparison to perform
     * @param trueBranch  the unique number identifying the {@code true} branch of the comparison
     * @param falseBranch the unique number identifying the {@code false} branch of the comparison
     * @see <a href="https://docs.oracle.com/javase/specs/jvms/se11/html/jvms-2.html#jvms-2.11.7">
     * The Java Virtual Machine Specification -- §2.11.7 Control Transfer Instructions
     * </a>
     * @see <a href="https://docs.oracle.com/javase/specs/jvms/se11/html/jvms-6.html#jvms-6.5">
     * The Java Virtual Machine Specification -- 6.5. Instructions
     * </a>
     */
    @SuppressWarnings("unused") // Will be called by the instrumented code.
    // NOTE: The name and signature of this method must not be changed because they're hard-coded
    // in instrumentMethodEntry() and instrumentBranchNode().
    public void passedBranch(final Object o, final int opcode, final int trueBranch,
                             final int falseBranch) {
        final double distanceTrue;
        final double distanceFalse;

        //IFNULL, IFNONNULL
        switch (opcode){
            case IFNULL:
                if (o == null){
                    distanceTrue = 0.0;
                    distanceFalse = 1.0;
                }
                else{
                    distanceTrue = 1.0;
                    distanceFalse = 0.0;
                }
                break;
            case IFNONNULL:
                if (o != null){
                    distanceTrue = 0.0;
                    distanceFalse = 1.0;
                }
                else{
                    distanceTrue = 1.0;
                    distanceFalse = 0.0;
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown opcode: " + opcode);
        }

        traceBranchDistance(trueBranch, distanceTrue, falseBranch, distanceFalse);
        //throw new UnsupportedOperationException("Implement me!");

        // Uncomment once you have implemented the method.
        // traceBranchDistance(trueBranch, distanceTrue, falseBranch, distanceFalse);
    }

    /**
     * Computes the branching distance for a conditional comparison of two references. Called by the
     * instrumented code each time such a decision node is taken.
     *
     * @param o           a reference
     * @param p           another reference
     * @param opcode      the opcode specifying the comparison to perform
     * @param trueBranch  the unique number identifying the {@code true} branch of the comparison
     * @param falseBranch the unique number identifying the {@code false} branch of the comparison
     * @see <a href="https://docs.oracle.com/javase/specs/jvms/se11/html/jvms-2.html#jvms-2.11.7">
     * The Java Virtual Machine Specification -- §2.11.7 Control Transfer Instructions
     * </a>
     * @see <a href="https://docs.oracle.com/javase/specs/jvms/se11/html/jvms-6.html#jvms-6.5">
     * The Java Virtual Machine Specification -- 6.5. Instructions
     * </a>
     */
    @SuppressWarnings("unused") // Will be called by the instrumented code.
    // NOTE: The name and signature of this method must not be changed because they're hard-coded
    // in instrumentMethodEntry() and instrumentBranchNode().
    public void passedBranch(final Object o, final Object p, final int opcode,
                             final int trueBranch, final int falseBranch) {
        final double distanceTrue;
        final double distanceFalse;
        //IF_ACMPEQ, IF_ACMPNE
        switch (opcode){
            case IF_ACMPEQ:
                if (o == p){
                    distanceTrue = 0.0;
                    distanceFalse = 1.0;
                }
                else{
                    distanceTrue = 1.0;
                    distanceFalse = 0.0;
                }
                break;
            case IF_ACMPNE:
                if (o != p){
                    distanceTrue = 0.0;
                    distanceFalse = 1.0;
                }
                else{
                    distanceTrue = 1.0;
                    distanceFalse = 0.0;
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown opcode: " + opcode);
        }
        traceBranchDistance(trueBranch, distanceTrue, falseBranch, distanceFalse);
        //throw new UnsupportedOperationException("Implement me!");

        // Uncomment once you have implemented the method.
        //traceBranchDistance(trueBranch, distanceTrue, falseBranch, distanceFalse);
    }

    /**
     * Computes the branching distance for method invocations.
     *
     * @param branchID the id specifying the root branch of the method to invoke
     */
    @SuppressWarnings("unused") // Will be called by the instrumented code.
    // NOTE: The name and signature of this method must not be changed because they're hard-coded
    // in instrumentMethodEntry() and instrumentBranchNode().
    public void passedBranch(final int branchID) {
        // For root branches, no distance computation is required at all. We can only determine if
        // the branch has been taken or not. When this method is called by the instrumented code, we
        // know for sure that the root branch of the MUT has been taken.
        traceBranchDistance(branchID);
    }

    /**
     * Creates and records a branch distance trace from the given pieces of information if the
     * branching distance improved. Called by {@code passedBranch()} whenever a branch node has been
     * taken.
     *
     * @param trueBranch    the unique number identifying the {@code true} branch
     * @param distanceTrue  the distance to taking the {@code true} branch
     * @param falseBranch   the unique number identifying the {@code false} branch
     * @param distanceFalse the distance to taking the {@code false} branch
     */
    private void traceBranchDistance(final int trueBranch, final double distanceTrue, final int falseBranch, final double distanceFalse) {

        if (distanceTrue < 0 || distanceFalse < 0)
            throw new IllegalArgumentException("Distances cannot be negative");

        if (!distances.containsKey(trueBranch) || distanceTrue < distances.get(trueBranch)) {
            distances.put(trueBranch, distanceTrue);
        }
        if (!distances.containsKey(falseBranch) || distanceFalse < distances.get(falseBranch)) {
            distances.put(falseBranch, distanceFalse);
        }
            //throw new UnsupportedOperationException("Implement me!");

    }

    /**
     * Creates and records a branch distance trace for the given root branch if the specified
     * distance has improved. Called by {@code passedBranch()} whenever a root branch has been
     * taken.
     *
     * @param rootBranch the unique number identifying the root branch
     */
    private void traceBranchDistance(final int rootBranch) {
        // This method is only called when the MUT itself was called, and therefore we automatically
        // know that the distance to the root branch of the MUT has to be 0.
        distances.put(rootBranch, 0.0);
    }

    /**
     * Returns the set of traced branches.
     *
     * @return the set of branches
     */
    @Override
    public Set<IBranch> getBranches() {
        return new LinkedHashSet<>(branches.values());
    }

    /**
     * Returns a traced branch by its ID, or {@code null} if no such branch exists.
     *
     * @param id the ID of the branch to return
     * @return the branch with the specified ID, or {@code null} if there is branch with that ID
     */
    @Override
    public IBranch getBranchById(final int id) {
        return branches.get(id);
    }

    /**
     * Returns the current branching distances. The map uses branch IDs to associate branches with
     * their corresponding distance.
     *
     * @return the current branch distances
     */
    @Override
    public Map<Integer, Double> getDistances() {
        return Collections.unmodifiableMap(distances);
    }

    /**
     * Clears the recorded branch distances for a new test case execution.
     */
    @Override
    public void clear() {
        distances.clear();
    }

    /**
     * Returns the singleton instance of this class.
     *
     * @return the singleton
     */
    public static BranchTracer getInstance() {
        return LazyHolder.INSTANCE;
    }

    /**
     * An enum class to encode the arity of (relational) operators. Unary operators (for example,
     * the Java byte code opcodes {@code ifnull} or {@code ifeq}) have one operand, whereas binary
     * operators (such as the Java byte code opcode {@code if_acmpeq} or {@code if_icmpeq}) have two
     * operands. There are also ternary or even 4-ary operators in Java byte code, which we do not
     * consider here.
     */
    enum Arity {
        UNARY, BINARY
    }

    /**
     * Implements the singleton pattern using the
     * <a href="https://en.wikipedia.org/wiki/Initialization-on-demand_holder_idiom">
     * initialization-on-demand holder idiom
     * </a>.
     */
    private static final class LazyHolder {

        private static final BranchTracer INSTANCE = new BranchTracer();
    }

}
